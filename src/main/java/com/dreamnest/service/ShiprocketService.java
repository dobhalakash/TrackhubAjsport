package com.dreamnest.service;

import com.dreamnest.entity.Address;
import com.dreamnest.entity.Order;
import com.dreamnest.entity.OrderItem;
import com.dreamnest.enums.PaymentMethod;
import com.dreamnest.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Thin client for the Shiprocket API (https://apidocs.shiprocket.in).
 *
 * <p>Shiprocket is a courier aggregator: rather than integrating with each
 * courier (Delhivery, Bluedart, etc.) individually, DreamNest pushes the
 * order to Shiprocket once and it picks/assigns a courier + AWB (Air Waybill
 * number) for us, which is what customers use to track their package.</p>
 *
 * <p><b>Go-live checklist:</b></p>
 * <ol>
 *   <li>Create a Shiprocket account at shiprocket.in and complete KYC.</li>
 *   <li>Add at least one Pickup Location under Settings -> Pickup Addresses,
 *       and note its exact nickname.</li>
 *   <li>Set {@code SHIPROCKET_EMAIL}, {@code SHIPROCKET_PASSWORD}, and
 *       {@code SHIPROCKET_PICKUP_LOCATION} as environment variables.</li>
 *   <li>(Optional) Configure a tracking webhook in Shiprocket pointing at
 *       {@code POST /api/shipments/shiprocket/webhook?token=...} for
 *       automatic status updates instead of relying only on manual refresh.</li>
 * </ol>
 */
@Service
public class ShiprocketService {

    private static final Logger log = LoggerFactory.getLogger(ShiprocketService.class);
    private static final String BASE_URL = "https://apiv2.shiprocket.in/v1/external";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${dreamnest.shiprocket.email}")
    private String email;

    @Value("${dreamnest.shiprocket.password}")
    private String password;

    @Value("${dreamnest.shiprocket.pickup-location}")
    private String pickupLocation;

    @Value("${dreamnest.shiprocket.default-weight-kg}")
    private double defaultWeightKg;

    @Value("${dreamnest.shiprocket.default-length-cm}")
    private double defaultLengthCm;

    @Value("${dreamnest.shiprocket.default-breadth-cm}")
    private double defaultBreadthCm;

    @Value("${dreamnest.shiprocket.default-height-cm}")
    private double defaultHeightCm;

    // Shiprocket tokens are valid ~10 days; cache and refresh proactively
    // a little early rather than waiting for a 401.
    private String cachedToken;
    private LocalDateTime tokenExpiresAt;

    public boolean isUsingDummyCredentials() {
        return email == null || email.startsWith("dummy") || password == null || password.startsWith("dummy_");
    }

    public static class ShipmentCreationResult {
        public String shiprocketOrderId;
        public String shiprocketShipmentId;
    }

    public static class AwbAssignmentResult {
        public String awbCode;
        public String courierName;
    }

    public static class TrackingResult {
        public String currentStatus;
        public String lastNote;
    }

    /** Pushes the order to Shiprocket as a new order + shipment. */
    @SuppressWarnings("unchecked")
    public ShipmentCreationResult createOrder(Order order, PaymentMethod paymentMethod) {
        requireConfigured();

        Address addr = order.getAddress();
        if (addr == null) {
            throw new BadRequestException("Order has no shipping address");
        }

        List<Map<String, Object>> orderItems = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            Map<String, Object> line = new HashMap<>();
            line.put("name", item.getProductName());
            line.put("sku", "SKU-" + (item.getProduct() != null ? item.getProduct().getId() : item.getId()));
            line.put("units", item.getQuantity());
            line.put("selling_price", item.getUnitPrice());
            orderItems.add(line);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("order_id", order.getOrderNumber());
        payload.put("order_date", order.getCreatedAt().toLocalDate().toString() + " " + LocalDate.now());
        payload.put("pickup_location", pickupLocation);
        payload.put("billing_customer_name", addr.getFullName());
        payload.put("billing_last_name", "");
        payload.put("billing_address", addr.getAddressLine1());
        payload.put("billing_address_2", addr.getAddressLine2() != null ? addr.getAddressLine2() : "");
        payload.put("billing_city", addr.getCity());
        payload.put("billing_pincode", addr.getPincode());
        payload.put("billing_state", addr.getState());
        payload.put("billing_country", addr.getCountry() != null ? addr.getCountry() : "India");
        payload.put("billing_email", order.getUser() != null ? order.getUser().getEmail() : "");
        payload.put("billing_phone", addr.getPhone());
        payload.put("shipping_is_billing", true);
        payload.put("order_items", orderItems);
        payload.put("payment_method", paymentMethod == PaymentMethod.COD ? "COD" : "Prepaid");
        payload.put("sub_total", order.getGrandTotal());
        payload.put("length", defaultLengthCm);
        payload.put("breadth", defaultBreadthCm);
        payload.put("height", defaultHeightCm);
        payload.put("weight", defaultWeightKg);

        Map<String, Object> response = post("/orders/create/adhoc", payload);
        if (response == null || response.get("order_id") == null) {
            log.error("Unexpected Shiprocket order-create response: {}", response);
            throw new BadRequestException("Shiprocket did not return an order id. Please try again or enter tracking manually.");
        }

        ShipmentCreationResult result = new ShipmentCreationResult();
        result.shiprocketOrderId = String.valueOf(response.get("order_id"));
        result.shiprocketShipmentId = String.valueOf(response.get("shipment_id"));
        return result;
    }

    /** Asks Shiprocket to pick a courier and assign an AWB for an already-created shipment. */
    @SuppressWarnings("unchecked")
    public AwbAssignmentResult assignAwb(String shiprocketShipmentId) {
        requireConfigured();

        Map<String, Object> payload = new HashMap<>();
        payload.put("shipment_id", Long.parseLong(shiprocketShipmentId));

        Map<String, Object> response = post("/courier/assign/awb", payload);
        Map<String, Object> data = response != null ? (Map<String, Object>) response.get("response") : null;
        Map<String, Object> awbData = data != null ? (Map<String, Object>) data.get("data") : null;

        if (awbData == null || awbData.get("awb_code") == null) {
            log.error("Unexpected Shiprocket AWB-assign response: {}", response);
            throw new BadRequestException("Shiprocket could not assign a courier for this shipment. " +
                    "This usually means the pickup pincode isn't serviceable yet, or KYC is incomplete.");
        }

        AwbAssignmentResult result = new AwbAssignmentResult();
        result.awbCode = String.valueOf(awbData.get("awb_code"));
        result.courierName = String.valueOf(awbData.get("courier_name"));
        return result;
    }

    /** Fetches the latest tracking status for an AWB. */
    @SuppressWarnings("unchecked")
    public TrackingResult track(String awbCode) {
        requireConfigured();

        Map<String, Object> response = get("/courier/track/awb/" + awbCode);
        Map<String, Object> trackingData = response != null ? (Map<String, Object>) response.get("tracking_data") : null;
        if (trackingData == null) {
            return null;
        }

        TrackingResult result = new TrackingResult();
        Object currentStatus = trackingData.get("shipment_status");
        result.currentStatus = currentStatus != null ? String.valueOf(currentStatus) : null;

        List<Map<String, Object>> activities = (List<Map<String, Object>>) trackingData.get("shipment_track_activities");
        if (activities != null && !activities.isEmpty()) {
            result.lastNote = String.valueOf(activities.get(0).get("activity"));
        }
        return result;
    }

    private void requireConfigured() {
        if (isUsingDummyCredentials()) {
            throw new BadRequestException(
                    "Shiprocket is not configured yet. Set SHIPROCKET_EMAIL and SHIPROCKET_PASSWORD, " +
                            "or enter tracking details manually instead.");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String path, Object body) {
        HttpHeaders headers = authHeaders();
        try {
            return restTemplate.exchange(BASE_URL + path, HttpMethod.POST,
                    new HttpEntity<>(body, headers), Map.class).getBody();
        } catch (HttpClientErrorException.Unauthorized e) {
            // Token expired earlier than expected - refresh once and retry.
            cachedToken = null;
            headers = authHeaders();
            return restTemplate.exchange(BASE_URL + path, HttpMethod.POST,
                    new HttpEntity<>(body, headers), Map.class).getBody();
        } catch (Exception e) {
            log.error("Shiprocket API call failed: POST {} - {}", path, e.getMessage());
            throw new BadRequestException("Could not reach Shiprocket right now. Please try again shortly.");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> get(String path) {
        HttpHeaders headers = authHeaders();
        try {
            return restTemplate.exchange(BASE_URL + path, HttpMethod.GET,
                    new HttpEntity<>(headers), Map.class).getBody();
        } catch (Exception e) {
            log.error("Shiprocket API call failed: GET {} - {}", path, e.getMessage());
            throw new BadRequestException("Could not reach Shiprocket right now. Please try again shortly.");
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized HttpHeaders authHeaders() {
        if (cachedToken == null || tokenExpiresAt == null || LocalDateTime.now().isAfter(tokenExpiresAt)) {
            Map<String, Object> body = new HashMap<>();
            body.put("email", email);
            body.put("password", password);

            HttpHeaders loginHeaders = new HttpHeaders();
            loginHeaders.setContentType(MediaType.APPLICATION_JSON);

            try {
                Map<String, Object> response = restTemplate.postForObject(
                        BASE_URL + "/auth/login", new HttpEntity<>(body, loginHeaders), Map.class);
                if (response == null || response.get("token") == null) {
                    throw new BadRequestException("Shiprocket login failed - check SHIPROCKET_EMAIL/SHIPROCKET_PASSWORD.");
                }
                cachedToken = (String) response.get("token");
                // Tokens last ~10 days; refresh a day early to be safe.
                tokenExpiresAt = LocalDateTime.now().plusDays(9);
            } catch (BadRequestException e) {
                throw e;
            } catch (Exception e) {
                log.error("Shiprocket authentication failed: {}", e.getMessage());
                throw new BadRequestException("Could not authenticate with Shiprocket. Please check your credentials.");
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(cachedToken);
        return headers;
    }
}

package com.dreamnest.service;

import com.dreamnest.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Thin client for the Razorpay Orders API and signature verification.
 *
 * <p>Razorpay (https://razorpay.com) is used here as TrackHub's payment
 * gateway because, unlike a bare UPI deep link, it gives us a
 * cryptographically verifiable proof of payment: after checkout, Razorpay
 * returns a payment id + signature that we verify server-side with our
 * secret key before ever marking an order as paid. A customer cannot fake
 * a "payment success" by simply calling our API, which a deep-link-only flow
 * could not prevent.</p>
 *
 * <p><b>Go-live checklist:</b></p>
 * <ol>
 *   <li>Create a Razorpay account and complete KYC/business verification.</li>
 *   <li>Generate Live Mode API keys from Settings -> API Keys.</li>
 *   <li>Set {@code RAZORPAY_KEY_ID} and {@code RAZORPAY_KEY_SECRET} as environment
 *       variables on your server (never commit them or ship the secret to the frontend).</li>
 *   <li>Configure a webhook (Settings -> Webhooks) pointing at
 *       {@code POST /api/payments/razorpay/webhook} with the "payment.captured" and
 *       "payment.failed" events, and set {@code RAZORPAY_WEBHOOK_SECRET}.</li>
 * </ol>
 */
@Service
public class RazorpayService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayService.class);
    private static final String ORDERS_URL = "https://api.razorpay.com/v1/orders";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${dreamnest.payment.razorpay.key-id}")
    private String keyId;

    @Value("${dreamnest.payment.razorpay.key-secret}")
    private String keySecret;

    @Value("${dreamnest.payment.razorpay.currency:INR}")
    private String currency;

    @Value("${dreamnest.payment.razorpay.webhook-secret:}")
    private String webhookSecret;

    public String getPublicKeyId() {
        return keyId;
    }

    /**
     * Creates a Razorpay order for the given amount (in rupees) and returns
     * the gateway's order id. Amount is converted to the smallest currency
     * unit (paise) as Razorpay requires.
     */
    @SuppressWarnings("unchecked")
    public String createOrder(BigDecimal amountInRupees, String receipt) {
        if (isUsingDummyCredentials()) {
            // Dev/demo fallback so the flow is still testable end-to-end
            // before real Razorpay keys are configured. NEVER ship this
            // branch enabled in production - it does not call Razorpay at all.
            String fakeId = "order_DEV" + System.currentTimeMillis();
            log.warn("Razorpay is using DUMMY credentials - generating a local fake order id ({}). " +
                    "Set RAZORPAY_KEY_ID/RAZORPAY_KEY_SECRET to go live.", fakeId);
            return fakeId;
        }

        long amountInPaise = amountInRupees.setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).longValueExact();

        Map<String, Object> body = new HashMap<>();
        body.put("amount", amountInPaise);
        body.put("currency", currency);
        body.put("receipt", receipt);
        body.put("payment_capture", 1);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(keyId, keySecret);

        try {
            Map<String, Object> response = restTemplate.exchange(
                    ORDERS_URL, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class).getBody();
            if (response == null || response.get("id") == null) {
                throw new BadRequestException("Payment gateway did not return an order id");
            }
            return response.get("id").toString();
        } catch (Exception e) {
            log.error("Failed to create Razorpay order", e);
            throw new BadRequestException("Could not initiate payment. Please try again.");
        }
    }

    /**
     * Verifies the signature returned by Razorpay Checkout after a successful
     * payment: HMAC-SHA256(razorpayOrderId + "|" + razorpayPaymentId, keySecret)
     * must equal the signature, exactly as documented by Razorpay. This is the
     * step that proves the payment actually happened and was not forged by
     * the client.
     */
    public boolean verifyPaymentSignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        if (razorpayOrderId == null || razorpayPaymentId == null || razorpaySignature == null) {
            return false;
        }
        if (isUsingDummyCredentials()) {
            // In dummy/dev mode there is no real Razorpay signature to check,
            // since createOrder() above never talked to Razorpay either. We
            // accept any non-blank values so the rest of the flow (UI,
            // polling, order status transitions) can be exercised end-to-end
            // in development. This branch MUST NOT be reachable once real
            // keys are configured (isUsingDummyCredentials() becomes false).
            log.warn("Razorpay is using DUMMY credentials - skipping real signature verification for order {}", razorpayOrderId);
            return razorpayPaymentId.startsWith("pay_") || razorpayPaymentId.startsWith("DEV");
        }
        String payload = razorpayOrderId + "|" + razorpayPaymentId;
        String expected = hmacSha256Hex(payload, keySecret);
        return constantTimeEquals(expected, razorpaySignature);
    }

    /**
     * Verifies an asynchronous Razorpay webhook payload using the separate
     * webhook secret (Dashboard -> Webhooks), comparing against the
     * X-Razorpay-Signature header.
     */
    public boolean verifyWebhookSignature(String rawBody, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank() || webhookSecret.startsWith("dummy")) {
            log.warn("Razorpay webhook secret is not configured - rejecting webhook for safety");
            return false;
        }
        String expected = hmacSha256Hex(rawBody, webhookSecret);
        return constantTimeEquals(expected, signatureHeader);
    }

    public boolean isUsingDummyCredentials() {
        return keyId == null || keyId.contains("DUMMY") || keySecret == null || keySecret.startsWith("dummy");
    }

    private String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC signature", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(
                Base64.getEncoder().encode(a.getBytes(StandardCharsets.UTF_8)),
                Base64.getEncoder().encode(b.getBytes(StandardCharsets.UTF_8)));
    }
}

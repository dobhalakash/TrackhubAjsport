package com.dreamnest.controller;

import com.dreamnest.dto.response.ApiResponse;
import com.dreamnest.entity.Order;
import com.dreamnest.entity.Payment;
import com.dreamnest.enums.OrderStatus;
import com.dreamnest.enums.PaymentStatus;
import com.dreamnest.repository.OrderRepository;
import com.dreamnest.repository.PaymentRepository;
import com.dreamnest.service.RazorpayService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Webhook endpoints for payment service provider (PSP) callbacks. These are
 * a defense-in-depth backstop in case a customer closes the browser tab
 * right after paying but before the Checkout success callback reaches
 * {@code POST /orders/{id}/verify-payment} - the synchronous verification
 * call is the primary path, this webhook is the asynchronous fallback.
 *
 * <p>Both webhooks verify their signature before trusting the payload. An
 * unsigned or incorrectly-signed request is always rejected, regardless of
 * what it claims.</p>
 */
@RestController
@RequestMapping("/payments")
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RazorpayService razorpayService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentWebhookController(OrderRepository orderRepository, PaymentRepository paymentRepository,
                                     RazorpayService razorpayService) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.razorpayService = razorpayService;
    }

    /**
     * Razorpay webhook (Dashboard -> Webhooks -> Add New Webhook). Configure
     * this URL with the "payment.captured" event and set
     * {@code RAZORPAY_WEBHOOK_SECRET} once live.
     */
    @PostMapping("/razorpay/webhook")
    public ApiResponse<String> razorpayWebhook(@RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
                                                @RequestBody String rawBody) {
        if (!razorpayService.verifyWebhookSignature(rawBody, signature)) {
            log.warn("Rejected Razorpay webhook with invalid/missing signature");
            return ApiResponse.success("ignored", "Invalid signature");
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String event = root.path("event").asText();
            if (!"payment.captured".equals(event)) {
                return ApiResponse.success("ignored", "Event not handled: " + event);
            }

            JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
            String razorpayOrderId = paymentEntity.path("order_id").asText(null);
            String razorpayPaymentId = paymentEntity.path("id").asText(null);
            if (razorpayOrderId == null) {
                return ApiResponse.success("ignored", "Missing order_id");
            }

            Payment payment = paymentRepository.findByGatewayOrderId(razorpayOrderId).orElse(null);
            if (payment == null) {
                log.warn("Razorpay webhook referenced unknown gateway order id: {}", razorpayOrderId);
                return ApiResponse.success("ignored", "Unknown order");
            }

            if (payment.getStatus() != PaymentStatus.SUCCESS) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setPaidAt(LocalDateTime.now());
                payment.setGatewayPaymentId(razorpayPaymentId);
                payment.setTransactionId(razorpayPaymentId);
                paymentRepository.save(payment);

                Order order = payment.getOrder();
                if (order != null && order.getStatus() == OrderStatus.PENDING) {
                    order.setStatus(OrderStatus.CONFIRMED);
                    orderRepository.save(order);
                }
            }

            return ApiResponse.success("ok", "Webhook processed");
        } catch (Exception e) {
            log.error("Failed to process Razorpay webhook", e);
            return ApiResponse.success("error", "Could not process webhook");
        }
    }

    /**
     * Legacy PhonePe webhook scaffold, inactive unless
     * {@code dreamnest.payment.phonepe.*} is configured with real credentials.
     * PhonePe signs callbacks with an X-VERIFY header computed as
     * {@code SHA256(base64(response) + saltKey) + "###" + saltIndex} - kept
     * here for reference if PhonePe is added alongside Razorpay later.
     */
    @PostMapping("/phonepe/webhook")
    public ApiResponse<String> phonePeWebhook(@RequestHeader(value = "X-VERIFY", required = false) String verifyHeader,
                                               @RequestBody Map<String, Object> payload) {
        log.info("Received PhonePe webhook (X-VERIFY present: {})", verifyHeader != null);
        // This integration is not wired to a live PhonePe merchant account in
        // this build - TrackHub's verified payment path is Razorpay
        // (see /orders/{id}/verify-payment). Reject silently rather than
        // trusting an unverifiable payload.
        return ApiResponse.success("ignored", "PhonePe integration not active");
    }
}

package com.dreamnest.service.impl;

import com.dreamnest.dto.request.ManualShipmentRequest;
import com.dreamnest.dto.response.ShipmentResponse;
import com.dreamnest.entity.Notification;
import com.dreamnest.entity.Order;
import com.dreamnest.entity.Payment;
import com.dreamnest.entity.Shipment;
import com.dreamnest.enums.OrderStatus;
import com.dreamnest.enums.PaymentMethod;
import com.dreamnest.enums.PaymentStatus;
import com.dreamnest.enums.ShipmentStatus;
import com.dreamnest.exception.BadRequestException;
import com.dreamnest.exception.ResourceNotFoundException;
import com.dreamnest.exception.UnauthorizedException;
import com.dreamnest.mapper.ShipmentMapper;
import com.dreamnest.repository.NotificationRepository;
import com.dreamnest.repository.OrderRepository;
import com.dreamnest.repository.PaymentRepository;
import com.dreamnest.repository.ShipmentRepository;
import com.dreamnest.service.ShipmentService;
import com.dreamnest.service.ShiprocketService;
import com.dreamnest.service.InvoiceService;
import com.dreamnest.service.PayoutService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ShipmentServiceImpl implements ShipmentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ShipmentRepository shipmentRepository;
    private final NotificationRepository notificationRepository;
    private final ShiprocketService shiprocketService;
    private final InvoiceService invoiceService;
    private final PayoutService payoutService;

    public ShipmentServiceImpl(OrderRepository orderRepository,
                                PaymentRepository paymentRepository,
                                ShipmentRepository shipmentRepository,
                                NotificationRepository notificationRepository,
                                ShiprocketService shiprocketService,
                                InvoiceService invoiceService,
                                PayoutService payoutService) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.shipmentRepository = shipmentRepository;
        this.notificationRepository = notificationRepository;
        this.shiprocketService = shiprocketService;
        this.invoiceService = invoiceService;
        this.payoutService = payoutService;
    }

    @Override
    @Transactional
    public ShipmentResponse createViaShiprocket(Long orderId, Long businessUserId, boolean isAdmin) {
        Order order = findOrder(orderId);
        requireOwnerOrAdmin(order, businessUserId, isAdmin);
        Payment payment = requirePaymentConfirmed(order);

        Shipment shipment = shipmentRepository.findByOrderId(orderId).orElse(null);
        if (shipment != null && shipment.getAwbNumber() != null) {
            throw new BadRequestException("This order already has a tracking number assigned (" + shipment.getAwbNumber() + ")");
        }
        if (shipment == null) {
            shipment = new Shipment(order);
        }

        ShiprocketService.ShipmentCreationResult created = shiprocketService.createOrder(order, payment.getPaymentMethod());
        shipment.setShiprocketOrderId(created.shiprocketOrderId);
        shipment.setShiprocketShipmentId(created.shiprocketShipmentId);
        shipment.setSource("SHIPROCKET");
        shipment.setStatus(ShipmentStatus.SHIPMENT_CREATED);

        ShiprocketService.AwbAssignmentResult awb = shiprocketService.assignAwb(created.shiprocketShipmentId);
        shipment.setAwbNumber(awb.awbCode);
        shipment.setCourierName(awb.courierName);
        shipment.setTrackingUrl("https://shiprocket.co/tracking/" + awb.awbCode);
        shipment.setStatus(ShipmentStatus.AWB_ASSIGNED);
        shipment.setShippedAt(LocalDateTime.now());

        shipment = shipmentRepository.save(shipment);

        advanceOrderStatus(order, OrderStatus.SHIPPED);
        notify(order, "Your order has shipped",
                "Order " + order.getOrderNumber() + " has shipped via " + awb.courierName + ". AWB: " + awb.awbCode);

        return ShipmentMapper.toResponse(shipment);
    }

    @Override
    @Transactional
    public ShipmentResponse saveManual(Long orderId, Long businessUserId, boolean isAdmin, ManualShipmentRequest request) {
        Order order = findOrder(orderId);
        requireOwnerOrAdmin(order, businessUserId, isAdmin);
        requirePaymentConfirmed(order);

        Shipment shipment = shipmentRepository.findByOrderId(orderId).orElseGet(() -> new Shipment(order));
        shipment.setCourierName(request.getCourierName());
        shipment.setAwbNumber(request.getAwbNumber());
        shipment.setTrackingUrl(request.getTrackingUrl());
        shipment.setSource("MANUAL");
        if (shipment.getStatus() == ShipmentStatus.PENDING) {
            shipment.setStatus(ShipmentStatus.AWB_ASSIGNED);
        }
        if (shipment.getShippedAt() == null) {
            shipment.setShippedAt(LocalDateTime.now());
        }

        shipment = shipmentRepository.save(shipment);

        advanceOrderStatus(order, OrderStatus.SHIPPED);
        notify(order, "Your order has shipped",
                "Order " + order.getOrderNumber() + " has shipped via " + request.getCourierName() +
                        ". Tracking number: " + request.getAwbNumber());

        return ShipmentMapper.toResponse(shipment);
    }

    @Override
    @Transactional
    public ShipmentResponse refreshTracking(Long orderId, Long businessUserId, boolean isAdmin) {
        Order order = findOrder(orderId);
        requireOwnerOrAdmin(order, businessUserId, isAdmin);

        Shipment shipment = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "orderId", orderId));

        if (shipment.getAwbNumber() == null) {
            throw new BadRequestException("No AWB number on file yet for this order");
        }
        if (!"SHIPROCKET".equals(shipment.getSource())) {
            // Manually-entered shipments aren't trackable via the Shiprocket
            // API since Shiprocket never created them - nothing to refresh.
            return ShipmentMapper.toResponse(shipment);
        }

        ShiprocketService.TrackingResult tracking = shiprocketService.track(shipment.getAwbNumber());
        if (tracking != null) {
            shipment.setLastTrackingNote(tracking.lastNote);
            shipment.setLastSyncedAt(LocalDateTime.now());
            applyStatusFromShiprocket(shipment, order, tracking.currentStatus);
            shipment = shipmentRepository.save(shipment);
        }

        return ShipmentMapper.toResponse(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse getByOrderId(Long orderId) {
        return shipmentRepository.findByOrderId(orderId).map(ShipmentMapper::toResponse).orElse(null);
    }

    @Override
    @Transactional
    public void applyWebhookUpdate(String awbNumber, String status, String note) {
        if (awbNumber == null) {
            return;
        }
        shipmentRepository.findByAwbNumber(awbNumber).ifPresent(shipment -> {
            shipment.setLastTrackingNote(note);
            shipment.setLastSyncedAt(LocalDateTime.now());
            applyStatusFromShiprocket(shipment, shipment.getOrder(), status);
            shipmentRepository.save(shipment);
        });
    }

    private void applyStatusFromShiprocket(Shipment shipment, Order order, String shiprocketStatus) {
        if (shiprocketStatus == null) {
            return;
        }
        String normalized = shiprocketStatus.trim().toUpperCase().replace(" ", "_");

        if (normalized.contains("DELIVERED")) {
            shipment.setStatus(ShipmentStatus.DELIVERED);
            shipment.setDeliveredAt(LocalDateTime.now());
            advanceOrderStatus(order, OrderStatus.DELIVERED);
            settleCodIfNeeded(order);
            sendInvoiceIfDelivered(order);
            generatePayoutsIfDelivered(order);
        } else if (normalized.contains("OUT_FOR_DELIVERY")) {
            shipment.setStatus(ShipmentStatus.OUT_FOR_DELIVERY);
            advanceOrderStatus(order, OrderStatus.OUT_FOR_DELIVERY);
        } else if (normalized.contains("TRANSIT") || normalized.contains("SHIPPED") || normalized.contains("IN_TRANSIT")) {
            shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        } else if (normalized.contains("PICKED")) {
            shipment.setStatus(ShipmentStatus.PICKED_UP);
        } else if (normalized.contains("RTO") || normalized.contains("RETURN")) {
            shipment.setStatus(ShipmentStatus.RTO);
        } else if (normalized.contains("CANCEL")) {
            shipment.setStatus(ShipmentStatus.CANCELLED);
        }
        // Unrecognized statuses are left as-is rather than guessed at.
    }

    private void settleCodIfNeeded(Order order) {
        paymentRepository.findByOrderId(order.getId()).ifPresent(payment -> {
            if (payment.getPaymentMethod() == PaymentMethod.COD && payment.getStatus() != PaymentStatus.SUCCESS) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);
            }
        });
    }

    /** Best-effort: emails the invoice once an order is delivered. Never blocks the status update if email fails. */
    private void sendInvoiceIfDelivered(Order order) {
        try {
            Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
            boolean sent = invoiceService.sendInvoiceEmailIfNeeded(order, payment);
            if (sent) {
                orderRepository.save(order);
            }
        } catch (Exception e) {
            // Invoice email is a nice-to-have, not a reason to fail the delivery update.
        }
    }

    /** Generates business payouts once a COD order is delivered via Shiprocket - the cash is now considered received. */
    private void generatePayoutsIfDelivered(Order order) {
        try {
            Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
            String paymentRef = payment != null ? payment.getGatewayPaymentId() : null;
            payoutService.generatePayoutsForOrder(order, paymentRef);
        } catch (Exception e) {
            // Best-effort - an admin can still see the order and generate/fix the payout manually if this fails.
        }
    }

    /** Only moves the order forward, never backward, and never overwrites a terminal CANCELLED state. */
    private void advanceOrderStatus(Order order, OrderStatus newStatus) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }
        if (newStatus.ordinal() > order.getStatus().ordinal()) {
            order.setStatus(newStatus);
            orderRepository.save(order);
        }
    }

    private void notify(Order order, String title, String message) {
        if (order.getUser() != null) {
            notificationRepository.save(new Notification(order.getUser(), title, message));
        }
    }

    private Payment requirePaymentConfirmed(Order order) {
        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", order.getId()));

        boolean confirmed = payment.getPaymentMethod() == PaymentMethod.COD
                ? order.getStatus() != OrderStatus.CANCELLED
                : payment.getStatus() == PaymentStatus.SUCCESS;

        if (!confirmed) {
            throw new BadRequestException("Tracking details can only be added once payment is confirmed for this order");
        }
        return payment;
    }

    private void requireOwnerOrAdmin(Order order, Long businessUserId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        boolean ownsItem = order.getItems().stream()
                .anyMatch(item -> item.getProduct() != null
                        && item.getProduct().getBusinessUser() != null
                        && item.getProduct().getBusinessUser().getId().equals(businessUserId));
        if (!ownsItem) {
            throw new UnauthorizedException("You do not have permission to manage shipping for this order");
        }
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
    }
}

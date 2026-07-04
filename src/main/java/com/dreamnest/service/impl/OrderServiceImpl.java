package com.dreamnest.service.impl;

import com.dreamnest.dto.request.CheckoutRequest;
import com.dreamnest.dto.request.UpdateOrderStatusRequest;
import com.dreamnest.dto.request.VerifyPaymentRequest;
import com.dreamnest.dto.response.OrderResponse;
import com.dreamnest.dto.response.PageResponse;
import com.dreamnest.dto.response.RazorpayOrderResponse;
import com.dreamnest.dto.response.UpiPaymentInfoResponse;
import com.dreamnest.entity.Address;
import com.dreamnest.entity.BusinessProfile;
import com.dreamnest.entity.Cart;
import com.dreamnest.entity.CartItem;
import com.dreamnest.entity.Coupon;
import com.dreamnest.entity.Order;
import com.dreamnest.entity.OrderItem;
import com.dreamnest.entity.Payment;
import com.dreamnest.entity.Product;
import com.dreamnest.entity.User;
import com.dreamnest.enums.OrderStatus;
import com.dreamnest.enums.PaymentMethod;
import com.dreamnest.enums.PaymentStatus;
import com.dreamnest.enums.RoleName;
import com.dreamnest.exception.BadRequestException;
import com.dreamnest.exception.ResourceNotFoundException;
import com.dreamnest.exception.UnauthorizedException;
import com.dreamnest.mapper.OrderMapper;
import com.dreamnest.mapper.ShipmentMapper;
import com.dreamnest.repository.AddressRepository;
import com.dreamnest.repository.BusinessProfileRepository;
import com.dreamnest.repository.CartItemRepository;
import com.dreamnest.repository.CartRepository;
import com.dreamnest.repository.CouponRepository;
import com.dreamnest.repository.NotificationRepository;
import com.dreamnest.repository.OrderRepository;
import com.dreamnest.repository.PaymentRepository;
import com.dreamnest.repository.ProductRepository;
import com.dreamnest.repository.ShipmentRepository;
import com.dreamnest.repository.UserRepository;
import com.dreamnest.service.CouponService;
import com.dreamnest.service.OrderService;
import com.dreamnest.service.RazorpayService;
import com.dreamnest.service.InvoiceService;
import com.dreamnest.service.IpGeolocationService;
import com.dreamnest.service.PayoutService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import com.dreamnest.entity.Notification;

/**
 * Implementation of {@link OrderService}.
 * Tax and shipping are calculated using simple fixed rates suitable for a demo storefront.
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.05"); // 5% GST
    private static final BigDecimal SHIPPING_FLAT_RATE = new BigDecimal("50.00");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("999.00");
    private static final int RETURN_WINDOW_DAYS = 7;

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final CouponRepository couponRepository;
    private final NotificationRepository notificationRepository;
    private final CouponService couponService;
    private final BusinessProfileRepository businessProfileRepository;
    private final RazorpayService razorpayService;
    private final ShipmentRepository shipmentRepository;
    private final InvoiceService invoiceService;
    private final IpGeolocationService ipGeolocationService;
    private final PayoutService payoutService;

    @Value("${dreamnest.payment.platform-upi-id:dreamnest@upi}")
    private String platformUpiId;

    @Value("${dreamnest.payment.platform-payee-name:TrackHub}")
    private String platformPayeeName;

    public OrderServiceImpl(OrderRepository orderRepository,
                             CartRepository cartRepository,
                             CartItemRepository cartItemRepository,
                             AddressRepository addressRepository,
                             ProductRepository productRepository,
                             UserRepository userRepository,
                             PaymentRepository paymentRepository,
                             CouponRepository couponRepository,
                             NotificationRepository notificationRepository,
                             CouponService couponService,
                             BusinessProfileRepository businessProfileRepository,
                             RazorpayService razorpayService,
                             ShipmentRepository shipmentRepository,
                             InvoiceService invoiceService,
                             IpGeolocationService ipGeolocationService,
                             PayoutService payoutService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.addressRepository = addressRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.couponRepository = couponRepository;
        this.notificationRepository = notificationRepository;
        this.couponService = couponService;
        this.businessProfileRepository = businessProfileRepository;
        this.razorpayService = razorpayService;
        this.shipmentRepository = shipmentRepository;
        this.invoiceService = invoiceService;
        this.ipGeolocationService = ipGeolocationService;
        this.payoutService = payoutService;
    }

    @Override
    @Transactional
    public OrderResponse checkout(Long userId, CheckoutRequest request, String clientIp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.getRole() == null || user.getRole().getName() != RoleName.CUSTOMER) {
            throw new BadRequestException("Business and admin accounts cannot purchase products. Please use a customer account to shop.");
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Your cart is empty"));

        List<CartItem> activeItems = cart.getItems().stream()
                .filter(item -> !item.isSavedForLater())
                .collect(Collectors.toList());

        if (activeItems.isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", request.getAddressId()));
        if (!address.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("This address does not belong to you");
        }

        // Validate stock availability, and (for COD orders) that every item's
        // seller actually allows Cash on Delivery - COD eligibility is set
        // per-product by the business admin, not assumed platform-wide.
        BigDecimal codAdvanceTotal = BigDecimal.ZERO;
        for (CartItem item : activeItems) {
            if (item.getProduct() != null) {
                Integer available = item.getProduct().getStock();
                if (available == null || available < item.getQuantity()) {
                    throw new BadRequestException("Insufficient stock for product: " + item.getProduct().getName());
                }
                if (request.getPaymentMethod() == PaymentMethod.COD) {
                    if (!item.getProduct().isCodEnabled()) {
                        throw new BadRequestException("Cash on Delivery is not available for \"" +
                                item.getProduct().getName() + "\". Please choose online payment or remove this item.");
                    }
                    BigDecimal advance = item.getProduct().getCodAdvanceAmount();
                    if (advance != null && advance.compareTo(BigDecimal.ZERO) > 0) {
                        codAdvanceTotal = codAdvanceTotal.add(advance.multiply(BigDecimal.valueOf(item.getQuantity())));
                    }
                }
            }
        }

        BigDecimal subtotal = activeItems.stream()
                .map(item -> item.getPriceAtAdd().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmount = BigDecimal.ZERO;
        Coupon appliedCoupon = null;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            discountAmount = couponService.validateAndCalculateDiscount(request.getCouponCode(), subtotal);
            appliedCoupon = couponRepository.findByCodeAndActiveTrue(request.getCouponCode()).orElse(null);
        }

        BigDecimal taxableAmount = subtotal.subtract(discountAmount);
        BigDecimal taxAmount = taxableAmount.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal shippingAmount = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 ? BigDecimal.ZERO : SHIPPING_FLAT_RATE;
        BigDecimal grandTotal = taxableAmount.add(taxAmount).add(shippingAmount).setScale(2, RoundingMode.HALF_UP);

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUser(user);
        order.setAddress(address);
        order.setCoupon(appliedCoupon);
        order.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        order.setTaxAmount(taxAmount);
        order.setShippingAmount(shippingAmount);
        order.setDiscountAmount(discountAmount.setScale(2, RoundingMode.HALF_UP));
        order.setGrandTotal(grandTotal);
        order.setStatus(OrderStatus.PENDING);
        order.setCustomerIp(clientIp);
        try {
            IpGeolocationService.Location location = ipGeolocationService.lookup(clientIp);
            if (location != null) {
                order.setCustomerCity(location.city);
                order.setCustomerRegion(location.region);
                order.setCustomerCountry(location.country);
            }
        } catch (Exception ignored) {
            // Location is a nice-to-have for the admin panel - never block checkout over it.
        }
        order = orderRepository.save(order);

        for (CartItem item : activeItems) {
            String productName;
            Product product = item.getProduct();
            if (product != null) {
                productName = product.getName();
                product.setStock(product.getStock() - item.getQuantity());
                productRepository.save(product);
            } else {
                productName = "Item";
            }

            BigDecimal unitPrice = item.getPriceAtAdd();
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            OrderItem orderItem = new OrderItem(order, product, productName,
                    item.getSize(), item.getQuantity(), unitPrice, totalPrice);
            order.getItems().add(orderItem);
        }
        order = orderRepository.save(order);

        // Create mock payment record
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(request.getPaymentMethod());
        if (request.getPaymentMethod() == PaymentMethod.COD) {
            if (codAdvanceTotal.compareTo(BigDecimal.ZERO) > 0) {
                // COD with a required upfront advance (e.g. transport charge) -
                // this amount must be paid online before the order ships; the
                // rest is collected in cash at delivery. Cap the advance at the
                // grand total just in case a seller misconfigures a larger
                // advance than the order is even worth.
                BigDecimal advance = codAdvanceTotal.min(grandTotal).setScale(2, RoundingMode.HALF_UP);
                payment.setAmount(advance);
                payment.setCodDueAmount(grandTotal.subtract(advance).setScale(2, RoundingMode.HALF_UP));
            } else {
                // Pure COD, nothing prepaid - the full amount is collected in cash on delivery.
                payment.setAmount(grandTotal);
                payment.setCodDueAmount(BigDecimal.ZERO);
            }
            payment.setStatus(PaymentStatus.PENDING);
        } else {
            payment.setAmount(grandTotal);
            payment.setCodDueAmount(BigDecimal.ZERO);
            // UPI, CARD, NET_BANKING and RAZORPAY are all routed through the
            // Razorpay gateway and only ever marked SUCCESS once
            // verifyRazorpayPayment() has cryptographically verified the
            // payment server-side (see RazorpayService). Marking it SUCCESS
            // here unconditionally would let a customer get free orders by
            // simply choosing "Card" at checkout - never do that.
            payment.setStatus(PaymentStatus.PENDING);
        }
        payment = paymentRepository.save(payment);

        if (appliedCoupon != null) {
            couponService.incrementUsage(appliedCoupon.getCode());
        }

        // Clear the active items from the cart
        for (CartItem item : activeItems) {
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
        }
        cartRepository.save(cart);

        // Notify the customer
        notificationRepository.save(new Notification(user, "Order Placed",
                "Your order " + order.getOrderNumber() + " has been placed successfully."));

        return OrderMapper.toResponse(order, payment);
    }

    @Override
    public PageResponse<OrderResponse> getOrdersForUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.from(orderRepository.findByUserId(userId, pageable).map(this::toResponseWithPayment));
    }

    @Override
    public OrderResponse getOrderById(Long userId, Long orderId, boolean isPrivileged) {
        Order order = findOrder(orderId);
        if (!isPrivileged && (order.getUser() == null || !order.getUser().getId().equals(userId))) {
            throw new UnauthorizedException("You do not have permission to view this order");
        }
        return toResponseWithPayment(order);
    }

    @Override
    public PageResponse<OrderResponse> getOrdersForBusiness(Long businessUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.from(orderRepository.findByBusinessUserId(businessUserId, pageable).map(this::toResponseWithPayment));
    }

    @Override
    public PageResponse<OrderResponse> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.from(orderRepository.findAll(pageable).map(this::toResponseWithPayment));
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request, Long businessUserId, boolean isAdmin) {
        Order order = findOrder(orderId);

        if (!isAdmin) {
            boolean ownsItem = order.getItems().stream()
                    .anyMatch(item -> item.getProduct() != null
                            && item.getProduct().getBusinessUser() != null
                            && item.getProduct().getBusinessUser().getId().equals(businessUserId));
            if (!ownsItem) {
                throw new UnauthorizedException("You do not have permission to update this order");
            }
        }

        order.setStatus(request.getStatus());

        if (request.getStatus() == OrderStatus.DELIVERED) {
            paymentRepository.findByOrderId(order.getId()).ifPresent(payment -> {
                if (payment.getPaymentMethod() == PaymentMethod.COD && payment.getStatus() != PaymentStatus.SUCCESS) {
                    payment.setStatus(PaymentStatus.SUCCESS);
                    payment.setPaidAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                }
            });
        } else if (request.getStatus() == OrderStatus.CANCELLED) {
            // Restock items on cancellation
            for (OrderItem item : order.getItems()) {
                if (item.getProduct() != null) {
                    Product product = item.getProduct();
                    product.setStock(product.getStock() + item.getQuantity());
                    productRepository.save(product);
                }
            }
        }

        order = orderRepository.save(order);

        if (request.getStatus() == OrderStatus.DELIVERED) {
            sendInvoiceIfDelivered(order);
            generatePayoutsIfDelivered(order);
        }

        if (order.getUser() != null) {
            notificationRepository.save(new Notification(order.getUser(), "Order Update",
                    "Your order " + order.getOrderNumber() + " is now " + order.getStatus().name() + "."));
        }

        return toResponseWithPayment(order);
    }

    @Override
    @Transactional(readOnly = true)
    public UpiPaymentInfoResponse getUpiPaymentInfo(Long userId, Long orderId) {
        Order order = findOrder(orderId);
        requireOwner(order, userId);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

        if (payment.getPaymentMethod() != PaymentMethod.UPI) {
            throw new BadRequestException("This order is not set up for UPI payment");
        }

        // Determine the payee: if every item in the order comes from a single
        // business that has configured their own UPI ID, pay that business
        // directly. Otherwise, fall back to the platform collection account.
        Set<Long> businessUserIds = new LinkedHashSet<>();
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null && item.getProduct().getBusinessUser() != null) {
                businessUserIds.add(item.getProduct().getBusinessUser().getId());
            }
        }

        String payeeUpiId = platformUpiId;
        String payeeName = platformPayeeName;

        if (businessUserIds.size() == 1) {
            BusinessProfile profile = businessProfileRepository.findByUserId(businessUserIds.iterator().next()).orElse(null);
            if (profile != null && profile.getUpiId() != null && !profile.getUpiId().isBlank()) {
                payeeUpiId = profile.getUpiId();
                payeeName = profile.getBusinessName();
            }
        }

        String transactionNote = "Order " + order.getOrderNumber();
        String upiUri = buildUpiUri(payeeUpiId, payeeName, payment.getAmount(), order.getOrderNumber(), transactionNote);

        UpiPaymentInfoResponse response = new UpiPaymentInfoResponse();
        response.setOrderId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setAmount(payment.getAmount());
        response.setPayeeUpiId(payeeUpiId);
        response.setPayeeName(payeeName);
        response.setTransactionNote(transactionNote);
        response.setUpiUri(upiUri);
        response.setPaymentStatus(payment.getStatus().name());
        return response;
    }

    @Override
    @Transactional
    public OrderResponse confirmUpiPayment(Long userId, Long orderId) {
        // SECURITY: this used to blindly mark the payment SUCCESS on the
        // customer's say-so, which means anyone could get a free order just
        // by calling this endpoint without ever paying. It has been
        // superseded by createRazorpayOrder() + verifyRazorpayPayment(),
        // which cryptographically verify payment with the gateway before
        // ever marking an order paid. This endpoint is kept only so old
        // clients fail loudly instead of silently "succeeding".
        Order order = findOrder(orderId);
        requireOwner(order, userId);
        throw new BadRequestException("This payment method has been upgraded. Please refresh and pay again using the secure checkout.");
    }

    @Override
    @Transactional
    public RazorpayOrderResponse createRazorpayOrder(Long userId, Long orderId) {
        Order order = findOrder(orderId);
        requireOwner(order, userId);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

        if (payment.getPaymentMethod() == PaymentMethod.COD && payment.getCodDueAmount().compareTo(BigDecimal.ZERO) == 0) {
            // Pure COD with no advance required - nothing to pay online.
            throw new BadRequestException("This order is set up for Cash on Delivery, not online payment");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new BadRequestException("This order has already been paid for");
        }

        // Reuse the existing gateway order if one was already created for this
        // payment (e.g. the customer refreshed the payment page) instead of
        // creating a duplicate order on Razorpay's side.
        if (payment.getGatewayOrderId() == null || payment.getGatewayOrderId().isBlank()) {
            String gatewayOrderId = razorpayService.createOrder(payment.getAmount(), order.getOrderNumber());
            payment.setGatewayOrderId(gatewayOrderId);
            paymentRepository.save(payment);
        }

        RazorpayOrderResponse response = new RazorpayOrderResponse();
        response.setOrderId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setRazorpayOrderId(payment.getGatewayOrderId());
        response.setKeyId(razorpayService.getPublicKeyId());
        response.setAmount(payment.getAmount());
        response.setCurrency("INR");
        if (order.getUser() != null) {
            String fullName = (order.getUser().getFirstName() == null ? "" : order.getUser().getFirstName())
                    + " " + (order.getUser().getLastName() == null ? "" : order.getUser().getLastName());
            response.setCustomerName(fullName.trim());
            response.setCustomerEmail(order.getUser().getEmail());
            response.setCustomerPhone(order.getUser().getMobileNumber());
        }
        return response;
    }

    @Override
    @Transactional
    public OrderResponse verifyRazorpayPayment(Long userId, Long orderId, VerifyPaymentRequest request) {
        Order order = findOrder(orderId);
        requireOwner(order, userId);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

        if (payment.getPaymentMethod() == PaymentMethod.COD && payment.getCodDueAmount().compareTo(BigDecimal.ZERO) == 0) {
            throw new BadRequestException("This order is set up for Cash on Delivery, not online payment");
        }

        if (payment.getGatewayOrderId() == null || !payment.getGatewayOrderId().equals(request.getRazorpayOrderId())) {
            throw new BadRequestException("Payment does not match this order");
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            // Already verified (e.g. duplicate callback) - return the current state idempotently.
            return toResponseWithPayment(order);
        }

        boolean verified = razorpayService.verifyPaymentSignature(
                request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature());

        if (!verified) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new BadRequestException("Payment verification failed. If money was deducted, it will be refunded automatically within 5-7 business days.");
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        payment.setGatewayPaymentId(request.getRazorpayPaymentId());
        payment.setTransactionId(request.getRazorpayPaymentId());
        paymentRepository.save(payment);

        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
            order = orderRepository.save(order);
        }

        notificationRepository.save(new Notification(order.getUser(), "Payment Received",
                "We've received your payment for order " + order.getOrderNumber() + "."));

        // Generate business payouts now only for fully-paid online orders.
        // A COD order with an advance only had the advance verified here
        // (not the item value) - its payout is generated later, once the
        // order is Delivered and the rest is collected in cash.
        if (payment.getPaymentMethod() != PaymentMethod.COD) {
            payoutService.generatePayoutsForOrder(order, request.getRazorpayPaymentId());
        }

        return toResponseWithPayment(order);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<OrderResponse> searchForSupport(String query) {
        if (query == null || query.trim().length() < 2) {
            return java.util.List.of();
        }
        String q = query.trim();
        java.util.LinkedHashMap<Long, Order> results = new java.util.LinkedHashMap<>();

        // An AWB number is an exact-ish identifier - check it first so a
        // support agent pasting a tracking number gets the right order
        // even if it doesn't textually resemble an order number/email.
        shipmentRepository.findByAwbNumber(q).ifPresent(shipment -> {
            if (shipment.getOrder() != null) {
                results.put(shipment.getOrder().getId(), shipment.getOrder());
            }
        });

        for (Order order : orderRepository.searchForSupport(q, PageRequest.of(0, 20))) {
            results.putIfAbsent(order.getId(), order);
        }

        return results.values().stream().map(this::toResponseWithPayment).collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(Long userId, Long orderId, boolean isPrivileged) {
        Order order = findOrder(orderId);

        if (!isPrivileged && (order.getUser() == null || !order.getUser().getId().equals(userId))) {
            throw new UnauthorizedException("You do not have permission to view this invoice");
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestException("The invoice is available once this order has been delivered.");
        }

        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        return invoiceService.generatePdf(order, payment);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId, String reason) {
        Order order = findOrder(orderId);
        requireOwner(order, userId);

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED
                && order.getStatus() != OrderStatus.PROCESSING) {
            throw new BadRequestException("This order can no longer be cancelled - it has already shipped. " +
                    "You can request a return instead once it's delivered.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        order = orderRepository.save(order);

        // Restock items
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null) {
                Product product = item.getProduct();
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            }
        }

        // Refund any amount already paid online (advance or full prepayment)
        paymentRepository.findByOrderId(order.getId()).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                payment.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
                // NOTE: this marks the payment refunded in our records. Actually
                // returning the money via Razorpay's Refunds API is a follow-up
                // step for the admin/business to trigger from their Razorpay
                // dashboard, or a future automated integration here.
            }
        });

        notificationRepository.save(new Notification(order.getUser(), "Order Cancelled",
                "Your order " + order.getOrderNumber() + " has been cancelled."));

        return toResponseWithPayment(order);
    }

    @Override
    @Transactional
    public OrderResponse requestReturn(Long userId, Long orderId, String reason) {
        Order order = findOrder(orderId);
        requireOwner(order, userId);

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestException("Only delivered orders can be returned");
        }
        if (order.getUpdatedAt() == null || order.getUpdatedAt().isBefore(LocalDateTime.now().minusDays(RETURN_WINDOW_DAYS))) {
            throw new BadRequestException("The return window (" + RETURN_WINDOW_DAYS + " days after delivery) has passed");
        }

        order.setStatus(OrderStatus.RETURN_REQUESTED);
        order.setReturnReason(reason);
        order.setReturnRequestedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        notificationRepository.save(new Notification(order.getUser(), "Return Requested",
                "Your return request for order " + order.getOrderNumber() + " has been submitted for review."));

        return toResponseWithPayment(order);
    }

    @Override
    @Transactional
    public OrderResponse decideReturn(Long orderId, Long businessUserId, boolean isAdmin, boolean approve) {
        Order order = findOrder(orderId);

        if (!isAdmin) {
            boolean ownsItem = order.getItems().stream()
                    .anyMatch(item -> item.getProduct() != null
                            && item.getProduct().getBusinessUser() != null
                            && item.getProduct().getBusinessUser().getId().equals(businessUserId));
            if (!ownsItem) {
                throw new UnauthorizedException("You do not have permission to manage returns for this order");
            }
        }

        if (order.getStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new BadRequestException("This order does not have a pending return request");
        }

        if (approve) {
            order.setStatus(OrderStatus.RETURNED);
            order = orderRepository.save(order);

            for (OrderItem item : order.getItems()) {
                if (item.getProduct() != null) {
                    Product product = item.getProduct();
                    product.setStock(product.getStock() + item.getQuantity());
                    productRepository.save(product);
                }
            }

            paymentRepository.findByOrderId(order.getId()).ifPresent(payment -> {
                if (payment.getStatus() == PaymentStatus.SUCCESS) {
                    payment.setStatus(PaymentStatus.REFUNDED);
                    paymentRepository.save(payment);
                }
            });

            notificationRepository.save(new Notification(order.getUser(), "Return Approved",
                    "Your return for order " + order.getOrderNumber() + " has been approved and refunded."));
        } else {
            order.setStatus(OrderStatus.DELIVERED);
            order = orderRepository.save(order);

            notificationRepository.save(new Notification(order.getUser(), "Return Rejected",
                    "Your return request for order " + order.getOrderNumber() + " was not approved. " +
                            "Contact support if you have questions."));
        }

        return toResponseWithPayment(order);
    }

    /**
     * Builds a standard UPI deep link (`upi://pay?...`). Opening this URI on a
     * phone launches the user's chosen UPI app (PhonePe, Google Pay, Paytm,
     * etc.) with the amount and payee pre-filled. The same string is rendered
     * as a QR code by the frontend so it can be scanned from a desktop too.
     */
    private String buildUpiUri(String payeeUpiId, String payeeName, BigDecimal amount, String orderNumber, String note) {
        return "upi://pay" +
                "?pa=" + encode(payeeUpiId) +
                "&pn=" + encode(payeeName) +
                "&am=" + amount.setScale(2, RoundingMode.HALF_UP) +
                "&cu=INR" +
                "&tr=" + encode(orderNumber) +
                "&tn=" + encode(note);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private OrderResponse toResponseWithPayment(Order order) {
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        OrderResponse response = OrderMapper.toResponse(order, payment);
        shipmentRepository.findByOrderId(order.getId())
                .ifPresent(shipment -> response.setShipment(ShipmentMapper.toResponse(shipment)));

        // A customer can self-cancel only before it ships, and request a
        // return only within a short window after delivery.
        response.setCanCancel(order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.CONFIRMED
                || order.getStatus() == OrderStatus.PROCESSING);
        boolean withinReturnWindow = order.getStatus() == OrderStatus.DELIVERED
                && order.getUpdatedAt() != null
                && order.getUpdatedAt().isAfter(LocalDateTime.now().minusDays(RETURN_WINDOW_DAYS));
        response.setCanRequestReturn(withinReturnWindow);

        return response;
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
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
            // Invoice email is a nice-to-have, not a reason to fail the status update.
        }
    }

    /** Generates business payouts once a COD order is delivered - the cash is now considered received. */
    private void generatePayoutsIfDelivered(Order order) {
        try {
            Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
            String paymentRef = payment != null ? payment.getGatewayPaymentId() : null;
            payoutService.generatePayoutsForOrder(order, paymentRef);
        } catch (Exception e) {
            // Payout generation is best-effort here too - an admin can still see the order
            // and the payout can be created/fixed manually if this somehow fails.
        }
    }

    /**
     * Ensures the given order belongs to {@code userId}. {@code order.getUser()}
     * is a lazy-loaded association; this is safe to call from within an
     * {@code @Transactional} method (the active Hibernate session resolves the
     * proxy), but will throw {@code LazyInitializationException} if called
     * outside one.
     */
    private void requireOwner(Order order, Long userId) {
        User owner = order.getUser();
        if (owner == null || owner.getId() == null || userId == null || !owner.getId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to access this order");
        }
    }

    private String generateOrderNumber() {
        String prefix = "TH" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String orderNumber;
        do {
            orderNumber = prefix + "-" + String.format("%05d", (int) (Math.random() * 100000));
        } while (orderRepository.findByOrderNumber(orderNumber).isPresent());
        return orderNumber;
    }
}

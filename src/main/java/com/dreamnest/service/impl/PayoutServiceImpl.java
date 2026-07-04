package com.dreamnest.service.impl;

import com.dreamnest.dto.request.MarkPayoutPaidRequest;
import com.dreamnest.dto.response.PageResponse;
import com.dreamnest.dto.response.PayoutResponse;
import com.dreamnest.dto.response.PayoutStatusHistoryResponse;
import com.dreamnest.dto.response.PayoutSummaryResponse;
import com.dreamnest.entity.*;
import com.dreamnest.enums.PayoutMethod;
import com.dreamnest.enums.PayoutStatus;
import com.dreamnest.exception.BadRequestException;
import com.dreamnest.exception.ResourceNotFoundException;
import com.dreamnest.exception.UnauthorizedException;
import com.dreamnest.repository.*;
import com.dreamnest.service.PayoutService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PayoutServiceImpl implements PayoutService {

    private final PayoutRepository payoutRepository;
    private final PayoutStatusHistoryRepository historyRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Value("${dreamnest.payout.platform-commission-percent}")
    private BigDecimal commissionPercent;

    public PayoutServiceImpl(PayoutRepository payoutRepository,
                              PayoutStatusHistoryRepository historyRepository,
                              BusinessProfileRepository businessProfileRepository,
                              NotificationRepository notificationRepository,
                              UserRepository userRepository) {
        this.payoutRepository = payoutRepository;
        this.historyRepository = historyRepository;
        this.businessProfileRepository = businessProfileRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void generatePayoutsForOrder(Order order, String razorpayPaymentId) {
        // Group order items by which business they belong to - a single
        // order can contain products from multiple sellers, and each only
        // gets credited for their own items, not the whole order.
        Map<User, BigDecimal> subtotalByBusiness = new LinkedHashMap<>();
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() == null || item.getProduct().getBusinessUser() == null) {
                continue;
            }
            User business = item.getProduct().getBusinessUser();
            subtotalByBusiness.merge(business, item.getTotalPrice(), BigDecimal::add);
        }

        for (Map.Entry<User, BigDecimal> entry : subtotalByBusiness.entrySet()) {
            User business = entry.getKey();
            BigDecimal subtotal = entry.getValue();

            // Idempotent: don't create a duplicate payout if one already exists for this order+business.
            if (payoutRepository.findByOrderIdAndBusinessUserId(order.getId(), business.getId()).isPresent()) {
                continue;
            }

            BigDecimal commission = subtotal.multiply(commissionPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal businessShare = subtotal.subtract(commission);

            Payout payout = new Payout();
            payout.setOrder(order);
            payout.setBusinessUser(business);
            payout.setRazorpayPaymentId(razorpayPaymentId);
            payout.setAmountPaid(subtotal);
            payout.setPlatformCommission(commission);
            payout.setBusinessShare(businessShare);
            payout.setStatus(PayoutStatus.PENDING);
            payout = payoutRepository.save(payout);

            historyRepository.save(new PayoutStatusHistory(payout, null, PayoutStatus.PENDING, null,
                    "Auto-generated from order " + order.getOrderNumber()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PayoutResponse> getAllPayouts(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Payout> result = (status != null && !status.isBlank())
                ? payoutRepository.findByStatus(PayoutStatus.valueOf(status.toUpperCase()), pageable)
                : payoutRepository.findAll(pageable);
        return toPageResponse(result);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PayoutResponse> getPayoutsForBusiness(Long businessUserId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Payout> result = (status != null && !status.isBlank())
                ? payoutRepository.findByBusinessUserIdAndStatus(businessUserId, PayoutStatus.valueOf(status.toUpperCase()), pageable)
                : payoutRepository.findByBusinessUserId(businessUserId, pageable);
        return toPageResponse(result);
    }

    @Override
    @Transactional(readOnly = true)
    public PayoutResponse getPayoutById(Long payoutId, Long requesterId, boolean isAdmin) {
        Payout payout = findPayout(payoutId);
        if (!isAdmin && !payout.getBusinessUser().getId().equals(requesterId)) {
            throw new UnauthorizedException("You do not have permission to view this payout");
        }
        return toResponse(payout);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayoutStatusHistoryResponse> getHistory(Long payoutId) {
        return historyRepository.findByPayoutIdOrderByChangedAtDesc(payoutId).stream()
                .map(this::toHistoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PayoutResponse updateStatus(Long payoutId, MarkPayoutPaidRequest request, Long adminUserId) {
        Payout payout = findPayout(payoutId);
        PayoutStatus oldStatus = payout.getStatus();
        PayoutStatus newStatus;
        try {
            newStatus = PayoutStatus.valueOf(request.getStatus().toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("Invalid payout status");
        }

        payout.setStatus(newStatus);
        if (request.getPayoutMethod() != null && !request.getPayoutMethod().isBlank()) {
            payout.setPayoutMethod(PayoutMethod.valueOf(request.getPayoutMethod().toUpperCase()));
        }
        if (request.getUtrNumber() != null) {
            payout.setUtrNumber(request.getUtrNumber().isBlank() ? null : request.getUtrNumber().trim());
        }
        if (request.getRemarks() != null) {
            payout.setRemarks(request.getRemarks().isBlank() ? null : request.getRemarks().trim());
        }
        if (request.getProofUrl() != null && !request.getProofUrl().isBlank()) {
            payout.setProofUrl(request.getProofUrl());
            payout.setProofName(request.getProofName());
        }
        if (newStatus == PayoutStatus.PAID && payout.getPayoutDate() == null) {
            payout.setPayoutDate(LocalDateTime.now());
        }

        payout = payoutRepository.save(payout);

        User admin = adminUserId != null ? userRepository.findById(adminUserId).orElse(null) : null;
        historyRepository.save(new PayoutStatusHistory(payout, oldStatus, newStatus, admin, request.getRemarks()));

        if (newStatus == PayoutStatus.PAID) {
            notificationRepository.save(new Notification(payout.getBusinessUser(), "Payout Completed",
                    "Your payout of Rs. " + payout.getBusinessShare() + " for order " + payout.getOrder().getOrderNumber()
                            + " has been marked as paid." + (payout.getUtrNumber() != null ? " Ref: " + payout.getUtrNumber() : "")));
        }

        return toResponse(payout);
    }

    @Override
    @Transactional
    public PayoutResponse raiseDispute(Long payoutId, Long businessUserId, String note) {
        Payout payout = findPayout(payoutId);
        if (!payout.getBusinessUser().getId().equals(businessUserId)) {
            throw new UnauthorizedException("You do not have permission to dispute this payout");
        }
        PayoutStatus oldStatus = payout.getStatus();
        payout.setStatus(PayoutStatus.DISPUTED);
        payout.setDisputeNote(note);
        payout = payoutRepository.save(payout);

        historyRepository.save(new PayoutStatusHistory(payout, oldStatus, PayoutStatus.DISPUTED, payout.getBusinessUser(), note));

        return toResponse(payout);
    }

    @Override
    @Transactional(readOnly = true)
    public PayoutSummaryResponse getSummaryForBusiness(Long businessUserId) {
        BigDecimal pending = payoutRepository.sumPendingForBusiness(businessUserId);
        BigDecimal paid = payoutRepository.sumPaidForBusiness(businessUserId);
        return new PayoutSummaryResponse(pending, paid);
    }

    private PageResponse<PayoutResponse> toPageResponse(Page<Payout> page) {
        PageResponse<PayoutResponse> response = new PageResponse<>();
        response.setContent(page.getContent().stream().map(this::toResponse).collect(Collectors.toList()));
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setLast(page.isLast());
        return response;
    }

    private PayoutResponse toResponse(Payout payout) {
        PayoutResponse response = new PayoutResponse();
        response.setId(payout.getId());
        response.setOrderId(payout.getOrder().getId());
        response.setOrderNumber(payout.getOrder().getOrderNumber());
        response.setBusinessUserId(payout.getBusinessUser().getId());
        response.setBusinessName(resolveBusinessName(payout.getBusinessUser()));
        response.setCustomerName(payout.getOrder().getUser() != null
                ? (payout.getOrder().getUser().getFirstName() + " " + payout.getOrder().getUser().getLastName()).trim()
                : null);
        response.setRazorpayPaymentId(payout.getRazorpayPaymentId());
        response.setAmountPaid(payout.getAmountPaid());
        response.setPlatformCommission(payout.getPlatformCommission());
        response.setBusinessShare(payout.getBusinessShare());
        response.setStatus(payout.getStatus().name());
        response.setPayoutMethod(payout.getPayoutMethod() != null ? payout.getPayoutMethod().name() : null);
        response.setUtrNumber(payout.getUtrNumber());
        response.setPayoutDate(payout.getPayoutDate());
        response.setRemarks(payout.getRemarks());
        response.setProofUrl(payout.getProofUrl());
        response.setProofName(payout.getProofName());
        response.setDisputeNote(payout.getDisputeNote());
        response.setCreatedAt(payout.getCreatedAt());
        response.setUpdatedAt(payout.getUpdatedAt());

        businessProfileRepository.findByUserId(payout.getBusinessUser().getId()).ifPresent(profile -> {
            response.setUpiId(profile.getUpiId());
            response.setBankAccountNumber(profile.getBankAccountNumber());
            response.setIfscCode(profile.getIfscCode());
            response.setBankName(profile.getBankName());
            response.setAccountHolderName(profile.getAccountHolderName());
        });

        return response;
    }

    private PayoutStatusHistoryResponse toHistoryResponse(PayoutStatusHistory history) {
        PayoutStatusHistoryResponse response = new PayoutStatusHistoryResponse();
        response.setId(history.getId());
        response.setOldStatus(history.getOldStatus() != null ? history.getOldStatus().name() : null);
        response.setNewStatus(history.getNewStatus().name());
        response.setChangedByName(history.getChangedBy() != null
                ? (history.getChangedBy().getFirstName() + " " + history.getChangedBy().getLastName()).trim()
                : "System");
        response.setNote(history.getNote());
        response.setChangedAt(history.getChangedAt());
        return response;
    }

    private String resolveBusinessName(User business) {
        return businessProfileRepository.findByUserId(business.getId())
                .map(BusinessProfile::getBusinessName)
                .orElse((business.getFirstName() + " " + business.getLastName()).trim());
    }

    private Payout findPayout(Long id) {
        return payoutRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payout", "id", id));
    }
}

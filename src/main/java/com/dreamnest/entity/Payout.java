package com.dreamnest.entity;

import com.dreamnest.enums.PayoutMethod;
import com.dreamnest.enums.PayoutStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tracks what the platform owes a business for a specific order's payment,
 * and the admin's record of actually paying it out. One Payout is created
 * automatically per order once its payment is confirmed (online payment
 * verified, or COD order marked Delivered) - see PayoutService.
 */
@Entity
@Table(name = "payouts", uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "business_user_id"}))
public class Payout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_user_id", nullable = false)
    private User businessUser;

    @Column(name = "razorpay_payment_id", length = 100)
    private String razorpayPaymentId;

    @Column(name = "amount_paid", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "platform_commission", nullable = false, precision = 10, scale = 2)
    private BigDecimal platformCommission;

    @Column(name = "business_share", nullable = false, precision = 10, scale = 2)
    private BigDecimal businessShare;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayoutStatus status = PayoutStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payout_method", length = 20)
    private PayoutMethod payoutMethod;

    @Column(name = "utr_number", length = 100)
    private String utrNumber;

    @Column(name = "payout_date")
    private LocalDateTime payoutDate;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "proof_url", length = 500)
    private String proofUrl;

    @Column(name = "proof_name", length = 255)
    private String proofName;

    @Column(name = "dispute_note", columnDefinition = "TEXT")
    private String disputeNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Payout() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public User getBusinessUser() {
        return businessUser;
    }

    public void setBusinessUser(User businessUser) {
        this.businessUser = businessUser;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public BigDecimal getPlatformCommission() {
        return platformCommission;
    }

    public void setPlatformCommission(BigDecimal platformCommission) {
        this.platformCommission = platformCommission;
    }

    public BigDecimal getBusinessShare() {
        return businessShare;
    }

    public void setBusinessShare(BigDecimal businessShare) {
        this.businessShare = businessShare;
    }

    public PayoutStatus getStatus() {
        return status;
    }

    public void setStatus(PayoutStatus status) {
        this.status = status;
    }

    public PayoutMethod getPayoutMethod() {
        return payoutMethod;
    }

    public void setPayoutMethod(PayoutMethod payoutMethod) {
        this.payoutMethod = payoutMethod;
    }

    public String getUtrNumber() {
        return utrNumber;
    }

    public void setUtrNumber(String utrNumber) {
        this.utrNumber = utrNumber;
    }

    public LocalDateTime getPayoutDate() {
        return payoutDate;
    }

    public void setPayoutDate(LocalDateTime payoutDate) {
        this.payoutDate = payoutDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getProofUrl() {
        return proofUrl;
    }

    public void setProofUrl(String proofUrl) {
        this.proofUrl = proofUrl;
    }

    public String getProofName() {
        return proofName;
    }

    public void setProofName(String proofName) {
        this.proofName = proofName;
    }

    public String getDisputeNote() {
        return disputeNote;
    }

    public void setDisputeNote(String disputeNote) {
        this.disputeNote = disputeNote;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

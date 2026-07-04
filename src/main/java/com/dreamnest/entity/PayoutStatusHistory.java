package com.dreamnest.entity;

import com.dreamnest.enums.PayoutStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Audit trail entry: records every status change made to a Payout, by whom, and when. */
@Entity
@Table(name = "payout_status_history")
public class PayoutStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payout_id", nullable = false)
    private Payout payout;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 20)
    private PayoutStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private PayoutStatus newStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id")
    private User changedBy;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    public PayoutStatusHistory() {
    }

    public PayoutStatusHistory(Payout payout, PayoutStatus oldStatus, PayoutStatus newStatus, User changedBy, String note) {
        this.payout = payout;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.note = note;
    }

    @PrePersist
    protected void onCreate() {
        this.changedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Payout getPayout() {
        return payout;
    }

    public PayoutStatus getOldStatus() {
        return oldStatus;
    }

    public PayoutStatus getNewStatus() {
        return newStatus;
    }

    public User getChangedBy() {
        return changedBy;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}

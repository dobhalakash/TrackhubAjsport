package com.dreamnest.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A single message in the support conversation between a business admin and
 * the platform's Super Admin. Each business has exactly one continuous
 * thread (like a support inbox), identified by {@code businessUser} -
 * simpler than a full ticketing system, and matches how small support
 * teams actually operate.
 */
@Entity
@Table(name = "support_messages")
public class SupportMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The business this thread belongs to (always the business admin's user, regardless of who sent this message). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_user_id", nullable = false)
    private User businessUser;

    /** Who actually sent this particular message. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "sender_role", nullable = false, length = 20)
    private String senderRole; // "BUSINESS" or "ADMIN"

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Column(name = "attachment_name", length = 255)
    private String attachmentName;

    @Column(name = "read_by_admin", nullable = false)
    private boolean readByAdmin = false;

    @Column(name = "read_by_business", nullable = false)
    private boolean readByBusiness = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public SupportMessage() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getBusinessUser() {
        return businessUser;
    }

    public void setBusinessUser(User businessUser) {
        this.businessUser = businessUser;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public void setAttachmentName(String attachmentName) {
        this.attachmentName = attachmentName;
    }

    public boolean isReadByAdmin() {
        return readByAdmin;
    }

    public void setReadByAdmin(boolean readByAdmin) {
        this.readByAdmin = readByAdmin;
    }

    public boolean isReadByBusiness() {
        return readByBusiness;
    }

    public void setReadByBusiness(boolean readByBusiness) {
        this.readByBusiness = readByBusiness;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

package com.dreamnest.dto.response;

import java.time.LocalDateTime;

/** One row in the Super Admin's support inbox: a business thread with its latest message. */
public class SupportThreadSummaryResponse {

    private Long businessUserId;
    private String businessName;
    private String lastMessage;
    private String lastSenderRole;
    private LocalDateTime lastMessageAt;
    private long unreadCount;

    public Long getBusinessUserId() {
        return businessUserId;
    }

    public void setBusinessUserId(Long businessUserId) {
        this.businessUserId = businessUserId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastSenderRole() {
        return lastSenderRole;
    }

    public void setLastSenderRole(String lastSenderRole) {
        this.lastSenderRole = lastSenderRole;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }
}

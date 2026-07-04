package com.dreamnest.service;

import com.dreamnest.dto.response.NotificationResponse;
import com.dreamnest.dto.response.PageResponse;

/**
 * Manages user notifications.
 */
public interface NotificationService {

    PageResponse<NotificationResponse> getNotifications(Long userId, int page, int size);

    long getUnreadCount(Long userId);

    void markAsRead(Long userId, Long notificationId);

    void markAllAsRead(Long userId);
}

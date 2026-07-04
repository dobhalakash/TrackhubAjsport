package com.dreamnest.mapper;

import com.dreamnest.dto.response.NotificationResponse;
import com.dreamnest.entity.Notification;

/**
 * Maps {@link Notification} entities to response DTOs.
 */
public class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationResponse toResponse(Notification notification) {
        if (notification == null) {
            return null;
        }
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setTitle(notification.getTitle());
        response.setMessage(notification.getMessage());
        response.setRead(notification.isRead());
        response.setCreatedAt(notification.getCreatedAt());
        return response;
    }
}

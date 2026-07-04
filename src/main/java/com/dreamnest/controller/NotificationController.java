package com.dreamnest.controller;

import com.dreamnest.dto.response.ApiResponse;
import com.dreamnest.dto.response.NotificationResponse;
import com.dreamnest.dto.response.PageResponse;
import com.dreamnest.service.NotificationService;
import com.dreamnest.util.SecurityUtil;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints for managing the authenticated user's notifications.
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(notificationService.getNotifications(userId(), page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> getUnreadCount() {
        return ApiResponse.success(Map.of("count", notificationService.getUnreadCount(userId())));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(userId(), id);
        return ApiResponse.success("Notification marked as read", null);
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead() {
        notificationService.markAllAsRead(userId());
        return ApiResponse.success("All notifications marked as read", null);
    }

    private Long userId() {
        return SecurityUtil.getCurrentUserId();
    }
}

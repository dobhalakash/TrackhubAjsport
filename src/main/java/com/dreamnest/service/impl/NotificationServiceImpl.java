package com.dreamnest.service.impl;

import com.dreamnest.dto.response.NotificationResponse;
import com.dreamnest.dto.response.PageResponse;
import com.dreamnest.entity.Notification;
import com.dreamnest.exception.ResourceNotFoundException;
import com.dreamnest.exception.UnauthorizedException;
import com.dreamnest.mapper.NotificationMapper;
import com.dreamnest.repository.NotificationRepository;
import com.dreamnest.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link NotificationService}.
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public PageResponse<NotificationResponse> getNotifications(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return PageResponse.from(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationMapper::toResponse));
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        if (!notification.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to access this notification");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.findByUserIdAndReadFalse(userId).forEach(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }
}

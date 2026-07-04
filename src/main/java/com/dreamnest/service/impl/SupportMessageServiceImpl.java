package com.dreamnest.service.impl;

import com.dreamnest.dto.request.SendSupportMessageRequest;
import com.dreamnest.dto.response.SupportMessageResponse;
import com.dreamnest.dto.response.SupportThreadSummaryResponse;
import com.dreamnest.entity.BusinessProfile;
import com.dreamnest.entity.Notification;
import com.dreamnest.entity.SupportMessage;
import com.dreamnest.entity.User;
import com.dreamnest.exception.BadRequestException;
import com.dreamnest.exception.ResourceNotFoundException;
import com.dreamnest.mapper.SupportMessageMapper;
import com.dreamnest.repository.BusinessProfileRepository;
import com.dreamnest.repository.NotificationRepository;
import com.dreamnest.repository.SupportMessageRepository;
import com.dreamnest.repository.UserRepository;
import com.dreamnest.service.SupportMessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupportMessageServiceImpl implements SupportMessageService {

    private final SupportMessageRepository supportMessageRepository;
    private final UserRepository userRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final NotificationRepository notificationRepository;

    public SupportMessageServiceImpl(SupportMessageRepository supportMessageRepository,
                                      UserRepository userRepository,
                                      BusinessProfileRepository businessProfileRepository,
                                      NotificationRepository notificationRepository) {
        this.supportMessageRepository = supportMessageRepository;
        this.userRepository = userRepository;
        this.businessProfileRepository = businessProfileRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public SupportMessageResponse sendAsBusiness(Long businessUserId, SendSupportMessageRequest request) {
        validateContent(request);
        User business = findUser(businessUserId);

        SupportMessage message = new SupportMessage();
        message.setBusinessUser(business);
        message.setSender(business);
        message.setSenderRole("BUSINESS");
        message.setMessage(request.getMessage());
        message.setAttachmentUrl(request.getAttachmentUrl());
        message.setAttachmentName(request.getAttachmentName());
        message.setReadByBusiness(true); // they just sent it
        message = supportMessageRepository.save(message);

        return enrich(SupportMessageMapper.toResponse(message), business);
    }

    @Override
    @Transactional
    public SupportMessageResponse sendAsAdmin(Long businessUserId, Long adminUserId, SendSupportMessageRequest request) {
        validateContent(request);
        User business = findUser(businessUserId);
        User admin = findUser(adminUserId);

        SupportMessage message = new SupportMessage();
        message.setBusinessUser(business);
        message.setSender(admin);
        message.setSenderRole("ADMIN");
        message.setMessage(request.getMessage());
        message.setAttachmentUrl(request.getAttachmentUrl());
        message.setAttachmentName(request.getAttachmentName());
        message.setReadByAdmin(true); // they just sent it
        message = supportMessageRepository.save(message);

        notificationRepository.save(new Notification(business, "Reply from TrackHub Support",
                request.getMessage() != null && !request.getMessage().isBlank()
                        ? truncate(request.getMessage(), 140)
                        : "Sent you an attachment."));

        return enrich(SupportMessageMapper.toResponse(message), business);
    }

    @Override
    @Transactional
    public List<SupportMessageResponse> getThreadForBusiness(Long businessUserId) {
        User business = findUser(businessUserId);
        List<SupportMessage> thread = supportMessageRepository.findByBusinessUserIdOrderByCreatedAtAsc(businessUserId);

        thread.stream()
                .filter(m -> "ADMIN".equals(m.getSenderRole()) && !m.isReadByBusiness())
                .forEach(m -> m.setReadByBusiness(true));
        supportMessageRepository.saveAll(thread);

        return thread.stream().map(m -> enrich(SupportMessageMapper.toResponse(m), business)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<SupportMessageResponse> getThreadForAdmin(Long businessUserId) {
        User business = findUser(businessUserId);
        List<SupportMessage> thread = supportMessageRepository.findByBusinessUserIdOrderByCreatedAtAsc(businessUserId);

        thread.stream()
                .filter(m -> "BUSINESS".equals(m.getSenderRole()) && !m.isReadByAdmin())
                .forEach(m -> m.setReadByAdmin(true));
        supportMessageRepository.saveAll(thread);

        return thread.stream().map(m -> enrich(SupportMessageMapper.toResponse(m), business)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportThreadSummaryResponse> getInboxForAdmin() {
        return supportMessageRepository.findLatestPerBusinessThread().stream()
                .map(latest -> {
                    SupportThreadSummaryResponse summary = new SupportThreadSummaryResponse();
                    Long businessUserId = latest.getBusinessUser().getId();
                    summary.setBusinessUserId(businessUserId);
                    summary.setBusinessName(resolveBusinessName(latest.getBusinessUser()));
                    summary.setLastMessage(latest.getMessage() != null ? truncate(latest.getMessage(), 80)
                            : (latest.getAttachmentName() != null ? "Attachment: " + latest.getAttachmentName() : ""));
                    summary.setLastSenderRole(latest.getSenderRole());
                    summary.setLastMessageAt(latest.getCreatedAt());
                    summary.setUnreadCount(supportMessageRepository.countUnreadForAdmin(businessUserId));
                    return summary;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCountForBusiness(Long businessUserId) {
        return supportMessageRepository.countUnreadForBusiness(businessUserId);
    }

    private SupportMessageResponse enrich(SupportMessageResponse response, User business) {
        response.setBusinessName(resolveBusinessName(business));
        return response;
    }

    private String resolveBusinessName(User business) {
        BusinessProfile profile = businessProfileRepository.findByUserId(business.getId()).orElse(null);
        if (profile != null && profile.getBusinessName() != null && !profile.getBusinessName().isBlank()) {
            return profile.getBusinessName();
        }
        return (business.getFirstName() + " " + business.getLastName()).trim();
    }

    private void validateContent(SendSupportMessageRequest request) {
        boolean hasMessage = request.getMessage() != null && !request.getMessage().isBlank();
        boolean hasAttachment = request.getAttachmentUrl() != null && !request.getAttachmentUrl().isBlank();
        if (!hasMessage && !hasAttachment) {
            throw new BadRequestException("Message cannot be empty");
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
}

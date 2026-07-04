package com.dreamnest.mapper;

import com.dreamnest.dto.response.SupportMessageResponse;
import com.dreamnest.entity.SupportMessage;

public class SupportMessageMapper {

    private SupportMessageMapper() {
    }

    public static SupportMessageResponse toResponse(SupportMessage message) {
        if (message == null) {
            return null;
        }
        SupportMessageResponse response = new SupportMessageResponse();
        response.setId(message.getId());
        response.setBusinessUserId(message.getBusinessUser() != null ? message.getBusinessUser().getId() : null);
        response.setBusinessName(message.getBusinessUser() != null
                ? (message.getBusinessUser().getFirstName() + " " + message.getBusinessUser().getLastName()).trim()
                : null);
        response.setSenderRole(message.getSenderRole());
        response.setSenderName(message.getSender() != null
                ? (message.getSender().getFirstName() + " " + message.getSender().getLastName()).trim()
                : null);
        response.setMessage(message.getMessage());
        response.setAttachmentUrl(message.getAttachmentUrl());
        response.setAttachmentName(message.getAttachmentName());
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }
}

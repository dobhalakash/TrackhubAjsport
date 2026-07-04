package com.dreamnest.service;

import com.dreamnest.dto.request.SendSupportMessageRequest;
import com.dreamnest.dto.response.SupportMessageResponse;
import com.dreamnest.dto.response.SupportThreadSummaryResponse;

import java.util.List;

/**
 * Manages the support chat between business admins and the platform's
 * Super Admin - lets a business raise an issue and get help without
 * leaving the dashboard, with optional document/image attachments.
 */
public interface SupportMessageService {

    /** Business admin sends a message to the platform admin. */
    SupportMessageResponse sendAsBusiness(Long businessUserId, SendSupportMessageRequest request);

    /** Admin replies on a specific business's thread. */
    SupportMessageResponse sendAsAdmin(Long businessUserId, Long adminUserId, SendSupportMessageRequest request);

    /** The business admin's own thread, oldest first. Marks admin's messages as read by the business. */
    List<SupportMessageResponse> getThreadForBusiness(Long businessUserId);

    /** Admin's view of a specific business's thread. Marks business's messages as read by the admin. */
    List<SupportMessageResponse> getThreadForAdmin(Long businessUserId);

    /** Admin's inbox: one row per business with an open thread, most recently active first. */
    List<SupportThreadSummaryResponse> getInboxForAdmin();

    /** Unread count for the business admin's own thread (used for a notification badge). */
    long getUnreadCountForBusiness(Long businessUserId);
}

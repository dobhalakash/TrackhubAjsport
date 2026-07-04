package com.dreamnest.service.impl;

import com.dreamnest.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Development/demo SMS sender: logs the OTP instead of sending a real text
 * message, since no SMS gateway credentials are configured by default
 * ({@code dreamnest.sms.enabled=false}).
 *
 * <p><b>Go live:</b> replace the body of {@link #sendOtp} with an HTTP call
 * to your SMS gateway of choice (e.g. MSG91, Twilio, Fast2SMS), using
 * {@code dreamnest.sms.api-key} / {@code dreamnest.sms.sender-id} (set via
 * the {@code SMS_API_KEY} / {@code SMS_SENDER_ID} environment variables),
 * and flip {@code dreamnest.sms.enabled=true} (env var {@code SMS_ENABLED}).</p>
 */
@Service
public class ConsoleSmsServiceImpl implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSmsServiceImpl.class);

    @Value("${dreamnest.sms.enabled:false}")
    private boolean enabled;

    @Value("${dreamnest.sms.api-key:}")
    private String apiKey;

    @Value("${dreamnest.sms.sender-id:DRMNST}")
    private String senderId;

    @Override
    public void sendOtp(String mobileNumber, String code, int expiryMinutes) {
        if (!enabled || apiKey == null || apiKey.isBlank() || apiKey.startsWith("DUMMY")) {
            log.warn("[DEV SMS] Real SMS gateway not configured (dreamnest.sms.* is using dummy/disabled values). " +
                    "Would send to {}: 'Your TrackHub verification code is {} (valid {} min)'",
                    mobileNumber, code, expiryMinutes);
            return;
        }

        // TODO (production): call your SMS gateway's HTTP API here using
        // apiKey/senderId, e.g.:
        //   POST https://api.msg91.com/api/v5/otp?otp={code}&mobile={mobileNumber}&authkey={apiKey}
        log.info("Sending SMS OTP via gateway to {}", mobileNumber);
    }
}

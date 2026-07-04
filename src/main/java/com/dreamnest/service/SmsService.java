package com.dreamnest.service;

/**
 * Sends SMS OTPs for mobile number verification. Implement against a real
 * gateway (MSG91, Twilio, Fast2SMS, etc.) before going live - see
 * {@link com.dreamnest.service.impl.ConsoleSmsServiceImpl} for the
 * development fallback currently wired up.
 */
public interface SmsService {

    void sendOtp(String mobileNumber, String code, int expiryMinutes);
}

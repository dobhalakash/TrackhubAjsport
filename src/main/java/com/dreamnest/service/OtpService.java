package com.dreamnest.service;

import com.dreamnest.entity.User;
import com.dreamnest.entity.VerificationCode;
import com.dreamnest.enums.VerificationChannel;
import com.dreamnest.exception.BadRequestException;
import com.dreamnest.repository.VerificationCodeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Generates, sends, and verifies one-time codes for email/mobile
 * verification at registration.
 */
@Service
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final VerificationCodeRepository verificationCodeRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    @Value("${dreamnest.otp.expiry-minutes:10}")
    private int expiryMinutes;

    @Value("${dreamnest.otp.length:6}")
    private int otpLength;

    public OtpService(VerificationCodeRepository verificationCodeRepository,
                       EmailService emailService,
                       SmsService smsService) {
        this.verificationCodeRepository = verificationCodeRepository;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    /**
     * Generates and dispatches an email verification code. Best-effort: if
     * the mail server isn't configured (dev/dummy credentials), registration
     * still succeeds - see {@link EmailService}.
     */
    @Transactional
    public void sendEmailVerification(User user) {
        String code = generateCode();
        verificationCodeRepository.save(new VerificationCode(user, VerificationChannel.EMAIL, code, expiresAt()));
        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), code, expiryMinutes);
    }

    /** Generates and dispatches an SMS verification code, if a mobile number was provided. */
    @Transactional
    public void sendMobileVerification(User user) {
        if (user.getMobileNumber() == null || user.getMobileNumber().isBlank()) {
            return;
        }
        String code = generateCode();
        verificationCodeRepository.save(new VerificationCode(user, VerificationChannel.MOBILE, code, expiresAt()));
        smsService.sendOtp(user.getMobileNumber(), code, expiryMinutes);
    }

    /** Verifies a code for the given channel, marking it consumed on success. Throws on mismatch/expiry. */
    @Transactional
    public void verify(User user, VerificationChannel channel, String submittedCode) {
        VerificationCode latest = verificationCodeRepository
                .findFirstByUserIdAndChannelAndConsumedFalseOrderByCreatedAtDesc(user.getId(), channel)
                .orElseThrow(() -> new BadRequestException("No pending verification code found. Please request a new one."));

        if (latest.isExpired()) {
            throw new BadRequestException("This code has expired. Please request a new one.");
        }
        if (!latest.getCode().equals(submittedCode)) {
            throw new BadRequestException("Incorrect verification code.");
        }

        latest.setConsumed(true);
        verificationCodeRepository.save(latest);
    }

    private String generateCode() {
        int bound = (int) Math.pow(10, otpLength);
        int value = RANDOM.nextInt(bound);
        return String.format("%0" + otpLength + "d", value);
    }

    private LocalDateTime expiresAt() {
        return LocalDateTime.now().plusMinutes(expiryMinutes);
    }
}

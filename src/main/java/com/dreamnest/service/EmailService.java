package com.dreamnest.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Sends transactional emails (verification codes, order invoices). Backed by
 * {@link JavaMailSender}, configured via {@code spring.mail.*} in
 * application.properties.
 *
 * <p>The properties shipped in this repo are DUMMY placeholders so the app
 * boots without a real mailbox. To actually deliver email in production, set
 * {@code MAIL_HOST}, {@code MAIL_USERNAME}, {@code MAIL_PASSWORD} and
 * {@code MAIL_FROM} as environment variables, pointing at a real
 * transactional email provider (SendGrid, AWS SES, Postmark) or a Gmail
 * account with an App Password.</p>
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${dreamnest.mail.from}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String firstName, String code, int expiryMinutes) {
        String subject = "Verify your TrackHub account";
        String body = "Hi " + (firstName == null || firstName.isBlank() ? "there" : firstName) + ",\n\n"
                + "Your TrackHub email verification code is: " + code + "\n\n"
                + "This code expires in " + expiryMinutes + " minutes. If you didn't create a TrackHub account, "
                + "you can safely ignore this email.\n\n"
                + "- The TrackHub Team";
        send(toEmail, subject, body);
    }

    public void sendBackInStockEmail(String toEmail, String productName, String productUrl) {
        String subject = productName + " is back in stock!";
        String body = "Good news - \"" + productName + "\" is back in stock on TrackHub.\n\n"
                + "Grab it before it sells out again: " + productUrl + "\n\n"
                + "- The TrackHub Team";
        send(toEmail, subject, body);
    }

    /**
     * Sends an email with a PDF attachment (used for order invoices). Returns
     * true if the email was handed off to the mail server successfully.
     */
    public boolean sendEmailWithPdfAttachment(String toEmail, String subject, String body,
                                               byte[] pdfBytes, String attachmentFilename) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body);
            helper.addAttachment(attachmentFilename, new org.springframework.core.io.ByteArrayResource(pdfBytes));
            mailSender.send(message);
            log.info("Email with attachment sent successfully to {} (subject: {})", toEmail, subject);
            return true;
        } catch (Exception e) {
            log.warn("Could not send invoice email to {} (is spring.mail.* configured with real credentials?): {}",
                    toEmail, e.getMessage());
            return false;
        }
    }

    private void send(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent successfully to {} (subject: {})", toEmail, subject);
        } catch (Exception e) {
            // Best-effort: registration should not fail just because the
            // (possibly still-dummy) mail server rejected the message. Log
            // loudly so it's obvious in dev that mail isn't configured yet.
            log.warn("Could not send email to {} (is spring.mail.* configured with real credentials?): {}",
                    toEmail, e.getMessage());
        }
    }
}

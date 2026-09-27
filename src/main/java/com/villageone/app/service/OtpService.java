package com.villageone.app.service;

import com.villageone.app.entity.OtpRecord;
import com.villageone.app.repository.AppConfigRepository;
import com.villageone.app.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository       otpRepo;
    private final AppConfigRepository configRepo;

    // ── Send OTP to email ──────────────────────────────────
    public void sendOtp(String email, OtpRecord.OtpType type) {

        if (email == null || !email.contains("@"))
            throw new RuntimeException("Invalid email address");

        // Delete old OTPs for same email+type
        otpRepo.deleteByMobileAndType(email, type);

        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Save to DB (using mobile field to store email — same column)
        OtpRecord record = OtpRecord.builder()
                .mobile(email)   // stores email in mobile field
                .otp(otp)
                .type(type)
                .verified(false)
                .build();
        otpRepo.save(record);

        // Send email
        sendEmailOtp(email, otp, type);

        log.info("OTP email sent: email={} type={}", email, type);
    }

    // ── Verify OTP ─────────────────────────────────────────
    public boolean verifyOtp(String email, String otp, OtpRecord.OtpType type) {
        OtpRecord record = otpRepo
                .findTopByMobileAndTypeAndVerifiedFalseOrderByCreatedAtDesc(email, type)
                .orElseThrow(() -> new RuntimeException("OTP not found. Please request a new OTP."));

        if (record.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new RuntimeException("OTP has expired. Please request a new OTP.");

        if (!record.getOtp().equals(otp))
            throw new RuntimeException("Invalid OTP. Please try again.");

        record.setVerified(true);
        otpRepo.save(record);

        log.info("OTP verified: email={} type={}", email, type);
        return true;
    }

    // ── Send OTP via Gmail SMTP ────────────────────────────
    private void sendEmailOtp(String toEmail, String otp, OtpRecord.OtpType type) {
        try {
            // Load config from DB
            String host     = configRepo.findByKeyName("email_host")
                    .map(c -> c.getKeyValue()).orElse("smtp.gmail.com");
            int    port     = Integer.parseInt(configRepo.findByKeyName("email_port")
                    .map(c -> c.getKeyValue()).orElse("587"));
            String username = configRepo.findByKeyName("email_username")
                    .map(c -> c.getKeyValue())
                    .orElseThrow(() -> new RuntimeException("Email not configured"));
            String password = configRepo.findByKeyName("email_password")
                    .map(c -> c.getKeyValue())
                    .orElseThrow(() -> new RuntimeException("Email password not configured"));
            String fromName = configRepo.findByKeyName("email_from_name")
                    .map(c -> c.getKeyValue()).orElse("VillageOne");

            // Build mail sender dynamically from DB config
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(host);
            mailSender.setPort(port);
            mailSender.setUsername(username);
            mailSender.setPassword(password);

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth",           "true");
            props.put("mail.smtp.starttls.enable","true");
            props.put("mail.smtp.ssl.trust",      "smtp.gmail.com");

            // Build email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(username, fromName);
            helper.setTo(toEmail);

            String subject = type == OtpRecord.OtpType.FORGOT_PASSWORD
                    ? "VillageOne - Password Reset OTP"
                    : "VillageOne - Verify Your Account";

            helper.setSubject(subject);
            helper.setText(buildEmailHtml(otp, type), true); // true = HTML

            mailSender.send(message);
            log.info("Email OTP sent successfully to {}", toEmail);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Email send failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send OTP email. Please try again.");
        }
    }

    // ── HTML email template ────────────────────────────────
    private String buildEmailHtml(String otp, OtpRecord.OtpType type) {
        String title = type == OtpRecord.OtpType.FORGOT_PASSWORD
                ? "Password Reset OTP"
                : "Verify Your Account";
        String message = type == OtpRecord.OtpType.FORGOT_PASSWORD
                ? "You requested to reset your VillageOne password. Use the OTP below:"
                : "Welcome to VillageOne! Use the OTP below to verify your account:";

        return "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;background:#f3f4f6;margin:0;padding:20px'>"
                + "<div style='max-width:480px;margin:0 auto;background:white;border-radius:16px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.1)'>"
                + "<div style='background:#2d7a3a;padding:24px;text-align:center'>"
                + "<h1 style='color:white;margin:0;font-size:24px'>🌿 VillageOne</h1>"
                + "</div>"
                + "<div style='padding:32px;text-align:center'>"
                + "<h2 style='color:#111827;margin:0 0 12px'>" + title + "</h2>"
                + "<p style='color:#6b7280;font-size:15px;margin:0 0 28px'>" + message + "</p>"
                + "<div style='background:#f0fdf4;border:2px dashed #2d7a3a;border-radius:12px;padding:20px;margin-bottom:24px'>"
                + "<p style='color:#6b7280;font-size:12px;margin:0 0 8px;text-transform:uppercase;letter-spacing:1px'>Your OTP</p>"
                + "<h1 style='color:#2d7a3a;font-size:40px;font-weight:800;letter-spacing:12px;margin:0'>" + otp + "</h1>"
                + "</div>"
                + "<p style='color:#9ca3af;font-size:13px;margin:0'>⏰ Valid for <strong>10 minutes</strong>. Do not share with anyone.</p>"
                + "</div>"
                + "<div style='background:#f9fafb;padding:16px;text-align:center;border-top:1px solid #e5e7eb'>"
                + "<p style='color:#9ca3af;font-size:12px;margin:0'>© 2025 VillageOne · villageone.in</p>"
                + "</div>"
                + "</div></body></html>";
    }
}
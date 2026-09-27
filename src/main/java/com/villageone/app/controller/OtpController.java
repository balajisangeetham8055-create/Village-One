package com.villageone.app.controller;

import com.villageone.app.entity.OtpRecord;
import com.villageone.app.entity.User;
import com.villageone.app.entity.WorkerSlot;
import com.villageone.app.repository.UserRepository;
import com.villageone.app.repository.WorkerSlotRepository;
import com.villageone.app.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService            otpService;
    private final UserRepository        userRepo;
    private final WorkerSlotRepository  workerSlotRepo;
    private final PasswordEncoder       passwordEncoder;

    // ── POST /api/otp/send ─────────────────────────────────
    // Accepts email (business users) OR mobile (worker users)
    @PostMapping("/send")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> body) {
        try {
            String email  = body.get("email");
            String mobile = body.get("mobile");
            String type   = body.get("type");

            String resolvedEmail = null;
            String displayMasked;

            if (email != null && !email.isBlank()) {
                // Business user — lookup by email
                boolean exists = userRepo.findAll().stream()
                        .anyMatch(u -> email.equalsIgnoreCase(u.getEmail()));
                if (!exists)
                    return ResponseEntity.badRequest().body(
                            Map.of("message", "No account found with this email address"));
                resolvedEmail = email;
                String[] parts = email.split("@");
                displayMasked = parts[0].substring(0, Math.min(2, parts[0].length())) + "****@" + parts[1];

            } else if (mobile != null && !mobile.isBlank()) {
                // Worker user — find worker by mobile, then get business owner's email
                User worker = userRepo.findByMobile(mobile).orElse(null);
                if (worker == null)
                    return ResponseEntity.badRequest().body(
                            Map.of("message", "No account found with this mobile number"));

                // Get business owner's email from WorkerSlot
                WorkerSlot slot = workerSlotRepo.findByWorkerUsername(worker.getUsername()).orElse(null);
                if (slot != null) {
                    // Send OTP to business owner's registered email
                    User owner = userRepo.findByUsername(slot.getOwnerUsername()).orElse(null);
                    if (owner != null && owner.getEmail() != null && !owner.getEmail().endsWith("@villageone.in")) {
                        resolvedEmail = owner.getEmail();
                    } else {
                        resolvedEmail = worker.getEmail(); // fallback
                    }
                } else {
                    resolvedEmail = worker.getEmail();
                }
                displayMasked = mobile.substring(0,2) + "xxxxxx" + mobile.substring(8);

            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "Email or mobile is required"));
            }

            otpService.sendOtp(resolvedEmail, OtpRecord.OtpType.valueOf(type));

            return ResponseEntity.ok(Map.of(
                    "message",       "OTP sent to " + displayMasked,
                    "resolvedEmail", resolvedEmail  // frontend uses this for verify + reset
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid OTP type"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── POST /api/otp/verify ───────────────────────────────
    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            String otp   = body.get("otp");
            String type  = body.get("type");

            otpService.verifyOtp(email, otp, OtpRecord.OtpType.valueOf(type));

            return ResponseEntity.ok(Map.of(
                    "message",  "OTP verified successfully",
                    "verified", true
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage(), "verified", false));
        }
    }

    // ── POST /api/otp/reset-password ──────────────────────
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        try {
            String email       = body.get("email");
            String newPassword = body.get("newPassword");

            if (newPassword == null || newPassword.length() < 6)
                return ResponseEntity.badRequest().body(
                        Map.of("message", "Password must be at least 6 characters"));

            // Find user by email and update password
            // OTP was already verified in /verify step — no need to verify again
            User user = userRepo.findAll().stream()
                    .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setPassword(passwordEncoder.encode(newPassword));
            userRepo.save(user);

            log.info("Password reset successful for email={}", email);
            return ResponseEntity.ok(Map.of("message", "Password reset successful! Please login."));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
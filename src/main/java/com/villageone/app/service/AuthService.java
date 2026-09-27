package com.villageone.app.service;

import com.villageone.app.dto.AppDto.*;
import com.villageone.app.entity.Plan;
import com.villageone.app.entity.User;
import com.villageone.app.repository.AppConfigRepository;
import com.villageone.app.repository.PlanRepository;
import com.villageone.app.repository.UserRepository;
import com.villageone.app.repository.WorkerSlotRepository;
import com.villageone.app.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository       userRepository;
    private final PlanRepository       planRepository;
    private final WorkerSlotRepository workerSlotRepository;
    private final AppConfigRepository  configRepository;
    private final PasswordEncoder      passwordEncoder;
    private final JwtUtil              jwtUtil;

    // ── REGISTER ─────────────────────────────────────────────
    public RegisterResponse register(RegisterRequest req) {
        boolean isWorker = "WORKER".equals(req.getRole());

        if (userRepository.existsByUsername(req.getUsername()))
            throw new RuntimeException("Username already taken. Please choose another.");
        if (userRepository.existsByMobile(req.getMobile()))
            throw new RuntimeException("Mobile number already registered.");

        LocalDate planStart = LocalDate.now();
        LocalDate planEnd;
        String planId;
        String planLabel;
        int planDays;

        if (isWorker) {
            planEnd   = planStart.plusYears(10);
            planId    = "worker";
            planLabel = "Worker";
            planDays  = 0;

        } else if ("free_trial".equals(req.getPlanId()) || req.getPlanId() == null || req.getPlanId().isBlank()) {
            int trialDays = getTrialDays();
            planEnd   = planStart.plusDays(trialDays);
            planId    = "free_trial";
            planLabel = "Free Trial";
            planDays  = trialDays;
            log.info("Free trial activated for username={} expires={}", req.getUsername(), planEnd);

        } else {
            Plan plan = planRepository.findById(req.getPlanId())
                    .orElseThrow(() -> new RuntimeException("Invalid plan selected"));
            planEnd   = planStart.plusDays(plan.getDays());
            planId    = plan.getId();
            planLabel = plan.getLabel();
            planDays  = plan.getDays();
        }

        User user = User.builder()
                .fullName(    safe(req.getFullName(),     req.getUsername()))
                .mobile(      req.getMobile())
                .email(       safe(req.getEmail(),        req.getUsername() + "@villageone.in"))
                .address(     safe(req.getAddress(),      "N/A"))
                .pincode(     safe(req.getPincode(),      "000000"))
                .storeName(   safe(req.getStoreName(),    req.getUsername()))
                .storeType(   safe(req.getStoreType(),    "General"))
                .storeAddress(safe(req.getStoreAddress(), "N/A"))
                .city(        safe(req.getCity(),         "N/A"))
                .state(       safe(req.getState(),        "N/A"))
                .storePincode(safe(req.getStorePincode(), "000000"))
                .gstNumber(   req.getGstNumber())
                .username(    req.getUsername())
                .password(    passwordEncoder.encode(req.getPassword()))
                .role(        isWorker ? User.Role.WORKER : User.Role.BUSINESS)
                .planId(      planId)
                .planStart(   planStart)
                .planEnd(     planEnd)
                .transactionId(safe(req.getTransactionId(), "free_trial"))
                .build();

        userRepository.save(user);
        log.info("Registered: username={} role={} plan={} expires={}",
                user.getUsername(), user.getRole(), planId, planEnd);

        return new RegisterResponse(
                "Registration successful! Welcome to VillageOne.",
                user.getUsername(), user.getStoreName(), planLabel, planDays, planEnd.toString()
        );
    }

    // ── LOGIN ─────────────────────────────────────────────────
    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (req.getMobile() != null && !req.getMobile().isBlank())
            if (!user.getMobile().equals(req.getMobile()))
                throw new RuntimeException("Invalid username or password");

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword()))
            throw new RuntimeException("Invalid username or password");

        boolean planExpired = false;
        int     daysLeft    = 0;
        String  planLabel   = "";
        String  planExpiry  = "";

        if (user.getRole() == User.Role.WORKER) {

            var slot = workerSlotRepository.findByWorkerUsername(user.getUsername()).orElse(null);

            if (slot != null) {

                // ── Check 1: Worker's own plan expiry ─────────────
                if (slot.getWorkerPlanEnd() != null) {
                    daysLeft  = (int) ChronoUnit.DAYS.between(LocalDate.now(), slot.getWorkerPlanEnd());
                    // Free slots have planEnd set to 10 years future — never expire
                    boolean isFreeSlot = slot.getWorkerPlanEnd().isAfter(LocalDate.now().plusYears(5));
                    if (!isFreeSlot && daysLeft < 0) {
                        planExpired = true;
                    }
                    planExpiry = slot.getWorkerPlanEnd().toString();
                    planLabel  = slot.getWorkerPlanLabel() != null ? slot.getWorkerPlanLabel() : "Free";
                }

                // ── Check 2: Owner's plan must ALSO be active ──────
                // If owner's plan expired → block ALL workers regardless of their own plan
                User owner = userRepository.findByUsername(slot.getOwnerUsername()).orElse(null);
                if (owner != null && owner.getPlanEnd() != null) {
                    boolean ownerExpired = owner.getPlanEnd().isBefore(LocalDate.now());
                    if (ownerExpired) {
                        planExpired = true;
                        planLabel   = "Owner plan expired";
                        daysLeft    = 0;
                        log.info("Worker {} blocked: owner {} plan expired on {}",
                                user.getUsername(), slot.getOwnerUsername(), owner.getPlanEnd());
                    }
                }
            }

        } else if (user.getRole() == User.Role.BUSINESS) {

            if (user.getPlanEnd() != null) {
                daysLeft    = (int) ChronoUnit.DAYS.between(LocalDate.now(), user.getPlanEnd());
                planExpired = daysLeft < 0;
                planExpiry  = user.getPlanEnd().toString();
            }
            planLabel = getPlanLabel(user.getPlanId());
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), user.getId());
        log.info("Login: username={} role={} planExpired={} daysLeft={}",
                user.getUsername(), user.getRole(), planExpired, daysLeft);

        return LoginResponse.builder()
                .token(token)
                .role(user.getRole().name())
                .username(user.getUsername())
                .storeName(user.getStoreName())
                .planId(user.getPlanId()    != null ? user.getPlanId()               : "")
                .planLabel(planLabel)
                .planStart(user.getPlanStart() != null ? user.getPlanStart().toString() : "")
                .planExpiry(planExpiry)
                .planDaysLeft(Math.max(0, daysLeft))
                .planExpired(planExpired)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────
    private int getTrialDays() {
        try {
            return Integer.parseInt(
                    configRepository.findByKeyName("free_trial_days")
                            .map(c -> c.getKeyValue()).orElse("30")
            );
        } catch (NumberFormatException e) { return 30; }
    }

    private String getPlanLabel(String planId) {
        if (planId == null) return "";
        return switch (planId) {
            case "free_trial"  -> "Free Trial";
            case "monthly"     -> "Monthly";
            case "half_yearly" -> "6 Months";
            case "yearly"      -> "1 Year";
            default            -> planId;
        };
    }

    private String safe(String v, String fallback) {
        return (v != null && !v.isBlank()) ? v : fallback;
    }
}
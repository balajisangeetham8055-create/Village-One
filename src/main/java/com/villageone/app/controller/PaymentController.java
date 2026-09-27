package com.villageone.app.controller;

import com.villageone.app.entity.*;
import com.villageone.app.repository.*;
import com.villageone.app.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final AppConfigRepository         configRepo;
    private final PlanRepository              planRepo;
    private final UserRepository              userRepo;
    private final WorkerSlotRepository        workerRepo;
    private final PaymentTransactionRepository txnRepo;
    private final JwtUtil                     jwtUtil;

    // ── GET /api/payment/razorpay-key — returns key ID only (never secret) ──
    // Called by frontend before opening Razorpay modal
    @GetMapping("/razorpay-key")
    public ResponseEntity<?> getRazorpayKey() {
        String keyId = configRepo.findByKeyName("razorpay_key_id")
                .map(AppConfig::getKeyValue)
                .orElse(null);

        if (keyId == null || keyId.isBlank())
            return ResponseEntity.status(503).body(Map.of("message", "Payment not configured. Contact admin."));

        return ResponseEntity.ok(Map.of("keyId", keyId));
    }

    // ── POST /api/payment/plan — pay for plan (registration OR upgrade) ───────
    @PostMapping("/plan")
    public ResponseEntity<?> payForPlan(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {
        try {
            String token         = authHeader.replace("Bearer ", "");
            String username      = jwtUtil.extractUsername(token);
            String planId        = body.get("planId");
            String razorpayPayId = body.get("razorpayPaymentId");
            String typeStr       = body.getOrDefault("type", "PLAN_UPGRADE");

            if (planId == null || planId.isBlank())
                return ResponseEntity.badRequest().body(Map.of("message", "Plan ID required"));
            if (razorpayPayId == null || razorpayPayId.isBlank())
                return ResponseEntity.badRequest().body(Map.of("message", "Payment ID required"));

            Plan plan = planRepo.findById(planId)
                    .orElseThrow(() -> new RuntimeException("Invalid plan: " + planId));
            User user = userRepo.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Save transaction record
            PaymentTransaction txn = PaymentTransaction.builder()
                    .username(username)
                    .type(PaymentTransaction.PaymentType.valueOf(typeStr))
                    .razorpayPaymentId(razorpayPayId)
                    .amount(plan.getPrice())
                    .planId(planId)
                    .status(PaymentTransaction.PaymentStatus.SUCCESS)
                    .notes(plan.getLabel() + " — " + plan.getDays() + " days")
                    .build();
            txnRepo.save(txn);

            // Extend plan — stack on existing if not expired
            LocalDate baseDate = (user.getPlanEnd() != null && user.getPlanEnd().isAfter(LocalDate.now()))
                    ? user.getPlanEnd()
                    : LocalDate.now();
            LocalDate newEnd = baseDate.plusDays(plan.getDays());

            user.setPlanId(planId);
            user.setPlanStart(LocalDate.now());
            user.setPlanEnd(newEnd);
            user.setTransactionId(razorpayPayId);
            userRepo.save(user);

            log.info("Plan payment: username={} plan={} txn={} newExpiry={}", username, planId, razorpayPayId, newEnd);

            return ResponseEntity.ok(Map.of(
                    "message",           "Plan activated successfully!",
                    "planId",            planId,
                    "planLabel",         plan.getLabel(),
                    "planExpiry",        newEnd.toString(),
                    "razorpayPaymentId", razorpayPayId,
                    "transactionId",     txn.getId().toString()
            ));
        } catch (RuntimeException e) {
            log.error("Plan payment failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── POST /api/payment/worker — pay for extra worker slot ─────────────────
    @PostMapping("/worker")
    public ResponseEntity<?> payForWorker(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {
        try {
            String token         = authHeader.replace("Bearer ", "");
            String username      = jwtUtil.extractUsername(token);
            String razorpayPayId = body.get("razorpayPaymentId");
            String workerUname   = body.get("workerUsername");

            if (razorpayPayId == null || razorpayPayId.isBlank())
                return ResponseEntity.badRequest().body(Map.of("message", "Payment ID required"));

            // Get worker price from DB config
            String priceStr = configRepo.findByKeyName("worker_price")
                    .map(AppConfig::getKeyValue).orElse("30");
            BigDecimal price = new BigDecimal(priceStr);

            PaymentTransaction txn = PaymentTransaction.builder()
                    .username(username)
                    .type(PaymentTransaction.PaymentType.WORKER_SLOT)
                    .razorpayPaymentId(razorpayPayId)
                    .amount(price)
                    .workerUsername(workerUname)
                    .status(PaymentTransaction.PaymentStatus.SUCCESS)
                    .notes("Worker slot: " + workerUname)
                    .build();
            txnRepo.save(txn);

            log.info("Worker payment: owner={} worker={} txn={}", username, workerUname, razorpayPayId);

            return ResponseEntity.ok(Map.of(
                    "message",           "Worker payment successful!",
                    "razorpayPaymentId", razorpayPayId,
                    "workerUsername",    workerUname != null ? workerUname : ""
            ));
        } catch (RuntimeException e) {
            log.error("Worker payment failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── GET /api/payment/history — all transactions for logged-in user ────────
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token    = authHeader.replace("Bearer ", "");
            String username = jwtUtil.extractUsername(token);
            List<PaymentTransaction> txns = txnRepo.findByUsernameOrderByCreatedAtDesc(username);
            return ResponseEntity.ok(txns);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── GET /api/payment/worker-price — returns ₹ per worker from DB ─────────
    @GetMapping("/worker-price")
    public ResponseEntity<?> getWorkerPrice() {
        String price = configRepo.findByKeyName("worker_price")
                .map(AppConfig::getKeyValue).orElse("30");
        return ResponseEntity.ok(Map.of("price", Integer.parseInt(price)));
    }
}
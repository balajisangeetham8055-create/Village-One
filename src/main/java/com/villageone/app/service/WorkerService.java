package com.villageone.app.service;

import com.villageone.app.dto.AppDto.*;
import com.villageone.app.entity.*;
import com.villageone.app.repository.*;
import com.villageone.app.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerService {

    private final WorkerSlotRepository    workerRepo;
    private final UserRepository          userRepo;
    private final InvoiceRepository       invoiceRepo;
    private final PlanRepository          planRepo;
    private final DeletedRecordRepository deletedRepo;
    private final AppConfigRepository     configRepo;
    private final PasswordEncoder         passwordEncoder;

    @Value("${worker.free-per-plan:1}") private int freePerPlan;

    // ── Add Worker ─────────────────────────────────────────
    public WorkerResponse addWorker(String ownerUsername, WorkerRequest req) {
        if (workerRepo.existsByWorkerUsername(req.getWorkerUsername()))
            throw new RuntimeException("Username '" + req.getWorkerUsername() + "' is already taken");
        if (userRepo.existsByUsername(req.getWorkerUsername()))
            throw new RuntimeException("Username '" + req.getWorkerUsername() + "' is already taken");
        if (userRepo.existsByMobile(req.getWorkerMobile()))
            throw new RuntimeException("Mobile number already registered");

        long currentCount = workerRepo.countByOwnerUsernameAndIsActiveTrue(ownerUsername);
        boolean isPaid    = currentCount >= freePerPlan;

        // Plan dates for worker
        String    planId    = isPaid ? (req.getPlanId() != null ? req.getPlanId() : "monthly") : "free";
        LocalDate planStart = LocalDate.now();
        LocalDate planEnd   = isPaid ? calculatePlanEnd(planId, planStart) : LocalDate.now().plusYears(10);
        String    planLabel = isPaid ? getPlanLabel(planId) : "Free";

        User workerUser = User.builder()
                .fullName(req.getWorkerName()).mobile(req.getWorkerMobile())
                .email(req.getWorkerUsername() + "@villageone.in")
                .address("N/A").pincode("000000")
                .storeName(ownerUsername).storeType("Worker")
                .storeAddress("N/A").city("N/A").state("N/A").storePincode("000000")
                .username(req.getWorkerUsername())
                .password(passwordEncoder.encode(req.getWorkerPassword()))
                .role(User.Role.WORKER)
                .planId(planId).planStart(planStart).planEnd(planEnd)
                .transactionId(req.getTransactionId() != null ? req.getTransactionId() : "free")
                .build();
        userRepo.save(workerUser);

        Long businessId = JwtAuthFilter.BUSINESS_ID.get();
        WorkerSlot slot = WorkerSlot.builder()
                .businessId(businessId).ownerUsername(ownerUsername)
                .workerUsername(req.getWorkerUsername()).workerName(req.getWorkerName())
                .workerMobile(req.getWorkerMobile())
                .canCreateInvoice(req.isCanCreateInvoice()).canEditInvoice(req.isCanEditInvoice())
                .canDeleteInvoice(req.isCanDeleteInvoice()).canViewReports(req.isCanViewReports())
                .canManageStock(req.isCanManageStock()).canViewProducts(req.isCanViewProducts())
                .canAddProduct(req.isCanAddProduct()).canEditProduct(req.isCanEditProduct())
                .canDeleteProduct(req.isCanDeleteProduct())
                .isPaidSlot(isPaid)
                .workerPlanId(planId).workerPlanStart(planStart)
                .workerPlanEnd(planEnd).workerPlanLabel(planLabel)
                .build();
        return toResponse(workerRepo.save(slot));
    }

    // ── Remove Worker with mandatory reason ────────────────
    public void removeWorker(Long slotId, String ownerUsername, String reason) {
        WorkerSlot slot = workerRepo.findById(slotId)
                .filter(s -> s.getOwnerUsername().equals(ownerUsername))
                .orElseThrow(() -> new RuntimeException("Worker not found"));

        if (reason == null || reason.trim().length() < 5)
            throw new RuntimeException("Reason is required (min 5 characters)");

        // Save deletion record
        deletedRepo.save(DeletedRecord.builder()
                .type(DeletedRecord.DeleteType.WORKER)
                .deletedEntityId(slot.getWorkerUsername())
                .deletedEntityName(slot.getWorkerName())
                .deletedBy(ownerUsername)
                .reason(reason.trim())
                .build());

        slot.setActive(false);
        workerRepo.save(slot);
        log.info("Worker removed: {} by {} reason: {}", slot.getWorkerUsername(), ownerUsername, reason);
    }

    // ── Upgrade worker plan ────────────────────────────────
    public WorkerResponse upgradeWorkerPlan(Long slotId, String ownerUsername,
                                            String planId, String txnId) {
        WorkerSlot slot = workerRepo.findById(slotId)
                .filter(s -> s.getOwnerUsername().equals(ownerUsername))
                .orElseThrow(() -> new RuntimeException("Worker not found"));

        // Stack on existing plan end if still active
        LocalDate base = (slot.getWorkerPlanEnd() != null && slot.getWorkerPlanEnd().isAfter(LocalDate.now()))
                ? slot.getWorkerPlanEnd() : LocalDate.now();
        LocalDate newEnd = calculatePlanEnd(planId, base);

        slot.setWorkerPlanId(planId);
        slot.setWorkerPlanStart(LocalDate.now());
        slot.setWorkerPlanEnd(newEnd);
        slot.setWorkerPlanLabel(getPlanLabel(planId));
        slot.setPaidSlot(true);
        workerRepo.save(slot);

        // Also update user record
        userRepo.findByUsername(slot.getWorkerUsername()).ifPresent(u -> {
            u.setPlanId(planId);
            u.setPlanStart(LocalDate.now());
            u.setPlanEnd(newEnd);
            u.setTransactionId(txnId);
            userRepo.save(u);
        });

        return toResponse(slot);
    }

    // ── Get workers ────────────────────────────────────────
    public List<WorkerResponse> getWorkers(String ownerUsername) {
        return workerRepo.findByOwnerUsernameAndIsActiveTrue(ownerUsername)
                .stream().map(this::toResponse).toList();
    }

    public WorkerResponse getWorkerDetail(Long id, String ownerUsername) {
        return toResponse(workerRepo.findById(id)
                .filter(s -> s.getOwnerUsername().equals(ownerUsername))
                .orElseThrow(() -> new RuntimeException("Worker not found")));
    }

    public WorkerResponse updatePermissions(Long id, String ownerUsername, WorkerRequest req) {
        WorkerSlot slot = workerRepo.findById(id)
                .filter(s -> s.getOwnerUsername().equals(ownerUsername))
                .orElseThrow(() -> new RuntimeException("Worker not found"));
        slot.setCanCreateInvoice(req.isCanCreateInvoice());
        slot.setCanEditInvoice(req.isCanEditInvoice());
        slot.setCanDeleteInvoice(req.isCanDeleteInvoice());
        slot.setCanViewReports(req.isCanViewReports());
        slot.setCanManageStock(req.isCanManageStock());
        slot.setCanViewProducts(req.isCanViewProducts());
        slot.setCanAddProduct(req.isCanAddProduct());
        slot.setCanEditProduct(req.isCanEditProduct());
        slot.setCanDeleteProduct(req.isCanDeleteProduct());
        return toResponse(workerRepo.save(slot));
    }

    public WorkerResponse getWorkerPermissions(String workerUsername) {
        return workerRepo.findByWorkerUsername(workerUsername)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Worker slot not found"));
    }

    public WorkerBillResponse getWorkerBill(String ownerUsername) {
        long total = workerRepo.countByOwnerUsernameAndIsActiveTrue(ownerUsername);
        long paid  = workerRepo.findByOwnerUsernameAndIsActiveTrue(ownerUsername)
                .stream().filter(WorkerSlot::isPaidSlot).count();
        int workerPrice = cfgInt("worker_price_monthly", 190); // from DB
        return WorkerBillResponse.builder()
                .totalWorkers((int) total).freeWorkers(freePerPlan)
                .paidWorkers((int) paid).pricePerWorker(workerPrice)
                .totalAmount(BigDecimal.valueOf(paid * workerPrice))
                .build();
    }

    // ── Get config from DB ─────────────────────────────────
    private String cfg(String key, String def) {
        return configRepo.findByKeyName(key).map(c -> c.getKeyValue()).orElse(def);
    }

    private int cfgInt(String key, int def) {
        try { return Integer.parseInt(cfg(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }

    // ── Helpers ────────────────────────────────────────────
    private LocalDate calculatePlanEnd(String planId, LocalDate from) {
        return switch (planId) {
            case "monthly"     -> from.plusMonths(1);
            case "half_yearly" -> from.plusMonths(6);
            case "yearly"      -> from.plusYears(1);
            default            -> from.plusMonths(1);
        };
    }

    private String getPlanLabel(String planId) {
        // Fetch label from DB, fallback to default
        return switch (planId) {
            case "monthly"     -> cfg("worker_plan_label_monthly",     "Monthly");
            case "half_yearly" -> cfg("worker_plan_label_half_yearly", "6 Months");
            case "yearly"      -> cfg("worker_plan_label_yearly",      "1 Year");
            case "free"        -> cfg("worker_plan_label_free",        "Free");
            default            -> "Free";
        };
    }

    public WorkerPlanPricing getWorkerPlanPricing() {
        return WorkerPlanPricing.builder()
                .monthly(cfgInt("worker_price_monthly", 190))
                .halfYearly(cfgInt("worker_price_half_yearly", 950))
                .yearly(cfgInt("worker_price_yearly", 1700))
                .labelMonthly(cfg("worker_plan_label_monthly", "Monthly"))
                .labelHalfYearly(cfg("worker_plan_label_half_yearly", "6 Months"))
                .labelYearly(cfg("worker_plan_label_yearly", "1 Year"))
                .freeSlots(cfgInt("worker_free_slots", 1))
                .build();
    }

    private WorkerResponse toResponse(WorkerSlot s) {
        int daysLeft = 0;
        boolean planActive = false;
        if (s.getWorkerPlanEnd() != null) {
            daysLeft   = (int) ChronoUnit.DAYS.between(LocalDate.now(), s.getWorkerPlanEnd());
            planActive = daysLeft >= 0;
        }
        return WorkerResponse.builder()
                .id(s.getId()).workerUsername(s.getWorkerUsername())
                .workerName(s.getWorkerName()).workerMobile(s.getWorkerMobile())
                .ownerUsername(s.getOwnerUsername())
                .canCreateInvoice(s.isCanCreateInvoice()).canEditInvoice(s.isCanEditInvoice())
                .canDeleteInvoice(s.isCanDeleteInvoice()).canViewReports(s.isCanViewReports())
                .canManageStock(s.isCanManageStock()).canViewProducts(s.isCanViewProducts())
                .canAddProduct(s.isCanAddProduct()).canEditProduct(s.isCanEditProduct())
                .canDeleteProduct(s.isCanDeleteProduct())
                .isPaidSlot(s.isPaidSlot()).isActive(s.isActive())
                .workerPlanId(s.getWorkerPlanId())
                .workerPlanLabel(s.getWorkerPlanLabel() != null ? s.getWorkerPlanLabel() : "Free")
                .workerPlanStart(s.getWorkerPlanStart() != null ? s.getWorkerPlanStart().toString() : "")
                .workerPlanEnd(s.getWorkerPlanEnd()   != null ? s.getWorkerPlanEnd().toString()   : "")
                .workerPlanDaysLeft(daysLeft).workerPlanActive(planActive)
                .build();
    }
}
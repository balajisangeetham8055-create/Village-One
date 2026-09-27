package com.villageone.app.controller;

import com.villageone.app.dto.AppDto.*;
import org.springframework.web.bind.annotation.RequestBody;
import com.villageone.app.service.WorkerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workers")
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerService workerService;

    @GetMapping
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<List<WorkerResponse>> getWorkers(Principal principal) {
        return ResponseEntity.ok(workerService.getWorkers(principal.getName()));
    }

    @PostMapping
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<?> addWorker(
            @Valid @RequestBody WorkerRequest req, Principal principal) {
        try {
            return ResponseEntity.ok(workerService.addWorker(principal.getName(), req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<WorkerResponse> getWorker(
            @PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(workerService.getWorkerDetail(id, principal.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<WorkerResponse> updateWorker(
            @PathVariable Long id,
            @RequestBody WorkerRequest req,
            Principal principal) {
        return ResponseEntity.ok(workerService.updatePermissions(id, principal.getName(), req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<?> removeWorker(
            @PathVariable Long id,
            @RequestBody(required = false) DeleteRequest req,
            Principal principal) {
        try {
            String reason = (req != null && req.getReason() != null) ? req.getReason() : "";
            workerService.removeWorker(id, principal.getName(), reason);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/upgrade-plan")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<?> upgradeWorkerPlan(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Principal principal) {
        try {
            return ResponseEntity.ok(workerService.upgradeWorkerPlan(
                    id, principal.getName(),
                    body.get("planId"),
                    body.get("razorpayPaymentId")
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/bill")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<WorkerBillResponse> getWorkerBill(Principal principal) {
        return ResponseEntity.ok(workerService.getWorkerBill(principal.getName()));
    }

    // GET /api/workers/plan-pricing — returns plan prices from DB (public)
    @GetMapping("/plan-pricing")
    public ResponseEntity<WorkerPlanPricing> getPlanPricing() {
        return ResponseEntity.ok(workerService.getWorkerPlanPricing());
    }
}
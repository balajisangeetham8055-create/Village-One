package com.villageone.app.controller;

import com.villageone.app.dto.AppDto.*;
import com.villageone.app.entity.WorkerSlot;
import com.villageone.app.repository.WorkerSlotRepository;
import com.villageone.app.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService       invoiceService;
    private final WorkerSlotRepository workerRepo;

    // Resolve the business owner username from the authenticated user
    private String resolveOwner(Authentication auth) {
        boolean isWorker = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_WORKER"));
        if (isWorker) {
            return workerRepo.findByWorkerUsername(auth.getName())
                    .map(WorkerSlot::getOwnerUsername)
                    .orElse(auth.getName());
        }
        return auth.getName();
    }

    // ── POST /api/invoices ────────────────────────────────
    @PostMapping
    public ResponseEntity<InvoiceResponse> create(
            @Valid @RequestBody InvoiceRequest req, Authentication auth) {
        return ResponseEntity.ok(
                invoiceService.createInvoice(resolveOwner(auth), auth.getName(), req));
    }

    // ── GET /api/invoices ─────────────────────────────────
    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> list(Authentication auth) {
        return ResponseEntity.ok(invoiceService.getInvoices(resolveOwner(auth)));
    }

    // ── GET /api/invoices/{invoiceNumber} ─────────────────
    // Now scoped to the owner — Business A's INV-0001 ≠ Business B's INV-0001
    @GetMapping("/{invoiceNumber}")
    public ResponseEntity<InvoiceResponse> get(
            @PathVariable String invoiceNumber, Authentication auth) {
        return ResponseEntity.ok(
                invoiceService.getInvoice(invoiceNumber, resolveOwner(auth)));
    }

    // ── PUT /api/invoices/{invoiceNumber} ─────────────────
    @PutMapping("/{invoiceNumber}")
    public ResponseEntity<InvoiceResponse> update(
            @PathVariable String invoiceNumber,
            @RequestBody InvoiceUpdateRequest req,
            Authentication auth) {

        boolean isWorker = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_WORKER"));
        if (isWorker) {
            WorkerSlot slot = workerRepo.findByWorkerUsername(auth.getName()).orElse(null);
            if (slot == null || !slot.isCanEditInvoice()) {
                log.warn("Worker {} denied invoice edit", auth.getName());
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(
                invoiceService.updateInvoice(invoiceNumber, resolveOwner(auth), req));
    }

    // ── DELETE /api/invoices/{invoiceNumber} ──────────────
    @DeleteMapping("/{invoiceNumber}")
    public ResponseEntity<Void> delete(
            @PathVariable String invoiceNumber, Authentication auth) {

        boolean isWorker = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_WORKER"));
        if (isWorker) {
            WorkerSlot slot = workerRepo.findByWorkerUsername(auth.getName()).orElse(null);
            if (slot == null || !slot.isCanDeleteInvoice()) {
                log.warn("Worker {} denied invoice delete", auth.getName());
                return ResponseEntity.status(403).build();
            }
        }
        invoiceService.deleteInvoice(invoiceNumber, resolveOwner(auth));
        return ResponseEntity.ok().build();
    }
}
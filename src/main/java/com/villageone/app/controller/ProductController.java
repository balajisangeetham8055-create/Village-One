package com.villageone.app.controller;

import com.villageone.app.dto.AppDto.*;
import com.villageone.app.entity.WorkerSlot;
import com.villageone.app.repository.WorkerSlotRepository;
import com.villageone.app.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService       productService;
    private final WorkerSlotRepository workerRepo;

    // ── Resolve owner username ─────────────────────────────
    private String resolveOwner(Authentication auth) {
        if (isWorker(auth)) {
            return workerRepo.findByWorkerUsername(auth.getName())
                    .map(WorkerSlot::getOwnerUsername)
                    .orElse(auth.getName());
        }
        return auth.getName();
    }

    // ── Check if current user is a worker ──────────────────
    private boolean isWorker(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_WORKER"));
    }

    // ── Get worker slot (null if not a worker) ─────────────
    private WorkerSlot getWorkerSlot(Authentication auth) {
        if (!isWorker(auth)) return null;
        return workerRepo.findByWorkerUsername(auth.getName()).orElse(null);
    }

    // ── GET /api/products ─────────────────────────────────
    // Workers with canViewProducts can view products
    @GetMapping
    public ResponseEntity<?> getProducts(Authentication auth) {
        if (isWorker(auth)) {
            WorkerSlot slot = getWorkerSlot(auth);
            if (slot == null || !slot.isCanViewProducts()) {
                log.warn("Worker {} denied: canViewProducts=false", auth.getName());
                return ResponseEntity.status(403).body(Map.of("message", "You don't have permission to view products"));
            }
        }
        return ResponseEntity.ok(productService.getProducts(resolveOwner(auth)));
    }

    // ── POST /api/products ────────────────────────────────
    // Business owners always allowed; workers need canAddProduct
    @PostMapping
    public ResponseEntity<?> addProduct(
            @Valid @RequestBody ProductRequest req, Authentication auth) {
        if (isWorker(auth)) {
            WorkerSlot slot = getWorkerSlot(auth);
            if (slot == null || !slot.isCanAddProduct()) {
                log.warn("Worker {} denied: canAddProduct=false", auth.getName());
                return ResponseEntity.status(403).body(Map.of("message", "You don't have permission to add products"));
            }
            // Workers add products under their owner's account
            return ResponseEntity.ok(productService.addProduct(resolveOwner(auth), req));
        }
        return ResponseEntity.ok(productService.addProduct(auth.getName(), req));
    }

    // ── PUT /api/products/{id} ────────────────────────────
    // Business owners always allowed; workers need canEditProduct
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest req,
            Authentication auth) {
        if (isWorker(auth)) {
            WorkerSlot slot = getWorkerSlot(auth);
            if (slot == null || !slot.isCanEditProduct()) {
                log.warn("Worker {} denied: canEditProduct=false", auth.getName());
                return ResponseEntity.status(403).body(Map.of("message", "You don't have permission to edit products"));
            }
            return ResponseEntity.ok(productService.updateProduct(id, resolveOwner(auth), req));
        }
        return ResponseEntity.ok(productService.updateProduct(id, auth.getName(), req));
    }

    // ── POST /api/products/stock/add ──────────────────────
    // Workers with canManageStock or canEditProduct can add stock
    @PostMapping("/stock/add")
    public ResponseEntity<?> addStock(
            @RequestBody StockUpdateRequest req, Authentication auth) {
        if (isWorker(auth)) {
            WorkerSlot slot = getWorkerSlot(auth);
            if (slot == null || (!slot.isCanEditProduct() && !slot.isCanManageStock())) {
                log.warn("Worker {} denied: no stock permission", auth.getName());
                return ResponseEntity.status(403).body(Map.of("message", "You don't have permission to update stock"));
            }
        }
        return ResponseEntity.ok(productService.addStock(resolveOwner(auth), req));
    }

    // ── DELETE /api/products/{id} ─────────────────────────
    // Business owners always allowed; workers need canDeleteProduct
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(
            @PathVariable Long id, Authentication auth) {
        if (isWorker(auth)) {
            WorkerSlot slot = getWorkerSlot(auth);
            if (slot == null || !slot.isCanDeleteProduct()) {
                log.warn("Worker {} denied: canDeleteProduct=false", auth.getName());
                return ResponseEntity.status(403).body(Map.of("message", "You don't have permission to delete products"));
            }
            productService.deleteProduct(id, resolveOwner(auth));
            return ResponseEntity.ok().build();
        }
        productService.deleteProduct(id, auth.getName());
        return ResponseEntity.ok().build();
    }
}
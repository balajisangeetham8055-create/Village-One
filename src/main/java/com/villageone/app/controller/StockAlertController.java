package com.villageone.app.controller;

import com.villageone.app.entity.StockAlert;
import com.villageone.app.entity.WorkerSlot;
import com.villageone.app.repository.ProductRepository;
import com.villageone.app.repository.StockAlertRepository;
import com.villageone.app.repository.WorkerSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class StockAlertController {

    private final StockAlertRepository alertRepo;
    private final WorkerSlotRepository  workerRepo;
    private final ProductRepository      productRepo;

    @GetMapping
    public ResponseEntity<List<StockAlert>> getAlerts(Authentication auth) {
        return ResponseEntity.ok(alertRepo.findByOwnerUsernameAndIsReadFalse(resolveOwner(auth)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        alertRepo.findById(id).ifPresent(a -> {
            a.setRead(true);
            alertRepo.save(a);
        });
        return ResponseEntity.ok().build();
    }

    // POST /api/alerts/refresh — re-scans all products and creates new alerts for low stock
    @PostMapping("/refresh")
    public ResponseEntity<List<StockAlert>> refreshAlerts(Authentication auth) {
        String owner = resolveOwner(auth);
        // Mark all existing alerts as read first
        alertRepo.findByOwnerUsernameAndIsReadFalse(owner)
                .forEach(a -> { a.setRead(true); alertRepo.save(a); });
        // Re-scan products and create fresh alerts for low stock
        productRepo.findByOwnerUsername(owner).stream()
                .filter(p -> p.getStockQuantity() <= p.getLowStockThreshold())
                .forEach(p -> {
                    boolean exists = alertRepo.existsByOwnerUsernameAndProductIdAndIsReadFalse(owner, p.getId());
                    if (!exists) {
                        alertRepo.save(StockAlert.builder()
                                .businessId(p.getBusinessId())
                                .ownerUsername(owner)
                                .productId(p.getId())
                                .productName(p.getName() + " (" + p.getUnit() + ")")
                                .currentStock(p.getStockQuantity().intValue())
                                .threshold(p.getLowStockThreshold().intValue())
                                .build());
                    }
                });
        return ResponseEntity.ok(alertRepo.findByOwnerUsernameAndIsReadFalse(owner));
    }

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
}
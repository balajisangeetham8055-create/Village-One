package com.villageone.app.controller;

import com.villageone.app.dto.AppDto.*;
import com.villageone.app.entity.WorkerSlot;
import com.villageone.app.repository.WorkerSlotRepository;
import com.villageone.app.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService        reportService;
    private final WorkerSlotRepository workerRepo;

    @PostMapping("/sales")
    public ResponseEntity<SalesReportResponse> getSalesReport(
            @RequestBody ReportRequest req, Authentication auth) {
        String owner = auth.getName();
        boolean isWorker = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_WORKER"));
        if (isWorker) {
            owner = workerRepo.findByWorkerUsername(auth.getName())
                    .map(WorkerSlot::getOwnerUsername)
                    .orElse(auth.getName());
        }
        return ResponseEntity.ok(reportService.getSalesReport(owner, req));
    }
}
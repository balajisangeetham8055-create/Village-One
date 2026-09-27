package com.villageone.app.controller;

import com.villageone.app.dto.AppDto.*;
import com.villageone.app.service.DashboardService;
import com.villageone.app.service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final WorkerService    workerService;

    @GetMapping("/business")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<DashboardResponse> businessDashboard(Principal principal) {
        return ResponseEntity.ok(dashboardService.getDashboard(principal.getName()));
    }

    @GetMapping("/worker")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<WorkerResponse> workerDashboard(Principal principal) {
        return ResponseEntity.ok(workerService.getWorkerPermissions(principal.getName()));
    }
}
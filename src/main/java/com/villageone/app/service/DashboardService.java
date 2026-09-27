package com.villageone.app.service;

import com.villageone.app.dto.AppDto.*;
import com.villageone.app.entity.*;
import com.villageone.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final InvoiceRepository    invoiceRepo;
    private final ProductRepository    productRepo;
    private final WorkerSlotRepository workerRepo;
    private final StockAlertRepository alertRepo;
    private final UserRepository       userRepo;

    public DashboardResponse getDashboard(String ownerUsername) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end   = LocalDate.now().atTime(23, 59, 59);
        var today = invoiceRepo.findByOwnerAndDateRange(ownerUsername, start, end);
        BigDecimal todaySales = today.stream()
                .filter(i -> i.getStatus() != Invoice.InvoiceStatus.CANCELLED)
                .map(Invoice::getGrandTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalProducts = productRepo.findByOwnerUsername(ownerUsername).size();
        long lowStock      = productRepo.findByOwnerUsername(ownerUsername).stream()
                .filter(p -> p.getStockQuantity() <= p.getLowStockThreshold()).count();
        long totalWorkers  = workerRepo.countByOwnerUsernameAndIsActiveTrue(ownerUsername);
        long alerts        = alertRepo.findByOwnerUsernameAndIsReadFalse(ownerUsername).size();
        User user = userRepo.findByUsername(ownerUsername).orElse(null);

        String planId     = user != null ? user.getPlanId()   : "";
        String planStart  = user != null && user.getPlanStart() != null ? user.getPlanStart().toString() : "";
        String planExpiry = user != null && user.getPlanEnd()   != null ? user.getPlanEnd().toString()   : "";
        int daysLeft = 0;
        boolean planActive = false;
        if (user != null && user.getPlanEnd() != null) {
            daysLeft   = (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), user.getPlanEnd());
            planActive = daysLeft >= 0;
        }
        // Map planId to label
        String planLabel = switch (planId != null ? planId : "") {
            case "monthly"     -> "Monthly";
            case "half_yearly" -> "6 Months";
            case "yearly"      -> "1 Year";
            case "worker"      -> "Worker";
            default            -> "No active plan";
        };

        return DashboardResponse.builder()
                .todaySales(todaySales).todayInvoices(today.size())
                .totalProducts((int) totalProducts).lowStockCount((int) lowStock)
                .totalWorkers((int) totalWorkers).unreadAlerts((int) alerts)
                .storeName(user != null ? user.getStoreName() : "")
                .ownerName(user != null ? user.getFullName()  : "")
                .planId(planId).planLabel(planLabel)
                .planStart(planStart).planExpiry(planExpiry)
                .planDaysLeft(daysLeft).planActive(planActive)
                .build();
    }
}
package com.villageone.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "worker_slots")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkerSlot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long    businessId;
    @Column(nullable = false) private String  ownerUsername;
    @Column(nullable = false, unique = true) private String workerUsername;
    @Column(nullable = false) private String  workerName;
    @Column(nullable = false) private String  workerMobile;

    @Column(nullable = false) private boolean canCreateInvoice;
    @Column(nullable = false) private boolean canEditInvoice;
    @Column(nullable = false) private boolean canDeleteInvoice;
    @Column(nullable = false) private boolean canViewReports;
    @Column(nullable = false) private boolean canManageStock;
    @Column(nullable = false) private boolean canViewProducts;
    @Column(nullable = false) private boolean canAddProduct;      // add new products
    @Column(nullable = false) private boolean canEditProduct;     // edit products
    @Column(nullable = false) private boolean canDeleteProduct;   // delete products
    @Column(nullable = false) private boolean isPaidSlot;
    @Column(nullable = false) private boolean isActive;

    // Plan tracking for worker
    @Column private String    workerPlanId;     // free / monthly / half_yearly / yearly
    @Column private LocalDate workerPlanStart;
    @Column private LocalDate workerPlanEnd;
    @Column private String    workerPlanLabel;  // Free / Monthly / 6 Months / 1 Year

    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column private LocalDateTime updatedAt;

    @PrePersist public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        isActive  = true;
    }
    @PreUpdate public void preUpdate() { updatedAt = LocalDateTime.now(); }
}
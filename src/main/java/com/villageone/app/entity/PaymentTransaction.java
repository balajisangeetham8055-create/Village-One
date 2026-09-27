package com.villageone.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Stores every payment done through Razorpay.
 * Types: PLAN_PURCHASE, PLAN_UPGRADE, WORKER_SLOT
 */
@Entity @Table(name = "payment_transactions")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentTransaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;          // business owner username

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentType type;         // PLAN_PURCHASE / PLAN_UPGRADE / WORKER_SLOT

    @Column(nullable = false)
    private String razorpayPaymentId; // razorpay_payment_id from frontend

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;        // amount paid in rupees

    @Column
    private String planId;            // which plan was purchased

    @Column
    private String workerUsername;    // for WORKER_SLOT type

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;     // SUCCESS / FAILED

    @Column(length = 500)
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() { createdAt = LocalDateTime.now(); }

    public enum PaymentType   { PLAN_PURCHASE, PLAN_UPGRADE, WORKER_SLOT }
    public enum PaymentStatus { SUCCESS, FAILED }
}
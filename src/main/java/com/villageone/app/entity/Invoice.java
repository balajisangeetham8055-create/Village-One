package com.villageone.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        name = "invoices",
        uniqueConstraints = {
                // invoice_number is unique PER BUSINESS OWNER, not globally
                // Business A and Business B can both have INV-0001
                @UniqueConstraint(
                        name = "UK_invoice_number_per_owner",
                        columnNames = {"invoice_number", "owner_username"}
                )
        }
)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Invoice {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)           private String invoiceNumber;
    @Column(nullable = false)           private Long   businessId;
    @Column(nullable = false)           private String ownerUsername;
    @Column(nullable = false)           private String createdByUsername;
    @Column(nullable = false)           private String customerName;
    @Column                             private String customerMobile;
    @Column                             private String customerAddress;

    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal subTotal;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal totalGst;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal grandTotal;

    @Column(nullable = false) @Enumerated(EnumType.STRING) private PaymentMode  paymentMode;
    @Column(nullable = false) @Enumerated(EnumType.STRING) private InvoiceStatus status;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InvoiceItem> items;

    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column                                      private LocalDateTime updatedAt;

    @PrePersist public void prePersist() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate  public void preUpdate()  { updatedAt = LocalDateTime.now(); }

    public enum PaymentMode   { CASH, UPI, CARD, CREDIT, BORROW }
    public enum InvoiceStatus { PAID, UNPAID, CANCELLED }
}
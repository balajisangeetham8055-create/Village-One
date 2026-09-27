package com.villageone.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "invoice_items")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column                   private Long       productId;
    @Column(nullable = false) private String     productName;
    @Column(nullable = false) private String     unit;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal quantity;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal unitPrice;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal purchasePricePerUnit;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal gstPercent;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal gstAmount;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal totalPrice;
    @Column                   private String     warranty;
}
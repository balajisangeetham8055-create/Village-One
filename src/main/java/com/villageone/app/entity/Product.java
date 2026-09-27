package com.villageone.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "products")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long      businessId;
    @Column(nullable = false) private String    ownerUsername;
    @Column(nullable = false) private String    name;
    @Column                   private String    shortCode;
    @Column                   private String    category;
    @Column                   private String    unit;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal purchasePrice;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal sellingPrice;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal gstPercent;
    @Column(nullable = false) private Double    stockQuantity;     // Double supports 0.5kg, 0.25kg
    @Column(nullable = false) private Double    lowStockThreshold;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column                   private LocalDateTime updatedAt;

    @PrePersist public void prePersist() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate  public void preUpdate()  { updatedAt = LocalDateTime.now(); }
}
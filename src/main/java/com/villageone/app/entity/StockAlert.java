package com.villageone.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "stock_alerts")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class StockAlert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long    businessId;
    @Column(nullable = false) private String  ownerUsername;
    @Column(nullable = false) private Long    productId;
    @Column(nullable = false) private String  productName;
    @Column(nullable = false) private Integer currentStock;
    @Column(nullable = false) private Integer threshold;
    @Column(nullable = false) private boolean isRead;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;

    @PrePersist public void prePersist() { createdAt = LocalDateTime.now(); isRead = false; }
}
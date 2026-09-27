package com.villageone.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "plans")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Plan {
    @Id                       private String     id;
    @Column(nullable = false) private String     label;
    @Column(nullable = false) private BigDecimal price;
    @Column(nullable = false) private String     priceLabel;
    @Column(nullable = false) private Integer    days;
    @Column(nullable = false) private String     per;
    @Column                   private String     badge;
    @Column(length = 1000)    private String     features;
    @Column(nullable = false) private boolean    active;
}
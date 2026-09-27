package com.villageone.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "deleted_records")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DeletedRecord {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DeleteType type;  // WORKER, BUSINESS_USER

    @Column(nullable = false) private String deletedEntityId;   // username or ID
    @Column(nullable = false) private String deletedEntityName; // full name
    @Column(nullable = false) private String deletedBy;         // who deleted
    @Column(nullable = false, length = 1000) private String reason;
    @Column(nullable = false, updatable = false) private LocalDateTime deletedAt;

    @PrePersist public void prePersist() { deletedAt = LocalDateTime.now(); }

    public enum DeleteType { WORKER, BUSINESS_USER }
}
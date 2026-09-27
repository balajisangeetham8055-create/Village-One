package com.villageone.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "support_tickets")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportTicket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true) private String ticketId;
    @Column(nullable = false) private String issueType;
    @Column(nullable = false) private String priority;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String mobile;
    @Column                   private String email;
    @Column(nullable = false) private String subject;
    @Column(length = 2000)    private String description;
    @Column(nullable = false) private String status;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;

    @PrePersist public void prePersist() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "OPEN";
    }
}
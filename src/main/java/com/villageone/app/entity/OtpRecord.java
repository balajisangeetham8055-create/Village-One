package com.villageone.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "otp_records")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OtpRecord {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Stores email or mobile — length 255 to support both
    @Column(nullable = false, length = 255)
    private String mobile;

    @Column(nullable = false, length = 6)
    private String otp;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OtpType type;

    @Builder.Default
    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        expiresAt = LocalDateTime.now().plusMinutes(10);
    }

    public enum OtpType { REGISTER, FORGOT_PASSWORD }
}
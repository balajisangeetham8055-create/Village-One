package com.villageone.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "users")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)                        private String    fullName;
    @Column(nullable = false, unique = true, length = 10) private String mobile;
    @Column(nullable = false, unique = true)         private String    email;
    @Column(nullable = false)                        private String    address;
    @Column(nullable = false, length = 6)            private String    pincode;
    @Column(nullable = false)                        private String    storeName;
    @Column(nullable = false)                        private String    storeType;
    @Column(nullable = false)                        private String    storeAddress;
    @Column(nullable = false)                        private String    city;
    @Column(nullable = false)                        private String    state;
    @Column(nullable = false, length = 6)            private String    storePincode;
    @Column                                          private String    gstNumber;
    @Column(nullable = false, unique = true)         private String    username;
    @Column(nullable = false)                        private String    password;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column private String    planId;
    @Column private LocalDate planStart;
    @Column private LocalDate planEnd;
    @Column private String    transactionId;
    @Column(columnDefinition = "LONGTEXT") private String profilePhoto; // base64 image

    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column                                      private LocalDateTime updatedAt;

    @PrePersist public void prePersist() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate  public void preUpdate()  { updatedAt = LocalDateTime.now(); }

    public enum Role { BUSINESS, WORKER }
}
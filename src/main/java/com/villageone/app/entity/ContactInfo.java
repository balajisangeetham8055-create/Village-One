package com.villageone.app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "contact_info")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ContactInfo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column private String whatsapp;
    @Column private String whatsappLabel;
    @Column private String phone;
    @Column private String phoneLabel;
    @Column private String email;
    @Column private String hoursChat;
    @Column private String hoursCall;
    @Column private String replyTime;
}
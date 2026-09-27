package com.villageone.app.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Stores app-level config like Razorpay keys.
 * key_name → key_value pairs in DB.
 * Admin sets these once via MySQL Workbench.
 *
 * INSERT INTO app_config (key_name, key_value, description) VALUES
 *   ('razorpay_key_id',     'rzp_live_xxx', 'Razorpay Live Key ID'),
 *   ('razorpay_key_secret', 'xxx',          'Razorpay Live Key Secret'),
 *   ('worker_price',        '30',           'Price per extra worker per month');
 */
@Entity @Table(name = "app_config")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AppConfig {

    @Id
    @Column(name = "key_name", nullable = false, unique = true)
    private String keyName;

    @Column(name = "key_value", nullable = false, length = 500)
    private String keyValue;

    @Column(length = 255)
    private String description;
}
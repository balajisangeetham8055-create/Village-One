package com.villageone.app.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Maintains per-business invoice sequence.
 * Each business owner gets their own counter: INV-0001, INV-0002...
 * Two businesses can both have INV-0001 — they are isolated by ownerUsername.
 *
 * The @Version field provides optimistic locking as a safety net,
 * but the primary concurrency control is the PESSIMISTIC_WRITE lock
 * in BusinessInvoiceSequenceRepository.findByOwnerUsernameForUpdate()
 */
@Entity
@Table(name = "business_invoice_sequence")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessInvoiceSequence {

    @Id
    @Column(name = "owner_username", nullable = false, unique = true)
    private String ownerUsername;

    @Builder.Default
    @Column(name = "last_sequence", nullable = false)
    private Long lastSequence = 0L;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;  // ← must default to 0L, not null
}
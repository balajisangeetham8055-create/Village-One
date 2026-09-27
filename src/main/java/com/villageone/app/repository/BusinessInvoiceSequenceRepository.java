package com.villageone.app.repository;

import com.villageone.app.entity.BusinessInvoiceSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface BusinessInvoiceSequenceRepository
        extends JpaRepository<BusinessInvoiceSequence, String> {

    /**
     * Pessimistic write lock — prevents two requests from getting the same
     * sequence number. Called AFTER ensureExists() guarantees the row exists.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM BusinessInvoiceSequence s WHERE s.ownerUsername = :owner")
    Optional<BusinessInvoiceSequence> findByOwnerUsernameForUpdate(
            @Param("owner") String ownerUsername);

    /**
     * INSERT ... ON DUPLICATE KEY UPDATE — safely creates a sequence row
     * for a new business owner using MySQL's upsert syntax.
     *
     * ON DUPLICATE KEY UPDATE last_sequence = last_sequence means:
     * - If row doesn't exist: INSERT with sequence=0, version=0
     * - If row already exists: no-op (nothing changes)
     *
     * Safe for concurrent calls. Works correctly with Hibernate's @Version
     * because we never touch version here — Hibernate handles that in saveAndFlush().
     */
    @Modifying
    @Transactional
    @Query(value =
            "INSERT INTO business_invoice_sequence (owner_username, last_sequence, version) " +
                    "VALUES (:owner, 0, 0) " +
                    "ON DUPLICATE KEY UPDATE last_sequence = last_sequence",
            nativeQuery = true)
    void ensureExists(@Param("owner") String ownerUsername);
}
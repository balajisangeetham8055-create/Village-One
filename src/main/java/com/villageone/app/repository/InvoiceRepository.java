package com.villageone.app.repository;

import com.villageone.app.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByOwnerUsernameOrderByCreatedAtDesc(String ownerUsername);

    // ── Safe: fetch by invoiceNumber + ownerUsername (avoids cross-business collision) ──
    Optional<Invoice> findByInvoiceNumberAndOwnerUsername(String invoiceNumber, String ownerUsername);

    // ── Fetch by ID (most reliable — ID is globally unique) ──
    // Already provided by JpaRepository.findById(Long id)

    long countByOwnerUsername(String ownerUsername);

    @Query("SELECT i FROM Invoice i WHERE i.ownerUsername = :owner AND i.createdAt BETWEEN :from AND :to")
    List<Invoice> findByOwnerAndDateRange(
            @Param("owner") String owner,
            @Param("from")  LocalDateTime from,
            @Param("to")    LocalDateTime to);
}
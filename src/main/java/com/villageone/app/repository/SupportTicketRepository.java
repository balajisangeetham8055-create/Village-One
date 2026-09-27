package com.villageone.app.repository;
import com.villageone.app.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    boolean existsByTicketId(String ticketId);
    Optional<SupportTicket> findByTicketId(String ticketId);
}
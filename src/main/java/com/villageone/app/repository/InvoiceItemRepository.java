package com.villageone.app.repository;
import com.villageone.app.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {
    List<InvoiceItem> findByInvoiceId(Long invoiceId);
    void deleteByInvoiceId(Long invoiceId);
}
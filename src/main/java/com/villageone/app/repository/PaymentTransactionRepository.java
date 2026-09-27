package com.villageone.app.repository;
import com.villageone.app.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    List<PaymentTransaction> findByUsernameOrderByCreatedAtDesc(String username);
}
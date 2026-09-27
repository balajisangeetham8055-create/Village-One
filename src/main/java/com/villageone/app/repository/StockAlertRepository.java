package com.villageone.app.repository;
import com.villageone.app.entity.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {
    List<StockAlert> findByOwnerUsernameAndIsReadFalse(String ownerUsername);
    boolean existsByOwnerUsernameAndProductIdAndIsReadFalse(String owner, Long productId);
    java.util.Optional<com.villageone.app.entity.StockAlert> findByOwnerUsernameAndProductIdAndIsReadFalse(String ownerUsername, Long productId);
}
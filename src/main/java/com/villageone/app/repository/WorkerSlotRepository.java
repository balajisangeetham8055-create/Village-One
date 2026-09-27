package com.villageone.app.repository;
import com.villageone.app.entity.WorkerSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface WorkerSlotRepository extends JpaRepository<WorkerSlot, Long> {
    List<WorkerSlot> findByOwnerUsernameAndIsActiveTrue(String ownerUsername);
    Optional<WorkerSlot> findByWorkerUsername(String workerUsername);
    long countByOwnerUsernameAndIsActiveTrue(String ownerUsername);
    boolean existsByWorkerUsername(String workerUsername);
}
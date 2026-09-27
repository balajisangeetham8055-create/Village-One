package com.villageone.app.repository;
import com.villageone.app.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PlanRepository extends JpaRepository<Plan, String> {
    List<Plan> findByActiveTrue();
}
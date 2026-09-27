package com.villageone.app.repository;
import com.villageone.app.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByOwnerUsername(String ownerUsername);
    boolean existsByOwnerUsernameAndName(String owner, String name);
}
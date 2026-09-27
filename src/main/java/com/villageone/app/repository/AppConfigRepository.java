package com.villageone.app.repository;
import com.villageone.app.entity.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface AppConfigRepository extends JpaRepository<AppConfig, String> {
    Optional<AppConfig> findByKeyName(String keyName);
}
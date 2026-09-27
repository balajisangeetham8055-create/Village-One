package com.villageone.app.repository;
import com.villageone.app.entity.ContactInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ContactInfoRepository extends JpaRepository<ContactInfo, Long> {
    Optional<ContactInfo> findFirstBy();
}
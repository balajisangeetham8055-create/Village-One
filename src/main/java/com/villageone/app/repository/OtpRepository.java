package com.villageone.app.repository;

import com.villageone.app.entity.OtpRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpRecord, Long> {

    // Get latest unverified OTP for mobile + type
    Optional<OtpRecord> findTopByMobileAndTypeAndVerifiedFalseOrderByCreatedAtDesc(
            String mobile, OtpRecord.OtpType type
    );

    // Delete old OTPs for same mobile + type before creating new one
    @Modifying
    @Transactional
    @Query("DELETE FROM OtpRecord o WHERE o.mobile = :mobile AND o.type = :type")
    void deleteByMobileAndType(String mobile, OtpRecord.OtpType type);

    // Cleanup expired OTPs
    @Modifying
    @Transactional
    @Query("DELETE FROM OtpRecord o WHERE o.expiresAt < :now")
    void deleteExpiredOtps(LocalDateTime now);
}
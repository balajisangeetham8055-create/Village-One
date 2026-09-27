package com.villageone.app.repository;
import com.villageone.app.entity.DeletedRecord;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DeletedRecordRepository extends JpaRepository<DeletedRecord, Long> {}
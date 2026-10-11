package com.adsentry.importing.repository;

import com.adsentry.importing.domain.ImportedRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ImportedRecordRepository extends JpaRepository<ImportedRecord, Long> {
    List<ImportedRecord> findByBatchId(Long batchId);
    List<ImportedRecord> findByIdentityKey(String identityKey);
    List<ImportedRecord> findByPlatformAndReportingDateBetween(String platform, LocalDate start, LocalDate end);
    long deleteByBatchId(Long batchId);
}

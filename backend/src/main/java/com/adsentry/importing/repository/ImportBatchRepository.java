package com.adsentry.importing.repository;

import com.adsentry.importing.domain.BatchStatus;
import com.adsentry.importing.domain.ImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {
    List<ImportBatch> findAllByOrderByImportTimestampDesc();
    List<ImportBatch> findByStatus(BatchStatus status);
}

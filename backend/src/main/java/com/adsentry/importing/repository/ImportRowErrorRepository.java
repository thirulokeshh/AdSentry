package com.adsentry.importing.repository;

import com.adsentry.importing.domain.ImportRowError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportRowErrorRepository extends JpaRepository<ImportRowError, Long> {
    List<ImportRowError> findByBatchIdOrderBySourceRowNumberAsc(Long batchId);
    org.springframework.data.domain.Page<ImportRowError> findByBatchId(Long batchId, org.springframework.data.domain.Pageable pageable);
    long countByBatchId(Long batchId);
    long deleteByBatchId(Long batchId);
}

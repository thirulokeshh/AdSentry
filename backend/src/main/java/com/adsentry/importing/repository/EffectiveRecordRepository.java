package com.adsentry.importing.repository;

import com.adsentry.importing.domain.EffectiveRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EffectiveRecordRepository extends JpaRepository<EffectiveRecord, Long> {
    List<EffectiveRecord> findByReportingDateBetween(LocalDate startDate, LocalDate endDate);
    List<EffectiveRecord> findByPlatformAndReportingDateBetween(String platform, LocalDate startDate, LocalDate endDate);
    Optional<EffectiveRecord> findByIdentityKey(String identityKey);
    List<EffectiveRecord> findByCampaignIdentityKey(String campaignIdentityKey);
    List<EffectiveRecord> findByCampaignIdentityKeyAndReportingDateBetween(String campaignIdentityKey, LocalDate startDate, LocalDate endDate);
    List<EffectiveRecord> findByBatchId(Long batchId);
}

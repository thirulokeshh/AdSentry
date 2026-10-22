package com.adsentry.importing.repository;

import com.adsentry.importing.domain.EffectiveRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("SELECT MIN(r.reportingDate) FROM EffectiveRecord r")
    LocalDate findMinReportingDate();

    @Query("SELECT MAX(r.reportingDate) FROM EffectiveRecord r")
    LocalDate findMaxReportingDate();

    @Query("SELECT MIN(r.reportingDate) FROM EffectiveRecord r WHERE r.platform = :platform")
    LocalDate findMinReportingDateByPlatform(@Param("platform") String platform);

    @Query("SELECT MAX(r.reportingDate) FROM EffectiveRecord r WHERE r.platform = :platform")
    LocalDate findMaxReportingDateByPlatform(@Param("platform") String platform);

    @Query("SELECT MIN(r.reportingDate) FROM EffectiveRecord r WHERE r.platform IN :platforms")
    LocalDate findMinReportingDateByPlatformIn(@Param("platforms") List<String> platforms);

    @Query("SELECT MAX(r.reportingDate) FROM EffectiveRecord r WHERE r.platform IN :platforms")
    LocalDate findMaxReportingDateByPlatformIn(@Param("platforms") List<String> platforms);

    @Query("SELECT r FROM EffectiveRecord r WHERE r.platform IN :platforms AND r.reportingDate BETWEEN :startDate AND :endDate")
    List<EffectiveRecord> findByPlatformInAndReportingDateBetween(@Param("platforms") List<String> platforms, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}

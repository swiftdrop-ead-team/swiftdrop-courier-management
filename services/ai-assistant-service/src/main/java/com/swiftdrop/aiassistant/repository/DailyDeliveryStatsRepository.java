package com.swiftdrop.aiassistant.repository;

import com.swiftdrop.aiassistant.entity.DailyDeliveryStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyDeliveryStatsRepository extends JpaRepository<DailyDeliveryStats, Long> {
    Optional<DailyDeliveryStats> findByReportDate(LocalDate reportDate);
}
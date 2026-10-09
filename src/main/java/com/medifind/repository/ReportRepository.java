package com.medifind.repository;

import com.medifind.entity.AvailabilityReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<AvailabilityReport, Long> {
    List<AvailabilityReport> findByUserId(Long userId);
}

package com.medifind.repository;

import com.medifind.entity.PharmacyHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface PharmacyHoursRepository extends JpaRepository<PharmacyHours, Long> {

    List<PharmacyHours> findByPharmacyId(Long pharmacyId);

    Optional<PharmacyHours> findByPharmacyIdAndDayOfWeek(Long pharmacyId, DayOfWeek dayOfWeek);

    void deleteByPharmacyId(Long pharmacyId);
}

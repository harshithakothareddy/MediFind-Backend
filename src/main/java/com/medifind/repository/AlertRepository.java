package com.medifind.repository;

import com.medifind.entity.AvailabilityAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<AvailabilityAlert, Long> {
    List<AvailabilityAlert> findByUserId(Long userId);

    List<AvailabilityAlert> findByMedicineIdAndActiveTrue(Long medicineId);

    Optional<AvailabilityAlert> findByUserIdAndMedicineIdAndActiveTrue(Long userId, Long medicineId);

    Optional<AvailabilityAlert> findByIdAndUserId(Long id, Long userId);

    void deleteByIdAndUserId(Long id, Long userId);
}

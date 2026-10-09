package com.medifind.repository;

import com.medifind.entity.PrescriptionDocument;
import com.medifind.enums.PrescriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<PrescriptionDocument, Long> {
    List<PrescriptionDocument> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<PrescriptionDocument> findByPharmacyIdOrderByCreatedAtDesc(Long pharmacyId);

    List<PrescriptionDocument> findByPharmacyIdAndStatusOrderByCreatedAtDesc(Long pharmacyId, PrescriptionStatus status);

    Optional<PrescriptionDocument> findByIdAndUserId(Long id, Long userId);
}
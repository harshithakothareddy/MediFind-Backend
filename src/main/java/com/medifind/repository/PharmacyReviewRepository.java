package com.medifind.repository;

import com.medifind.entity.PharmacyReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PharmacyReviewRepository extends JpaRepository<PharmacyReview, Long> {
    List<PharmacyReview> findByPharmacyIdOrderByCreatedAtDesc(Long pharmacyId);

    Optional<PharmacyReview> findByReservationId(Long reservationId);

    long countByPharmacyId(Long pharmacyId);

    @Query("SELECT AVG(r.rating) FROM PharmacyReview r WHERE r.pharmacy.id = :pharmacyId")
    Double averageRatingByPharmacyId(@Param("pharmacyId") Long pharmacyId);
}
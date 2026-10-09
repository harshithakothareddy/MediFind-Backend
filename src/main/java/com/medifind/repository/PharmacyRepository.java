package com.medifind.repository;

import com.medifind.entity.Pharmacy;
import com.medifind.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PharmacyRepository extends JpaRepository<Pharmacy, Long>, JpaSpecificationExecutor<Pharmacy> {

    Optional<Pharmacy> findByOwnerId(Long ownerId);

    Page<Pharmacy> findByVerificationStatus(VerificationStatus status, Pageable pageable);

    long countByVerificationStatus(VerificationStatus status);

    long countByVerifiedTrue();

    List<Pharmacy> findByVerifiedTrue();

    @Query(value = """
        SELECT p.*, (6371 * acos(
            cos(radians(:userLat)) * cos(radians(p.latitude)) *
            cos(radians(p.longitude) - radians(:userLng)) +
            sin(radians(:userLat)) * sin(radians(p.latitude))
        )) AS distance
        FROM pharmacies p
        WHERE p.verification_status = 'VERIFIED'
        HAVING distance <= :radiusKm
        ORDER BY distance ASC
        """, nativeQuery = true)
    List<Pharmacy> findNearbyPharmacies(
        @Param("userLat") double userLat,
        @Param("userLng") double userLng,
        @Param("radiusKm") double radiusKm
    );
}

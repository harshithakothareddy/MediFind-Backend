package com.medifind.repository;

import com.medifind.entity.FavoritePharmacy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FavoritePharmacyRepository extends JpaRepository<FavoritePharmacy, Long> {

    Page<FavoritePharmacy> findByUserId(Long userId, Pageable pageable);

    Optional<FavoritePharmacy> findByUserIdAndPharmacyId(Long userId, Long pharmacyId);

    boolean existsByUserIdAndPharmacyId(Long userId, Long pharmacyId);

    void deleteByUserIdAndPharmacyId(Long userId, Long pharmacyId);
}

package com.medifind.repository;

import com.medifind.entity.FavoriteMedicine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FavoriteMedicineRepository extends JpaRepository<FavoriteMedicine, Long> {

    Page<FavoriteMedicine> findByUserId(Long userId, Pageable pageable);

    Optional<FavoriteMedicine> findByUserIdAndMedicineId(Long userId, Long medicineId);

    boolean existsByUserIdAndMedicineId(Long userId, Long medicineId);

    void deleteByUserIdAndMedicineId(Long userId, Long medicineId);
}

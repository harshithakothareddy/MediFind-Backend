package com.medifind.repository;

import com.medifind.entity.Inventory;
import com.medifind.enums.AvailabilityStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i JOIN FETCH i.pharmacy JOIN FETCH i.medicine WHERE i.id = :inventoryId")
    Optional<Inventory> findByIdForUpdate(@Param("inventoryId") Long inventoryId);

    Optional<Inventory> findByPharmacyIdAndMedicineId(Long pharmacyId, Long medicineId);

    Page<Inventory> findByPharmacyId(Long pharmacyId, Pageable pageable);

    Page<Inventory> findByPharmacyIdAndAvailabilityStatus(Long pharmacyId, AvailabilityStatus status, Pageable pageable);

    List<Inventory> findByPharmacyId(Long pharmacyId);

    List<Inventory> findByMedicineId(Long medicineId);

        List<Inventory> findByPharmacyIdAndExpiryDateLessThanEqualAndStockQuantityGreaterThanOrderByExpiryDateAsc(
            Long pharmacyId, LocalDate throughDate, Integer minimumQuantity);

    @Query("""
        SELECT i FROM Inventory i
        JOIN FETCH i.pharmacy p
        JOIN FETCH i.medicine m
                WHERE (LOWER(m.name) LIKE LOWER(CONCAT('%', :medicineQuery, '%'))
           OR LOWER(m.genericName) LIKE LOWER(CONCAT('%', :medicineQuery, '%')))
          AND (:status IS NULL OR i.availabilityStatus = :status)
                    AND (i.expiryDate IS NULL OR i.expiryDate >= CURRENT_DATE)
          AND (:verifiedOnly = false OR p.verified = true)
        """)
    List<Inventory> searchAvailability(
        @Param("medicineQuery") String medicineQuery,
        @Param("status") AvailabilityStatus status,
        @Param("verifiedOnly") boolean verifiedOnly
    );

    long countByPharmacyId(Long pharmacyId);

    long countByPharmacyIdAndAvailabilityStatus(Long pharmacyId, AvailabilityStatus status);

    long countByAvailabilityStatus(AvailabilityStatus status);

    Page<Inventory> findByAvailabilityStatus(AvailabilityStatus status, Pageable pageable);

    @Query("SELECT i FROM Inventory i JOIN FETCH i.pharmacy p JOIN FETCH i.medicine m ORDER BY i.lastUpdatedAt DESC")
    Page<Inventory> findRecentUpdates(Pageable pageable);

    @Query("SELECT i FROM Inventory i JOIN FETCH i.pharmacy p JOIN FETCH i.medicine m WHERE m.id IN :medicineIds AND (i.expiryDate IS NULL OR i.expiryDate >= CURRENT_DATE)")
    List<Inventory> findByMedicineIdIn(@Param("medicineIds") List<Long> medicineIds);
}

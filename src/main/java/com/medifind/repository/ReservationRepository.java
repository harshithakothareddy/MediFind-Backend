package com.medifind.repository;

import com.medifind.entity.MedicineReservation;
import com.medifind.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<MedicineReservation, Long> {
    List<MedicineReservation> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<MedicineReservation> findByInventoryPharmacyIdOrderByCreatedAtDesc(Long pharmacyId);

    List<MedicineReservation> findByStatusInAndExpiresAtBefore(
            Collection<ReservationStatus> statuses, LocalDateTime expiresAt);

    boolean existsByUserIdAndInventoryPharmacyIdAndStatus(Long userId, Long pharmacyId, ReservationStatus status);

    Optional<MedicineReservation> findByIdAndUserId(Long id, Long userId);

    Optional<MedicineReservation> findByReservationCode(String reservationCode);
}
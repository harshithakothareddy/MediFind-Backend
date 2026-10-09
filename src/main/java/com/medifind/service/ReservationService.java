package com.medifind.service;

import com.medifind.dto.reservation.ReservationCreateRequest;
import com.medifind.dto.reservation.ReservationDto;
import com.medifind.entity.Inventory;
import com.medifind.entity.MedicineReservation;
import com.medifind.entity.Notification;
import com.medifind.entity.Pharmacy;
import com.medifind.entity.User;
import com.medifind.enums.NotificationType;
import com.medifind.enums.ReservationStatus;
import com.medifind.enums.Role;
import com.medifind.exception.BadRequestException;
import com.medifind.exception.ResourceNotFoundException;
import com.medifind.repository.InventoryRepository;
import com.medifind.repository.NotificationRepository;
import com.medifind.repository.PharmacyRepository;
import com.medifind.repository.ReservationRepository;
import com.medifind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final List<ReservationStatus> HOLD_STATUSES =
            List.of(ReservationStatus.CONFIRMED, ReservationStatus.READY_FOR_PICKUP);

    private final ReservationRepository reservationRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final PharmacyRepository pharmacyRepository;
    private final NotificationRepository notificationRepository;
    private final InventoryService inventoryService;

    @Transactional
    public ReservationDto create(String email, ReservationCreateRequest request) {
        User user = getUser(email);
        Inventory inventory = inventoryRepository.findByIdForUpdate(request.getInventoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", request.getInventoryId()));
        if (!Boolean.TRUE.equals(inventory.getPharmacy().getVerified())) {
            throw new BadRequestException("Reservations are available only at verified pharmacies.");
        }
        if (inventory.getExpiryDate() != null && inventory.getExpiryDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("This medicine batch has expired and cannot be reserved.");
        }
        if (inventory.getStockQuantity() == null || inventory.getStockQuantity() < request.getQuantity()) {
            throw new BadRequestException("There is not enough stock to reserve that quantity.");
        }

        LocalDateTime now = LocalDateTime.now();
        MedicineReservation reservation = MedicineReservation.builder()
                .reservationCode("MF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase())
                .user(user)
                .inventory(inventory)
                .quantity(request.getQuantity())
                .unitPrice(inventory.getPrice())
                .notes(request.getNotes() == null || request.getNotes().isBlank() ? null : request.getNotes().trim())
                .status(ReservationStatus.CONFIRMED)
                .expiresAt(now.plusHours(3))
                .build();

        inventoryService.setStockQuantity(inventory.getId(), inventory.getStockQuantity() - request.getQuantity());
        return toDto(reservationRepository.save(reservation));
    }

    @Transactional(readOnly = true)
    public List<ReservationDto> getForUser(String email) {
        Long userId = getUser(email).getId();
        return reservationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationDto> getForPharmacy(String email) {
        User user = getUser(email);
        if (user.getRole() == Role.ADMIN) {
            return reservationRepository.findAll().stream().map(this::toDto).toList();
        }
        Pharmacy pharmacy = pharmacyRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy not found for owner"));
        return reservationRepository.findByInventoryPharmacyIdOrderByCreatedAtDesc(pharmacy.getId())
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public ReservationDto cancelByUser(Long reservationId, String email) {
        User user = getUser(email);
        MedicineReservation reservation = findReservation(reservationId);
        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can only cancel your own reservations.");
        }
        if (!HOLD_STATUSES.contains(reservation.getStatus())) {
            throw new BadRequestException("This reservation can no longer be cancelled.");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        inventoryService.setStockQuantity(reservation.getInventory().getId(),
                reservation.getInventory().getStockQuantity() + reservation.getQuantity());
        return toDto(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationDto updateByPharmacy(Long reservationId, ReservationStatus nextStatus, String email) {
        User user = getUser(email);
        MedicineReservation reservation = findReservation(reservationId);
        Long pharmacyId = reservation.getInventory().getPharmacy().getId();
        if (user.getRole() != Role.ADMIN) {
            Pharmacy ownedPharmacy = pharmacyRepository.findByOwnerId(user.getId())
                    .orElseThrow(() -> new AccessDeniedException("No pharmacy is linked to this account."));
            if (!ownedPharmacy.getId().equals(pharmacyId)) {
                throw new AccessDeniedException("You can only update reservations for your pharmacy.");
            }
        }

        boolean validTransition = (reservation.getStatus() == ReservationStatus.CONFIRMED
                && nextStatus == ReservationStatus.READY_FOR_PICKUP)
                || (reservation.getStatus() == ReservationStatus.READY_FOR_PICKUP
                && nextStatus == ReservationStatus.PICKED_UP);
        if (!validTransition) {
            throw new BadRequestException("Reservation status transition is not allowed.");
        }

        reservation.setStatus(nextStatus);
        if (nextStatus == ReservationStatus.READY_FOR_PICKUP) {
            notificationRepository.save(Notification.builder()
                    .user(reservation.getUser())
                    .type(NotificationType.PHARMACY_UPDATE)
                    .title("Your reservation is ready")
                    .message(reservation.getInventory().getMedicine().getName() + " is ready for pickup at "
                            + reservation.getInventory().getPharmacy().getName() + ".")
                    .build());
        }
        return toDto(reservationRepository.save(reservation));
    }

    @Transactional(readOnly = true)
    public ReservationDto getByCode(String code) {
        MedicineReservation reservation = reservationRepository.findByReservationCode(code.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with code: " + code));
        return toDto(reservation);
    }

    @Transactional
    public ReservationDto verifyPickupByCode(String code, String email) {
        User user = getUser(email);
        MedicineReservation reservation = reservationRepository.findByReservationCode(code.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with code: " + code));

        Long pharmacyId = reservation.getInventory().getPharmacy().getId();
        if (user.getRole() != Role.ADMIN) {
            Pharmacy ownedPharmacy = pharmacyRepository.findByOwnerId(user.getId())
                    .orElseThrow(() -> new AccessDeniedException("No pharmacy is linked to this account."));
            if (!ownedPharmacy.getId().equals(pharmacyId)) {
                throw new AccessDeniedException("You can only verify reservations for your pharmacy.");
            }
        }

        if (reservation.getStatus() == ReservationStatus.PICKED_UP) {
            throw new BadRequestException("This reservation has already been picked up.");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED || reservation.getStatus() == ReservationStatus.EXPIRED) {
            throw new BadRequestException("This reservation is no longer active (" + reservation.getStatus() + ").");
        }

        reservation.setStatus(ReservationStatus.PICKED_UP);
        notificationRepository.save(Notification.builder()
                .user(reservation.getUser())
                .type(NotificationType.PHARMACY_UPDATE)
                .title("Medicine Picked Up Successfully")
                .message("You picked up " + reservation.getInventory().getMedicine().getName() + " from "
                        + reservation.getInventory().getPharmacy().getName() + ".")
                .build());

        return toDto(reservationRepository.save(reservation));
    }

    @Scheduled(fixedDelayString = "60000")
    @Transactional
    public void expireOverdueReservations() {
        List<MedicineReservation> overdue = reservationRepository
                .findByStatusInAndExpiresAtBefore(HOLD_STATUSES, LocalDateTime.now());
        for (MedicineReservation reservation : overdue) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            Inventory inventory = reservation.getInventory();
            inventoryService.setStockQuantity(inventory.getId(), inventory.getStockQuantity() + reservation.getQuantity());
            notificationRepository.save(Notification.builder()
                    .user(reservation.getUser())
                    .type(NotificationType.SYSTEM)
                    .title("Reservation expired")
                    .message("Your hold for " + inventory.getMedicine().getName() + " expired and the stock was released.")
                    .build());
        }
        reservationRepository.saveAll(overdue);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private MedicineReservation findReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));
    }

    private ReservationDto toDto(MedicineReservation reservation) {
        Inventory inventory = reservation.getInventory();
        Pharmacy pharmacy = inventory.getPharmacy();
        return ReservationDto.builder()
                .id(reservation.getId())
                .reservationCode(reservation.getReservationCode())
                .inventoryId(inventory.getId())
                .medicineId(inventory.getMedicine().getId())
                .medicineName(inventory.getMedicine().getName())
                .genericName(inventory.getMedicine().getGenericName())
                .strength(inventory.getMedicine().getStrength())
                .dosageForm(inventory.getMedicine().getDosageForm())
                .pharmacyId(pharmacy.getId())
                .pharmacyName(pharmacy.getName())
                .pharmacyAddress(pharmacy.getAddress())
                .pharmacyPhone(pharmacy.getPhone())
                .customerName(reservation.getUser().getFullName())
                .quantity(reservation.getQuantity())
                .unitPrice(reservation.getUnitPrice())
                .totalPrice(reservation.getUnitPrice().multiply(BigDecimal.valueOf(reservation.getQuantity())))
                .notes(reservation.getNotes())
                .status(reservation.getStatus())
                .createdAt(reservation.getCreatedAt())
                .expiresAt(reservation.getExpiresAt())
                .build();
    }
}
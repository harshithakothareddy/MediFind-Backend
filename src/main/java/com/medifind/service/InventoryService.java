package com.medifind.service;

import com.medifind.dto.PageResponse;
import com.medifind.dto.inventory.InventoryDto;
import com.medifind.dto.inventory.InventoryUpsertRequest;
import com.medifind.dto.inventory.InventoryUpdateRequest;
import com.medifind.entity.Inventory;
import com.medifind.entity.Medicine;
import com.medifind.entity.AvailabilityAlert;
import com.medifind.entity.Notification;
import com.medifind.entity.Pharmacy;
import com.medifind.enums.NotificationType;
import com.medifind.enums.AvailabilityStatus;
import com.medifind.exception.DuplicateResourceException;
import com.medifind.exception.ResourceNotFoundException;
import com.medifind.repository.InventoryRepository;
import com.medifind.repository.AlertRepository;
import com.medifind.repository.MedicineRepository;
import com.medifind.repository.NotificationRepository;
import com.medifind.repository.PharmacyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final PharmacyRepository pharmacyRepository;
    private final MedicineRepository medicineRepository;
    private final AlertRepository alertRepository;
    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public PageResponse<InventoryDto> getPharmacyInventory(Long pharmacyId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastUpdatedAt").descending());
        Page<Inventory> result = inventoryRepository.findByPharmacyId(pharmacyId, pageable);
        return toPageResponse(result);
    }

    @Transactional
    public InventoryDto upsertInventory(Long pharmacyId, InventoryUpsertRequest req) {
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", pharmacyId));
        Medicine medicine = medicineRepository.findById(req.getMedicineId())
                .orElseThrow(() -> new ResourceNotFoundException("Medicine", req.getMedicineId()));

        Inventory inventory = inventoryRepository
                .findByPharmacyIdAndMedicineId(pharmacyId, req.getMedicineId())
                .orElse(Inventory.builder()
                        .pharmacy(pharmacy)
                        .medicine(medicine)
                        .build());
                boolean wasOutOfStock = inventory.getId() == null
                    || inventory.getStockQuantity() == null
                    || inventory.getStockQuantity() <= 0;

        inventory.setStockQuantity(req.getStockQuantity());
        inventory.setMinimumStockLevel(req.getMinimumStockLevel());
        inventory.setPrice(req.getPrice());
        inventory.setBatchNumber(req.getBatchNumber());
        inventory.setExpiryDate(req.getExpiryDate());
        inventory.setLastUpdatedAt(LocalDateTime.now());
        // @PrePersist/@PreUpdate will compute availabilityStatus

        Inventory saved = inventoryRepository.save(inventory);
        if (wasOutOfStock && saved.getStockQuantity() > 0) {
            notifyAvailabilitySubscribers(medicine, LocalDateTime.now());
        }
        return toDto(saved);
    }

    private void notifyAvailabilitySubscribers(Medicine medicine, LocalDateTime restockedAt) {
        List<AvailabilityAlert> alerts = alertRepository.findByMedicineIdAndActiveTrue(medicine.getId());
        for (AvailabilityAlert alert : alerts) {
            notificationRepository.save(Notification.builder()
                    .user(alert.getUser())
                    .type(NotificationType.STOCK_AVAILABLE)
                    .title(medicine.getName() + " is back in stock")
                    .message("A pharmacy has restocked " + medicine.getName() + ". Check current availability.")
                    .build());
            alert.setActive(false);
            alert.setTriggeredAt(restockedAt);
        }
        alertRepository.saveAll(alerts);
    }

    @Transactional
    public InventoryDto setStockQuantity(Long inventoryId, int quantity) {
        if (quantity < 0) {
            throw new com.medifind.exception.BadRequestException("Stock quantity cannot be negative.");
        }
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", inventoryId));
        boolean wasOutOfStock = inventory.getStockQuantity() == null || inventory.getStockQuantity() <= 0;
        inventory.setStockQuantity(quantity);
        inventory.setLastUpdatedAt(LocalDateTime.now());
        Inventory saved = inventoryRepository.save(inventory);
        if (wasOutOfStock && quantity > 0) {
            notifyAvailabilitySubscribers(saved.getMedicine(), LocalDateTime.now());
        }
        return toDto(saved);
    }

    @Transactional
    public InventoryDto updateInventory(Long inventoryId, InventoryUpdateRequest request) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", inventoryId));
        boolean wasOutOfStock = inventory.getStockQuantity() == null || inventory.getStockQuantity() <= 0;
        if (request.getStockQuantity() != null) inventory.setStockQuantity(request.getStockQuantity());
        if (request.getMinimumStockLevel() != null) inventory.setMinimumStockLevel(request.getMinimumStockLevel());
        if (request.getPrice() != null) inventory.setPrice(request.getPrice());
        if (request.getBatchNumber() != null) inventory.setBatchNumber(request.getBatchNumber().trim());
        if (request.getExpiryDate() != null) inventory.setExpiryDate(request.getExpiryDate());
        inventory.setLastUpdatedAt(LocalDateTime.now());
        Inventory saved = inventoryRepository.save(inventory);
        if (wasOutOfStock && saved.getStockQuantity() > 0) {
            notifyAvailabilitySubscribers(saved.getMedicine(), LocalDateTime.now());
        }
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<InventoryDto> getByStatus(Long pharmacyId, AvailabilityStatus status) {
        return inventoryRepository.findByPharmacyIdAndAvailabilityStatus(
                        pharmacyId, status, PageRequest.of(0, 100, Sort.by("lastUpdatedAt").descending()))
                .getContent().stream().map(this::toDto).toList();
    }

    @Transactional
    public void deleteInventory(Long pharmacyId, Long inventoryId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", inventoryId));
        if (!inventory.getPharmacy().getId().equals(pharmacyId)) {
            throw new ResourceNotFoundException("Inventory not found for this pharmacy");
        }
        inventoryRepository.delete(inventory);
    }

    @Transactional(readOnly = true)
    public List<InventoryDto> searchAvailability(String query, boolean verifiedOnly) {
        List<Inventory> results = inventoryRepository.searchAvailability(
                query, AvailabilityStatus.IN_STOCK, verifiedOnly);
        return results.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryDto> getRecentUpdates(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return inventoryRepository.findRecentUpdates(pageable)
                .getContent().stream().map(this::toDto).toList();
    }

    public InventoryDto toDto(Inventory inv) {
        return InventoryDto.builder()
                .id(inv.getId())
                .pharmacy(PharmacyService.toDto(inv.getPharmacy(), null))
                .medicine(MedicineService.toDto(inv.getMedicine()))
                .stockQuantity(inv.getStockQuantity())
                .minimumStockLevel(inv.getMinimumStockLevel())
                .price(inv.getPrice())
                .batchNumber(inv.getBatchNumber())
                .expiryDate(inv.getExpiryDate())
                .availabilityStatus(inv.getAvailabilityStatus())
                .dataSource(inv.getDataSource())
                .dataType(inv.getDataType())
                .lastUpdatedAt(inv.getLastUpdatedAt())
                .build();
    }

            @Transactional(readOnly = true)
            public List<InventoryDto> getExpiryWatch(Long pharmacyId, int days) {
            int safeDays = Math.max(1, Math.min(days, 365));
            LocalDate throughDate = LocalDate.now().plusDays(safeDays);
            return inventoryRepository
                .findByPharmacyIdAndExpiryDateLessThanEqualAndStockQuantityGreaterThanOrderByExpiryDateAsc(
                    pharmacyId, throughDate, 0)
                .stream().map(this::toDto).toList();
            }

    private PageResponse<InventoryDto> toPageResponse(Page<Inventory> page) {
        return PageResponse.<InventoryDto>builder()
                .content(page.getContent().stream().map(this::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}

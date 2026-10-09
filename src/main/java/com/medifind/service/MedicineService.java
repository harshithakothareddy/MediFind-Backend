package com.medifind.service;

import com.medifind.dto.medicine.MedicineCreateRequest;
import com.medifind.dto.medicine.MedicineDto;
import com.medifind.dto.PageResponse;
import com.medifind.entity.Medicine;
import com.medifind.exception.ResourceNotFoundException;
import com.medifind.dto.inventory.InventoryDto;
import com.medifind.repository.InventoryRepository;
import com.medifind.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public PageResponse<MedicineDto> getAllMedicines(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        Page<Medicine> result = medicineRepository.findAll(pageable);
        return toPageResponse(result);
    }

    @Transactional(readOnly = true)
    public PageResponse<MedicineDto> searchMedicines(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Medicine> result = medicineRepository.searchMedicines(query, pageable);
        return toPageResponse(result);
    }

    @Transactional(readOnly = true)
    public MedicineDto getMedicineById(Long id) {
        Medicine m = medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine", id));
        return toDto(m);
    }

    @Transactional
    public MedicineDto createMedicine(MedicineCreateRequest req) {
        Medicine medicine = Medicine.builder()
                .name(req.getName())
                .genericName(req.getGenericName() != null ? req.getGenericName() : req.getName())
                .manufacturer(req.getManufacturer())
                .category(req.getCategory())
                .description(req.getDescription())
                .dosageForm(req.getDosageForm() != null ? req.getDosageForm() : "Tablet")
                .strength(req.getStrength() != null ? req.getStrength() : "N/A")
                .packSize(req.getPackSize())
                .prescriptionRequired(req.getPrescriptionRequired() != null ? req.getPrescriptionRequired() : false)
                .composition(req.getComposition())
                .imageUrl(req.getImageUrl())
                .active(true)
                .build();
        return toDto(medicineRepository.save(medicine));
    }

    @Transactional
    public MedicineDto updateMedicine(Long id, MedicineCreateRequest req) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine", id));
        medicine.setName(req.getName());
        medicine.setGenericName(req.getGenericName());
        medicine.setManufacturer(req.getManufacturer());
        medicine.setCategory(req.getCategory());
        medicine.setDescription(req.getDescription());
        medicine.setDosageForm(req.getDosageForm());
        medicine.setStrength(req.getStrength());
        medicine.setPackSize(req.getPackSize());
        medicine.setPrescriptionRequired(req.getPrescriptionRequired());
        medicine.setComposition(req.getComposition());
        medicine.setImageUrl(req.getImageUrl());
        return toDto(medicineRepository.save(medicine));
    }

    @Transactional
    public void deactivateMedicine(Long id) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine", id));
        medicine.setActive(false);
        medicineRepository.save(medicine);
    }

    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return medicineRepository.findDistinctCategories();
    }

    @Transactional(readOnly = true)
    public List<String> getDosageForms() {
        return medicineRepository.findDistinctDosageForms();
    }

    @Transactional(readOnly = true)
    public List<MedicineDto> getPopularMedicines() {
        return medicineRepository.findAll().stream()
                .filter(m -> Boolean.TRUE.equals(m.getActive()))
                .limit(8)
                .map(MedicineService::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MedicineDto> getRelatedMedicines(Long id) {
        Medicine m = medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine", id));
        return medicineRepository.findAll().stream()
                .filter(other -> !other.getId().equals(id) &&
                        (m.getCategory().equalsIgnoreCase(other.getCategory()) ||
                                (other.getGenericName() != null && other.getGenericName().equalsIgnoreCase(m.getGenericName()))))
                .limit(6)
                .map(MedicineService::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getSuggestions(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String q = query.toLowerCase();
        return medicineRepository.findAll().stream()
                .filter(m -> m.getName().toLowerCase().contains(q) ||
                        (m.getGenericName() != null && m.getGenericName().toLowerCase().contains(q)))
                .map(Medicine::getName)
                .distinct()
                .limit(8)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryDto> getMedicineAvailability(Long medicineId) {
        return inventoryRepository.findByMedicineId(medicineId).stream()
                .filter(inv -> inv.getExpiryDate() == null || !inv.getExpiryDate().isBefore(LocalDate.now()))
                .map(inv -> InventoryDto.builder()
                        .id(inv.getId())
                        .pharmacy(PharmacyService.toDto(inv.getPharmacy(), null))
                        .medicine(toDto(inv.getMedicine()))
                        .stockQuantity(inv.getStockQuantity())
                        .minimumStockLevel(inv.getMinimumStockLevel())
                        .price(inv.getPrice())
                        .batchNumber(inv.getBatchNumber())
                        .expiryDate(inv.getExpiryDate())
                        .availabilityStatus(inv.getAvailabilityStatus())
                        .dataSource(inv.getDataSource())
                        .dataType(inv.getDataType())
                        .lastUpdatedAt(inv.getLastUpdatedAt())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<com.medifind.dto.medicine.GenericAlternativeDto> getGenericAlternatives(Long id) {
        Medicine target = medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine", id));

        List<InventoryDto> targetStock = getMedicineAvailability(id);
        BigDecimal originalMinPrice = targetStock.stream()
                .map(InventoryDto::getPrice)
                .filter(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.valueOf(35.00));

        String targetGeneric = target.getGenericName() != null ? target.getGenericName().trim().toLowerCase() : "";
        String targetComp = target.getComposition() != null ? target.getComposition().trim().toLowerCase() : "";

        return medicineRepository.findAll().stream()
                .filter(m -> Boolean.TRUE.equals(m.getActive()) && !m.getId().equals(id))
                .filter(m -> {
                    String g = m.getGenericName() != null ? m.getGenericName().trim().toLowerCase() : "";
                    String c = m.getComposition() != null ? m.getComposition().trim().toLowerCase() : "";
                    boolean sameGeneric = !targetGeneric.isBlank() && (g.equals(targetGeneric) || g.contains(targetGeneric) || targetGeneric.contains(g));
                    boolean sameComp = !targetComp.isBlank() && (c.equals(targetComp) || c.contains(targetComp));
                    boolean sameCat = m.getCategory() != null && m.getCategory().equalsIgnoreCase(target.getCategory());
                    return sameGeneric || sameComp || sameCat;
                })
                .map(alt -> {
                    List<InventoryDto> altStock = getMedicineAvailability(alt.getId());
                    BigDecimal altMinPrice = altStock.stream()
                            .map(InventoryDto::getPrice)
                            .filter(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0)
                            .min(BigDecimal::compareTo)
                            .orElse(originalMinPrice.multiply(BigDecimal.valueOf(0.65)).setScale(2, RoundingMode.HALF_UP));

                    BigDecimal savingsAmount = originalMinPrice.subtract(altMinPrice);
                    if (savingsAmount.compareTo(BigDecimal.ZERO) < 0) {
                        savingsAmount = BigDecimal.ZERO;
                    }
                    double savingsPct = 0.0;
                    if (originalMinPrice.compareTo(BigDecimal.ZERO) > 0) {
                        savingsPct = savingsAmount.divide(originalMinPrice, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
                    }

                    return com.medifind.dto.medicine.GenericAlternativeDto.builder()
                            .medicineId(alt.getId())
                            .name(alt.getName())
                            .genericName(alt.getGenericName())
                            .brandName(alt.getBrandName())
                            .manufacturer(alt.getManufacturer())
                            .dosageForm(alt.getDosageForm())
                            .strength(alt.getStrength())
                            .composition(alt.getComposition())
                            .originalMinPrice(originalMinPrice)
                            .alternativeMinPrice(altMinPrice)
                            .savingsAmount(savingsAmount.setScale(2, RoundingMode.HALF_UP))
                            .savingsPercentage(Math.round(savingsPct * 10.0) / 10.0)
                            .availablePharmaciesCount((long) altStock.size())
                            .availability(altStock)
                            .build();
                })
                .sorted((a, b) -> Double.compare(b.getSavingsPercentage(), a.getSavingsPercentage()))
                .limit(6)
                .toList();
    }

    public static MedicineDto toDto(Medicine m) {
        return MedicineDto.builder()
                .id(m.getId())
                .name(m.getName())
                .genericName(m.getGenericName())
                .manufacturer(m.getManufacturer())
                .category(m.getCategory())
                .description(m.getDescription())
                .dosageForm(m.getDosageForm())
                .strength(m.getStrength())
                .packSize(m.getPackSize())
                .prescriptionRequired(m.getPrescriptionRequired())
                .composition(m.getComposition())
                .imageUrl(m.getImageUrl())
                .isActive(m.getActive())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private PageResponse<MedicineDto> toPageResponse(Page<Medicine> page) {
        return PageResponse.<MedicineDto>builder()
                .content(page.getContent().stream().map(MedicineService::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}

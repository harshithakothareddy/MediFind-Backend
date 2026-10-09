package com.medifind.dto.prescription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionScanResultDto {
    private String scannedDoctorName;
    private String scannedDate;
    private String patientName;
    private String diagnosis;
    private List<ScannedMedicineItemDto> extractedMedicines;
    private List<PharmacyFulfillmentDto> pharmacyMatches;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScannedMedicineItemDto {
        private String rawText;
        private Long matchedMedicineId;
        private String matchedMedicineName;
        private String genericName;
        private String strength;
        private String dosageForm;
        private Integer quantity;
        private String frequency;
        private String instructions;
        private Boolean matched;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PharmacyFulfillmentDto {
        private Long pharmacyId;
        private String pharmacyName;
        private String area;
        private String address;
        private String phone;
        private Boolean open24Hours;
        private Boolean verified;
        private Double rating;
        private String distance;
        private Integer itemsAvailableCount;
        private Integer totalItemsCount;
        private Double matchPercentage;
        private BigDecimal totalEstimatedPrice;
        private List<FulfilledItemDto> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FulfilledItemDto {
        private Long medicineId;
        private String medicineName;
        private Long inventoryId;
        private Boolean inStock;
        private Integer availableStock;
        private BigDecimal price;
    }
}

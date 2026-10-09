package com.medifind.dto.medicine;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MedicineDto {
    private Long id;
    private String name;
    private String genericName;
    private String brandName;
    private String manufacturer;
    private String category;
    private String description;
    private String dosageForm;
    private String strength;
    private String packSize;
    private Boolean prescriptionRequired;
    private String composition;
    private String imageUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
    // Availability info (when fetched in context of a search)
    private Long availablePharmaciesCount;
}

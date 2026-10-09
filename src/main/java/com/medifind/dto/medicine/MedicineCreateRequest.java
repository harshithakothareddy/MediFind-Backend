package com.medifind.dto.medicine;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class MedicineCreateRequest {

    @NotBlank(message = "Medicine name is required")
    @Size(max = 200)
    private String name;

    @Size(max = 200)
    private String genericName;

    @NotBlank(message = "Manufacturer is required")
    @Size(max = 150)
    private String manufacturer;

    @NotBlank(message = "Category is required")
    private String category;

    private String subCategory;
    private String description;
    private String dosageForm;
    private String strength;
    private String packSize;
    private Boolean prescriptionRequired = false;
    private String composition;
    private String sideEffects;
    private String contraindications;
    private String storageConditions;
    private String imageUrl;
}

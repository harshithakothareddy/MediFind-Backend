package com.medifind.dto.medicine;

import com.medifind.dto.inventory.InventoryDto;
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
public class GenericAlternativeDto {
    private Long medicineId;
    private String name;
    private String genericName;
    private String brandName;
    private String manufacturer;
    private String dosageForm;
    private String strength;
    private String composition;
    private BigDecimal originalMinPrice;
    private BigDecimal alternativeMinPrice;
    private BigDecimal savingsAmount;
    private Double savingsPercentage;
    private Long availablePharmaciesCount;
    private List<InventoryDto> availability;
}

package com.medifind.dto.search;

import com.medifind.dto.inventory.InventoryDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Result for a medicine availability search:
 * which pharmacies near the user carry a given medicine.
 */
@Data
@Builder
public class AvailabilityResult {
    private Long medicineId;
    private String medicineName;
    private String genericName;
    private int totalPharmaciesFound;
    private List<InventoryDto> availableAt;
}

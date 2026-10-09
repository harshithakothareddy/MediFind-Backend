package com.medifind.dto.inventory;

import com.medifind.dto.medicine.MedicineDto;
import com.medifind.dto.pharmacy.PharmacyDto;
import com.medifind.enums.AvailabilityStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class InventoryDto {
    private Long id;
    private PharmacyDto pharmacy;
    private MedicineDto medicine;
    private Integer stockQuantity;
    private Integer minimumStockLevel;
    private BigDecimal price;
    private String batchNumber;
    private LocalDate expiryDate;
    private AvailabilityStatus availabilityStatus;
    private String dataSource;
    private String dataType;
    private LocalDateTime lastUpdatedAt;
}

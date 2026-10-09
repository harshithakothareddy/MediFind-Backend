package com.medifind.dto.inventory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InventoryUpdateRequest {
    @Min(0)
    private Integer stockQuantity;

    @Min(0)
    private Integer minimumStockLevel;

    @DecimalMin("0.01")
    private BigDecimal price;

    @Size(max = 80)
    private String batchNumber;

    private LocalDate expiryDate;
}
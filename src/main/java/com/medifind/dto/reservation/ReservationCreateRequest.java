package com.medifind.dto.reservation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReservationCreateRequest {

    @NotNull
    private Long inventoryId;

    @NotNull
    @Min(1)
    @Max(10)
    private Integer quantity;

    @Size(max = 500)
    private String notes;
}
package com.medifind.dto.alert;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AvailabilityAlertRequest {
    @NotNull
    private Long medicineId;
}
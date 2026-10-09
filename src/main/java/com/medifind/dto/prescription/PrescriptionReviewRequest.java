package com.medifind.dto.prescription;

import com.medifind.enums.PrescriptionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PrescriptionReviewRequest {
    @NotNull
    private PrescriptionStatus status;

    @Size(max = 1000)
    private String notes;
}
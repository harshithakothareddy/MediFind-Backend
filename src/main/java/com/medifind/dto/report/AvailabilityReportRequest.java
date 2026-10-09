package com.medifind.dto.report;

import com.medifind.enums.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AvailabilityReportRequest {
    @NotNull
    private Long pharmacyId;

    private Long medicineId;

    @NotNull
    private ReportType type;

    @NotBlank
    @Size(max = 1000)
    private String description;
}
package com.medifind.dto.report;

import com.medifind.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReportStatusUpdateRequest {
    @NotNull
    private ReportStatus status;

    @Size(max = 1000)
    private String notes;
}
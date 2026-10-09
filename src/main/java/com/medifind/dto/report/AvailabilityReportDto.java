package com.medifind.dto.report;

import com.medifind.enums.ReportStatus;
import com.medifind.enums.ReportType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AvailabilityReportDto {
    private Long id;
    private String reporterName;
    private Long pharmacyId;
    private String pharmacyName;
    private Long medicineId;
    private String medicineName;
    private ReportType issueType;
    private String details;
    private ReportStatus status;
    private String adminNotes;
    private LocalDateTime createdAt;
}
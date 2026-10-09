package com.medifind.dto.prescription;

import com.medifind.enums.PrescriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PrescriptionDocumentDto {
    private Long id;
    private Long pharmacyId;
    private String pharmacyName;
    private String patientName;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private PrescriptionStatus status;
    private String reviewNotes;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
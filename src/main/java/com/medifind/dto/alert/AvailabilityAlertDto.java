package com.medifind.dto.alert;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AvailabilityAlertDto {
    private Long id;
    private Long medicineId;
    private String medicineName;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime triggeredAt;
}
package com.medifind.dto.pharmacy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PharmacyHoursDto {
    private String dayOfWeek;
    private String openTime;
    private String closeTime;
    private Boolean closed;
}

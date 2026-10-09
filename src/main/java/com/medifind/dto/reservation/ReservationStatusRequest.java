package com.medifind.dto.reservation;

import com.medifind.enums.ReservationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReservationStatusRequest {
    @NotNull
    private ReservationStatus status;
}
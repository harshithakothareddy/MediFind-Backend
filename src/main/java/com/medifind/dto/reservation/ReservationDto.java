package com.medifind.dto.reservation;

import com.medifind.enums.ReservationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ReservationDto {
    private Long id;
    private String reservationCode;
    private Long inventoryId;
    private Long medicineId;
    private String medicineName;
    private String genericName;
    private String strength;
    private String dosageForm;
    private Long pharmacyId;
    private String pharmacyName;
    private String pharmacyAddress;
    private String pharmacyPhone;
    private String customerName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String notes;
    private ReservationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
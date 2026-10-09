package com.medifind.dto.review;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PharmacyReviewDto {
    private Long id;
    private Long pharmacyId;
    private String customerName;
    private Long reservationId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
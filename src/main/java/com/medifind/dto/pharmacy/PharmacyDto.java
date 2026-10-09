package com.medifind.dto.pharmacy;

import com.medifind.enums.VerificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PharmacyDto {
    private Long id;
    private String name;
    private String licenseNumber;
    private String ownerName;
    private String email;
    private String phone;
    private String address;
    private String area;
    private String city;
    private String state;
    private String pincode;
    private Double latitude;
    private Double longitude;
    private String description;
    private Double rating;
    private Integer totalReviews;
    private Boolean verified;
    private VerificationStatus verificationStatus;
    private Boolean open24Hours;
    private String dataSource;
    private String dataType;
    private List<PharmacyHoursDto> operatingHours;
    // Computed / contextual
    private Double distanceKm;
    private LocalDateTime createdAt;
}

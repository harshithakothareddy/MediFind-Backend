package com.medifind.dto.pharmacy;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PharmacyCreateRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    @Size(max = 60)
    private String licenseNumber;

    @Size(max = 100)
    private String ownerName;

    @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    @NotBlank
    private String address;

    @NotBlank
    private String area;

    @NotBlank
    private String city;

    @NotBlank
    private String state;

    @NotBlank
    private String pincode;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    private String description;
    private Boolean open24Hours = false;
}

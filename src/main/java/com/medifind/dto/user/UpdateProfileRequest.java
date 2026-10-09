package com.medifind.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(min = 2, max = 60)
    private String firstName;
    @Size(min = 2, max = 60)
    private String lastName;
    private String phone;
    private String city;
    private String state;
    private String pincode;
    private Double latitude;
    private Double longitude;
    private String profileImage;
}

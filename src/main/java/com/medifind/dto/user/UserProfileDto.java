package com.medifind.dto.user;

import com.medifind.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserProfileDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Role role;
    private String city;
    private String state;
    private String pincode;
    private Double latitude;
    private Double longitude;
    private String profileImage;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}

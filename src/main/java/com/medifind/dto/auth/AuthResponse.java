package com.medifind.dto.auth;

import com.medifind.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private Role role;
    private String profileImage;
}

package com.medifind.dto.auth;

import com.medifind.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 60)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 60)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be 6–100 characters")
    private String password;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be a 10-digit number")
    private String phone;

    @NotNull(message = "Role is required")
    private Role role;

    private String city;
    private String state;
    private String pincode;
}

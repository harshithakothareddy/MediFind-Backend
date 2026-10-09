package com.medifind.controller;

import com.medifind.dto.ApiResponse;
import com.medifind.dto.PageResponse;
import com.medifind.dto.user.UpdateProfileRequest;
import com.medifind.dto.user.UserProfileDto;
import com.medifind.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** Get currently authenticated user's profile */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileDto>> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(userService.getProfile(userDetails.getUsername())));
    }

    /** Update currently authenticated user's profile */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileDto>> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Profile updated", userService.updateProfile(userDetails.getUsername(), request)));
    }

    /** ADMIN: Get user by ID */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserProfileDto>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getById(id)));
    }

    /** ADMIN: Toggle user account enabled state */
    @PatchMapping("/{id}/toggle-enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggleEnabled(@PathVariable Long id) {
        userService.toggleUserEnabled(id);
        return ResponseEntity.ok(ApiResponse.success("User status toggled", null));
    }

    /** Get user dashboard data */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserProfileDto profile = userService.getProfile(userDetails.getUsername());
        java.util.Map<String, Object> data = java.util.Map.of(
                "profile", profile,
                "recentActivity", java.util.List.of()
        );
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /** Update notification preferences (placeholder) */
    @PutMapping("/notification-preferences")
    public ResponseEntity<ApiResponse<String>> updateNotificationPreferences(
            @RequestBody java.util.Map<String, Object> prefs) {
        return ResponseEntity.ok(ApiResponse.success("Preferences updated", "SUCCESS"));
    }

    /** Upload avatar (placeholder — returns profile image URL) */
    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> uploadAvatar() {
        return ResponseEntity.ok(ApiResponse.success(java.util.Map.of("profileImage", "")));
    }
}

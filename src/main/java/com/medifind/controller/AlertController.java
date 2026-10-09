package com.medifind.controller;

import com.medifind.dto.ApiResponse;
import com.medifind.dto.alert.AvailabilityAlertDto;
import com.medifind.dto.alert.AvailabilityAlertRequest;
import com.medifind.entity.AvailabilityAlert;
import com.medifind.entity.Medicine;
import com.medifind.entity.User;
import com.medifind.exception.ResourceNotFoundException;
import com.medifind.repository.AlertRepository;
import com.medifind.repository.MedicineRepository;
import com.medifind.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertRepository alertRepository;
    private final MedicineRepository medicineRepository;
    private final UserRepository userRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<AvailabilityAlertDto>>> getAllAlerts(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUser(userDetails).getId();
        return ResponseEntity.ok(ApiResponse.success(alertRepository.findByUserId(userId).stream()
                .map(this::toDto).toList()));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<AvailabilityAlertDto>> createAlert(
            @Valid @RequestBody AvailabilityAlertRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        Medicine medicine = medicineRepository.findById(request.getMedicineId())
                .orElseThrow(() -> new ResourceNotFoundException("Medicine", request.getMedicineId()));
        AvailabilityAlert alert = alertRepository.findByUserIdAndMedicineIdAndActiveTrue(user.getId(), medicine.getId())
                .orElseGet(() -> alertRepository.save(AvailabilityAlert.builder()
                        .user(user).medicine(medicine).active(true).build()));
        return ResponseEntity.ok(ApiResponse.success("Availability alert saved", toDto(alert)));
    }

    @PatchMapping("/{id}/disable")
    @Transactional
    public ResponseEntity<ApiResponse<AvailabilityAlertDto>> disableAlert(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        AvailabilityAlert alert = findOwnedAlert(id, getUser(userDetails).getId());
        alert.setActive(false);
        return ResponseEntity.ok(ApiResponse.success("Alert disabled", toDto(alertRepository.save(alert))));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<String>> deleteAlert(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUser(userDetails).getId();
        findOwnedAlert(id, userId);
        alertRepository.deleteByIdAndUserId(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Alert deleted", "SUCCESS"));
    }

    private User getUser(UserDetails userDetails) {
        if (userDetails == null) throw new AccessDeniedException("Sign in to manage availability alerts.");
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user could not be found."));
    }

    private AvailabilityAlert findOwnedAlert(Long id, Long userId) {
        return alertRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", id));
    }

    private AvailabilityAlertDto toDto(AvailabilityAlert alert) {
        return AvailabilityAlertDto.builder()
                .id(alert.getId())
                .medicineId(alert.getMedicine().getId())
                .medicineName(alert.getMedicine().getName())
                .active(alert.getActive())
                .createdAt(alert.getCreatedAt())
                .triggeredAt(alert.getTriggeredAt())
                .build();
    }
}

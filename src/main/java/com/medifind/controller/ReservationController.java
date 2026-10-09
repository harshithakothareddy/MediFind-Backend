package com.medifind.controller;

import com.medifind.dto.ApiResponse;
import com.medifind.dto.reservation.ReservationCreateRequest;
import com.medifind.dto.reservation.ReservationDto;
import com.medifind.dto.reservation.ReservationStatusRequest;
import com.medifind.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'PHARMACY')")
    public ResponseEntity<ApiResponse<ReservationDto>> create(
            @Valid @RequestBody ReservationCreateRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Medicine reserved", reservationService.create(user.getUsername(), request)));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('USER', 'PHARMACY', 'ADMIN')")
    public ResponseEntity<ApiResponse<ReservationDto>> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(reservationService.getByCode(code)));
    }

    @PostMapping("/code/{code}/verify-pickup")
    @PreAuthorize("hasAnyRole('PHARMACY', 'ADMIN')")
    public ResponseEntity<ApiResponse<ReservationDto>> verifyPickup(
            @PathVariable String code,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success("Reservation verified and marked as picked up",
                reservationService.verifyPickupByCode(code, user.getUsername())));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'PHARMACY')")
    public ResponseEntity<ApiResponse<List<ReservationDto>>> mine(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(reservationService.getForUser(user.getUsername())));
    }

    @GetMapping("/pharmacy")
    @PreAuthorize("hasAnyRole('PHARMACY', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ReservationDto>>> pharmacy(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(reservationService.getForPharmacy(user.getUsername())));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<ReservationDto>> cancel(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success("Reservation cancelled",
                reservationService.cancelByUser(id, user.getUsername())));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('PHARMACY', 'ADMIN')")
    public ResponseEntity<ApiResponse<ReservationDto>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ReservationStatusRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success("Reservation updated",
                reservationService.updateByPharmacy(id, request.getStatus(), user.getUsername())));
    }
}
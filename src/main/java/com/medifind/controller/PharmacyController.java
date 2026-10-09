package com.medifind.controller;

import com.medifind.dto.ApiResponse;
import com.medifind.dto.PageResponse;
import com.medifind.dto.pharmacy.PharmacyCreateRequest;
import com.medifind.dto.pharmacy.PharmacyDto;
import com.medifind.enums.VerificationStatus;
import com.medifind.repository.UserRepository;
import com.medifind.service.PharmacyService;
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
@RequestMapping("/pharmacies")
@RequiredArgsConstructor
public class PharmacyController {

    private final PharmacyService pharmacyService;
    private final UserRepository userRepository;
    private final com.medifind.service.InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PharmacyDto>>> getAllPharmacies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(pharmacyService.getAllPharmacies(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PharmacyDto>> getPharmacyById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(pharmacyService.getPharmacyById(id)));
    }

    @GetMapping("/{id}/inventory")
    public ResponseEntity<ApiResponse<com.medifind.dto.PageResponse<com.medifind.dto.inventory.InventoryDto>>> getPharmacyInventory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getPharmacyInventory(id, page, size)));
    }

    @GetMapping("/{id}/hours")
    public ResponseEntity<ApiResponse<List<com.medifind.dto.pharmacy.PharmacyHoursDto>>> getPharmacyHours(@PathVariable Long id) {
        PharmacyDto p = pharmacyService.getPharmacyById(id);
        return ResponseEntity.ok(ApiResponse.success(p.getOperatingHours() != null ? p.getOperatingHours() : List.of()));
    }

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<PharmacyDto>>> getNearbyPharmacies(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5.0") double radius) {
        return ResponseEntity.ok(ApiResponse.success(pharmacyService.getNearbyPharmacies(lat, lng, radius)));
    }

    /** Pharmacy owner registers their pharmacy */
    @PostMapping
    @PreAuthorize("hasAnyRole('PHARMACY', 'ADMIN')")
    public ResponseEntity<ApiResponse<PharmacyDto>> registerPharmacy(
            @Valid @RequestBody PharmacyCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long ownerId = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Pharmacy registered, pending verification",
                        pharmacyService.createPharmacy(request, ownerId)));
    }

    /** Pharmacy owner views their own pharmacy */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('PHARMACY', 'ADMIN')")
    public ResponseEntity<ApiResponse<PharmacyDto>> getMyPharmacy(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long ownerId = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow().getId();
        return ResponseEntity.ok(ApiResponse.success(pharmacyService.getPharmacyByOwner(ownerId)));
    }

    /** Admin verifies or rejects a pharmacy */
    @RequestMapping(value = "/{id}/verify", method = {RequestMethod.PATCH, RequestMethod.PUT})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PharmacyDto>> verifyPharmacy(
            @PathVariable Long id,
            @RequestParam(required = false) VerificationStatus status,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        VerificationStatus finalStatus = status;
        if (finalStatus == null && body != null && body.containsKey("status")) {
            try {
                finalStatus = VerificationStatus.valueOf(body.get("status").toString().toUpperCase());
            } catch (Exception ignored) {
                finalStatus = VerificationStatus.VERIFIED;
            }
        }
        if (finalStatus == null) {
            finalStatus = VerificationStatus.VERIFIED;
        }
        return ResponseEntity.ok(ApiResponse.success(
                "Pharmacy status updated", pharmacyService.verifyPharmacy(id, finalStatus)));
    }

    /** Admin: list pharmacies by verification status */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<PharmacyDto>>> getByStatus(
            @PathVariable VerificationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(pharmacyService.getPharmaciesByStatus(status, page, size)));
    }
}

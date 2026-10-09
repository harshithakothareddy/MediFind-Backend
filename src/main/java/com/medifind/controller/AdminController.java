package com.medifind.controller;

import com.medifind.dto.ApiResponse;
import com.medifind.dto.PageResponse;
import com.medifind.dto.admin.AdminDashboardStats;
import com.medifind.dto.user.UpdateProfileRequest;
import com.medifind.dto.user.UserProfileDto;
import com.medifind.enums.Role;
import com.medifind.service.AdminService;
import com.medifind.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final com.medifind.service.PharmacyService pharmacyService;
    private final com.medifind.service.MedicineService medicineService;
    private final com.medifind.service.InventoryService inventoryService;

    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<AdminDashboardStats>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDashboardStats()));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponse<UserProfileDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers(page, size)));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserProfileDto>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getById(id)));
    }

    @GetMapping("/users/role/{role}")
    public ResponseEntity<ApiResponse<PageResponse<UserProfileDto>>> getUsersByRole(
            @PathVariable Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUsersByRole(role, page, size)));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserProfileDto>> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User updated", userService.updateById(id, request)));
    }

    @PatchMapping("/users/{id}/activate")
    public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable Long id) {
        userService.setUserEnabled(id, true);
        return ResponseEntity.ok(ApiResponse.success("User activated", null));
    }

    @PatchMapping("/users/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable Long id) {
        userService.setUserEnabled(id, false);
        return ResponseEntity.ok(ApiResponse.success("User deactivated", null));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted", null));
    }

    @GetMapping("/pharmacies")
    public ResponseEntity<ApiResponse<PageResponse<com.medifind.dto.pharmacy.PharmacyDto>>> getAdminPharmacies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(pharmacyService.getAllPharmacies(page, size)));
    }

    @GetMapping("/pharmacies/pending")
    public ResponseEntity<ApiResponse<PageResponse<com.medifind.dto.pharmacy.PharmacyDto>>> getPendingPharmacies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(pharmacyService.getPharmaciesByStatus(com.medifind.enums.VerificationStatus.PENDING, page, size)));
    }

    @PutMapping("/pharmacies/{id}/verify")
    public ResponseEntity<ApiResponse<String>> verifyPharmacy(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Object> body) {
        String status = body.getOrDefault("status", "VERIFIED").toString();
        pharmacyService.verifyPharmacy(id, com.medifind.enums.VerificationStatus.valueOf(status));
        return ResponseEntity.ok(ApiResponse.success("Pharmacy verification updated", "SUCCESS"));
    }

    @GetMapping("/medicines")
    public ResponseEntity<ApiResponse<PageResponse<com.medifind.dto.medicine.MedicineDto>>> getAdminMedicines(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(medicineService.getAllMedicines(page, size, "name")));
    }

    @GetMapping("/inventory")
    public ResponseEntity<ApiResponse<?>> getAdminInventory(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getRecentUpdates(limit)));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<Object>>> getAdminReports() {
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<List<Object>>> getAdminAuditLogs() {
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<List<Object>>> getAdminNotifications() {
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }
}

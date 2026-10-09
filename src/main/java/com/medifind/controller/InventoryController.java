package com.medifind.controller;

import com.medifind.dto.ApiResponse;
import com.medifind.dto.PageResponse;
import com.medifind.dto.inventory.InventoryDto;
import com.medifind.dto.inventory.InventoryUpsertRequest;
import com.medifind.dto.inventory.InventoryUpdateRequest;
import com.medifind.repository.PharmacyRepository;
import com.medifind.repository.UserRepository;
import com.medifind.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final PharmacyRepository pharmacyRepository;
    private final UserRepository userRepository;
    private final com.medifind.repository.InventoryRepository inventoryRepository;

    /** Pharmacy portal or public: get inventory */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getInventory(
            @RequestParam(required = false) Long pharmacyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long targetPharmacyId = pharmacyId;
        if (targetPharmacyId == null && userDetails != null) {
            targetPharmacyId = pharmacyRepository.findByOwnerId(
                    userRepository.findByEmail(userDetails.getUsername()).map(com.medifind.entity.User::getId).orElse(0L)
            ).map(com.medifind.entity.Pharmacy::getId).orElse(null);
        }
        if (targetPharmacyId == null) {
            // Default to first pharmacy if none specified
            targetPharmacyId = pharmacyRepository.findAll().stream().findFirst().map(com.medifind.entity.Pharmacy::getId).orElse(1L);
        }
        return ResponseEntity.ok(ApiResponse.success(
                inventoryService.getPharmacyInventory(targetPharmacyId, page, size)));
    }

    /** Public: get all inventory for a specific pharmacy */
    @GetMapping("/pharmacy/{pharmacyId}")
    public ResponseEntity<ApiResponse<PageResponse<InventoryDto>>> getPharmacyInventory(
            @PathVariable Long pharmacyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                inventoryService.getPharmacyInventory(pharmacyId, page, size)));
    }

    /** Get single inventory item */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryDto>> getInventoryById(@PathVariable Long id) {
        return inventoryRepository.findById(id)
                .map(inv -> ResponseEntity.ok(ApiResponse.success(inventoryService.toDto(inv))))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Pharmacy: add or update stock for a medicine */
    @PostMapping("/pharmacy/{pharmacyId}")
    @PreAuthorize("hasAnyRole('PHARMACY', 'ADMIN')")
    public ResponseEntity<ApiResponse<InventoryDto>> upsertInventory(
            @PathVariable Long pharmacyId,
            @Valid @RequestBody InventoryUpsertRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                "Inventory updated", inventoryService.upsertInventory(
                    resolvePharmacyId(pharmacyId, userDetails), request)));
    }

    /** General create/upsert inventory */
    @PostMapping
    @PreAuthorize("hasAnyRole('PHARMACY', 'ADMIN')")
    public ResponseEntity<ApiResponse<InventoryDto>> addInventory(
            @Valid @RequestBody InventoryUpsertRequest request,
            @RequestParam(required = false) Long pharmacyId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long targetPharmacyId = resolvePharmacyId(pharmacyId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                "Inventory added", inventoryService.upsertInventory(targetPharmacyId, request)));
    }

    /** Update stock quantity */
    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('PHARMACY', 'ADMIN')")
    public ResponseEntity<ApiResponse<InventoryDto>> updateStock(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        com.medifind.entity.Inventory inv = inventoryRepository.findById(id)
                .orElseThrow(() -> new com.medifind.exception.ResourceNotFoundException("Inventory", id));
        assertPharmacyAccess(inv.getPharmacy().getId(), userDetails);
        Object quantity = body.containsKey("quantity") ? body.get("quantity") : body.get("stockQuantity");
        if (quantity == null) {
            throw new com.medifind.exception.BadRequestException("Stock quantity is required.");
        }
        return ResponseEntity.ok(ApiResponse.success("Stock updated",
                inventoryService.setStockQuantity(id, Integer.parseInt(quantity.toString()))));
    }

    /** Update inventory item */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PHARMACY', 'ADMIN')")
    public ResponseEntity<ApiResponse<InventoryDto>> updateInventoryItem(
            @PathVariable Long id,
            @Valid @RequestBody InventoryUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        com.medifind.entity.Inventory inv = inventoryRepository.findById(id)
                .orElseThrow(() -> new com.medifind.exception.ResourceNotFoundException("Inventory", id));
        assertPharmacyAccess(inv.getPharmacy().getId(), userDetails);
        return ResponseEntity.ok(ApiResponse.success("Inventory updated", inventoryService.updateInventory(id, request)));
    }

    /** Low stock items */
    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('PHARMACY', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<InventoryDto>>> getLowStock(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getByStatus(
                resolvePharmacyId(null, userDetails), com.medifind.enums.AvailabilityStatus.LOW_STOCK)));
    }

    /** Out of stock items */
    @GetMapping("/out-of-stock")
    @PreAuthorize("hasAnyRole('PHARMACY', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<InventoryDto>>> getOutOfStock(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getByStatus(
                resolvePharmacyId(null, userDetails), com.medifind.enums.AvailabilityStatus.OUT_OF_STOCK)));
    }

    @GetMapping("/expiry-watch")
    @PreAuthorize("hasAnyRole('PHARMACY', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<InventoryDto>>> getExpiryWatch(
            @RequestParam(defaultValue = "90") int days,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                inventoryService.getExpiryWatch(resolvePharmacyId(null, userDetails), days)));
    }

    /** Pharmacy: remove a medicine from inventory */
    @DeleteMapping("/pharmacy/{pharmacyId}/item/{inventoryId}")
    @PreAuthorize("hasAnyRole('PHARMACY', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteInventory(
            @PathVariable Long pharmacyId,
            @PathVariable Long inventoryId,
            @AuthenticationPrincipal UserDetails userDetails) {
        assertPharmacyAccess(pharmacyId, userDetails);
        inventoryService.deleteInventory(pharmacyId, inventoryId);
        return ResponseEntity.ok(ApiResponse.success("Inventory item removed", null));
    }

    /** Direct delete inventory item */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PHARMACY', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteInventoryItem(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        com.medifind.entity.Inventory inventory = inventoryRepository.findById(id)
            .orElseThrow(() -> new com.medifind.exception.ResourceNotFoundException("Inventory", id));
        assertPharmacyAccess(inventory.getPharmacy().getId(), userDetails);
        inventoryService.deleteInventory(inventory.getPharmacy().getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Inventory item removed", null));
    }

    /** Public: search medicine availability across all pharmacies */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<?>> searchAvailability(
            @RequestParam(required = false, defaultValue = "") String medicine,
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(defaultValue = "false") boolean verifiedOnly) {
        String query = !medicine.isBlank() ? medicine : q;
        return ResponseEntity.ok(ApiResponse.success(
                inventoryService.searchAvailability(query, verifiedOnly)));
    }

    /** Admin/Dashboard: recent inventory updates */
    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> getRecentUpdates(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getRecentUpdates(limit)));
    }

    private Long resolvePharmacyId(Long requestedPharmacyId, UserDetails userDetails) {
        boolean admin = userDetails.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (admin) {
            if (requestedPharmacyId != null) return requestedPharmacyId;
            return pharmacyRepository.findAll().stream().findFirst()
                    .map(com.medifind.entity.Pharmacy::getId)
                    .orElseThrow(() -> new com.medifind.exception.ResourceNotFoundException("Pharmacy not found"));
        }
        Long userId = userRepository.findByEmail(userDetails.getUsername())
                .map(com.medifind.entity.User::getId)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user could not be found."));
        Long ownedPharmacyId = pharmacyRepository.findByOwnerId(userId)
                .map(com.medifind.entity.Pharmacy::getId)
                .orElseThrow(() -> new AccessDeniedException("No pharmacy is linked to this account."));
        if (requestedPharmacyId != null && !requestedPharmacyId.equals(ownedPharmacyId)) {
            throw new AccessDeniedException("You can only manage your own pharmacy inventory.");
        }
        return ownedPharmacyId;
    }

    private void assertPharmacyAccess(Long pharmacyId, UserDetails userDetails) {
        resolvePharmacyId(pharmacyId, userDetails);
    }
}

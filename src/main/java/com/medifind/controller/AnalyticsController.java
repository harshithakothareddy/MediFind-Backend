package com.medifind.controller;

import com.medifind.dto.ApiResponse;
import com.medifind.dto.admin.AdminDashboardStats;
import com.medifind.enums.AvailabilityStatus;
import com.medifind.repository.InventoryRepository;
import com.medifind.repository.MedicineRepository;
import com.medifind.repository.PharmacyRepository;
import com.medifind.repository.UserRepository;
import com.medifind.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AdminService adminService;
    private final PharmacyRepository pharmacyRepository;
    private final MedicineRepository medicineRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;

    @GetMapping("/platform/stats")
    public ResponseEntity<ApiResponse<AdminDashboardStats>> getPlatformStats() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDashboardStats()));
    }

    @GetMapping("/pharmacy/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPharmacyStats(
            @RequestParam(required = false) Long pharmacyId) {
        long totalItems = inventoryRepository.count();
        long inStock = inventoryRepository.countByAvailabilityStatus(AvailabilityStatus.IN_STOCK);
        long lowStock = inventoryRepository.countByAvailabilityStatus(AvailabilityStatus.LOW_STOCK);
        long outOfStock = inventoryRepository.countByAvailabilityStatus(AvailabilityStatus.OUT_OF_STOCK);

        Map<String, Object> stats = Map.of(
                "totalMedicines", totalItems,
                "inStockMedicines", inStock,
                "lowStockMedicines", lowStock,
                "outOfStockMedicines", outOfStock,
                "totalSearchesToday", 142,
                "customerVisits", 88
        );
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/pharmacy/inventory")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPharmacyInventoryAnalytics() {
        long inStock = inventoryRepository.countByAvailabilityStatus(AvailabilityStatus.IN_STOCK);
        long lowStock = inventoryRepository.countByAvailabilityStatus(AvailabilityStatus.LOW_STOCK);
        long outOfStock = inventoryRepository.countByAvailabilityStatus(AvailabilityStatus.OUT_OF_STOCK);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "inStock", inStock,
                "lowStock", lowStock,
                "outOfStock", outOfStock
        )));
    }

    @GetMapping("/pharmacy/stock-trend")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStockTrend() {
        return ResponseEntity.ok(ApiResponse.success(List.of(
                Map.of("day", "Mon", "stockLevel", 95),
                Map.of("day", "Tue", "stockLevel", 92),
                Map.of("day", "Wed", "stockLevel", 88),
                Map.of("day", "Thu", "stockLevel", 94),
                Map.of("day", "Fri", "stockLevel", 90),
                Map.of("day", "Sat", "stockLevel", 96),
                Map.of("day", "Sun", "stockLevel", 93)
        )));
    }

    @GetMapping("/platform/user-growth")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserGrowth() {
        return ResponseEntity.ok(ApiResponse.success(List.of(
                Map.of("month", "Jan", "users", 420),
                Map.of("month", "Feb", "users", 680),
                Map.of("month", "Mar", "users", 950),
                Map.of("month", "Apr", "users", 1240)
        )));
    }

    @GetMapping("/pharmacy/searches")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPharmacySearches() {
        return ResponseEntity.ok(ApiResponse.success(List.of(
                Map.of("term", "Paracetamol", "count", 48),
                Map.of("term", "Azithromycin", "count", 35),
                Map.of("term", "Metformin", "count", 28),
                Map.of("term", "Amoxicillin", "count", 22),
                Map.of("term", "Omeprazole", "count", 18)
        )));
    }

    @GetMapping("/platform/searches")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPlatformSearches() {
        return ResponseEntity.ok(ApiResponse.success(List.of(
                Map.of("term", "Paracetamol", "searches", 1842, "availability", "92%"),
                Map.of("term", "Azithromycin", "searches", 1245, "availability", "78%"),
                Map.of("term", "Metformin", "searches", 1028, "availability", "95%"),
                Map.of("term", "Amoxicillin", "searches", 892, "availability", "86%"),
                Map.of("term", "Omeprazole", "searches", 734, "availability", "90%")
        )));
    }

    @GetMapping("/platform/pharmacy-growth")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPharmacyGrowth() {
        return ResponseEntity.ok(ApiResponse.success(List.of(
                Map.of("month", "Jan", "pharmacies", 8),
                Map.of("month", "Feb", "pharmacies", 12),
                Map.of("month", "Mar", "pharmacies", 18),
                Map.of("month", "Apr", "pharmacies", 25)
        )));
    }

    @GetMapping("/platform/alerts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAlertActivity() {
        return ResponseEntity.ok(ApiResponse.success(List.of(
                Map.of("day", "Mon", "triggered", 12, "resolved", 10),
                Map.of("day", "Tue", "triggered", 8, "resolved", 8),
                Map.of("day", "Wed", "triggered", 15, "resolved", 12),
                Map.of("day", "Thu", "triggered", 10, "resolved", 9),
                Map.of("day", "Fri", "triggered", 18, "resolved", 14)
        )));
    }

    @GetMapping("/pharmacy/activity")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentActivity() {
        return ResponseEntity.ok(ApiResponse.success(List.of(
                Map.of("type", "STOCK_UPDATE", "message", "Paracetamol stock updated to 150", "time", "2 min ago"),
                Map.of("type", "LOW_STOCK", "message", "Azithromycin running low (5 units)", "time", "15 min ago"),
                Map.of("type", "SEARCH", "message", "High search volume: Metformin", "time", "32 min ago")
        )));
    }

    @GetMapping("/platform/availability")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAvailabilityOverview() {
        long inStock = inventoryRepository.countByAvailabilityStatus(AvailabilityStatus.IN_STOCK);
        long lowStock = inventoryRepository.countByAvailabilityStatus(AvailabilityStatus.LOW_STOCK);
        long outOfStock = inventoryRepository.countByAvailabilityStatus(AvailabilityStatus.OUT_OF_STOCK);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "inStock", inStock,
                "lowStock", lowStock,
                "outOfStock", outOfStock,
                "overallAvailabilityRate", "92.4%"
        )));
    }
}

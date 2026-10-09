package com.medifind.dto.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminDashboardStats {
    private long totalUsers;
    private long totalPharmacies;
    private long verifiedPharmacies;
    private long pendingVerifications;
    private long totalMedicines;
    private long totalInventoryEntries;
    private long inStockItems;
    private long lowStockItems;
    private long outOfStockItems;
}

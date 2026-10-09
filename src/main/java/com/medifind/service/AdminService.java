package com.medifind.service;

import com.medifind.dto.admin.AdminDashboardStats;
import com.medifind.enums.AvailabilityStatus;
import com.medifind.enums.Role;
import com.medifind.enums.VerificationStatus;
import com.medifind.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PharmacyRepository pharmacyRepository;
    private final MedicineRepository medicineRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public AdminDashboardStats getDashboardStats() {
        return AdminDashboardStats.builder()
                .totalUsers(userRepository.countByRole(Role.USER))
                .totalPharmacies(pharmacyRepository.count())
                .verifiedPharmacies(pharmacyRepository.countByVerifiedTrue())
                .pendingVerifications(pharmacyRepository.countByVerificationStatus(VerificationStatus.PENDING))
                .totalMedicines(medicineRepository.countByActiveTrue())
                .totalInventoryEntries(inventoryRepository.count())
                .inStockItems(inventoryRepository.countByAvailabilityStatus(AvailabilityStatus.IN_STOCK))
                .lowStockItems(inventoryRepository.countByAvailabilityStatus(AvailabilityStatus.LOW_STOCK))
                .outOfStockItems(inventoryRepository.countByAvailabilityStatus(AvailabilityStatus.OUT_OF_STOCK))
                .build();
    }
}

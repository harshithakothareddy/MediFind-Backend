package com.medifind.service;

import com.medifind.dto.inventory.InventoryUpsertRequest;
import com.medifind.entity.Inventory;
import com.medifind.entity.AvailabilityAlert;
import com.medifind.entity.Medicine;
import com.medifind.entity.Notification;
import com.medifind.entity.Pharmacy;
import com.medifind.exception.ResourceNotFoundException;
import com.medifind.repository.AlertRepository;
import com.medifind.repository.InventoryRepository;
import com.medifind.repository.MedicineRepository;
import com.medifind.repository.NotificationRepository;
import com.medifind.repository.PharmacyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private PharmacyRepository pharmacyRepository;
    @Mock private MedicineRepository medicineRepository;
    @Mock private AlertRepository alertRepository;
    @Mock private NotificationRepository notificationRepository;
    @InjectMocks private InventoryService inventoryService;

    @Test
    void upsertPersistsStockPriceAndThreshold() {
        Pharmacy pharmacy = pharmacy();
        Medicine medicine = medicine();
        when(pharmacyRepository.findById(3L)).thenReturn(Optional.of(pharmacy));
        when(medicineRepository.findById(8L)).thenReturn(Optional.of(medicine));
        when(inventoryRepository.findByPharmacyIdAndMedicineId(3L, 8L)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        InventoryUpsertRequest request = new InventoryUpsertRequest();
        request.setMedicineId(8L);
        request.setStockQuantity(25);
        request.setMinimumStockLevel(5);
        request.setPrice(new BigDecimal("42.50"));

        var response = inventoryService.upsertInventory(3L, request);

        assertEquals(25, response.getStockQuantity());
        assertEquals(new BigDecimal("42.50"), response.getPrice());
        assertEquals("Paracetamol", response.getMedicine().getName());
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void restockingOutOfStockMedicineNotifiesAndClosesActiveAlerts() {
        Pharmacy pharmacy = pharmacy();
        Medicine medicine = medicine();
        Inventory inventory = Inventory.builder().id(10L).pharmacy(pharmacy).medicine(medicine)
                .stockQuantity(0).price(new BigDecimal("42.50")).build();
        AvailabilityAlert alert = mock(AvailabilityAlert.class);
        when(pharmacyRepository.findById(3L)).thenReturn(Optional.of(pharmacy));
        when(medicineRepository.findById(8L)).thenReturn(Optional.of(medicine));
        when(inventoryRepository.findByPharmacyIdAndMedicineId(3L, 8L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.findByMedicineIdAndActiveTrue(8L)).thenReturn(java.util.List.of(alert));

        InventoryUpsertRequest request = new InventoryUpsertRequest();
        request.setMedicineId(8L);
        request.setStockQuantity(25);
        request.setMinimumStockLevel(5);
        request.setPrice(new BigDecimal("42.50"));

        inventoryService.upsertInventory(3L, request);

        verify(notificationRepository).save(any(Notification.class));
        verify(alert).setActive(false);
        verify(alert).setTriggeredAt(any(java.time.LocalDateTime.class));
        verify(alertRepository).saveAll(java.util.List.of(alert));
    }

    @Test
    void firstInStockRecordNotifiesExistingMedicineWatchers() {
        Pharmacy pharmacy = pharmacy();
        Medicine medicine = medicine();
        AvailabilityAlert alert = mock(AvailabilityAlert.class);
        when(pharmacyRepository.findById(3L)).thenReturn(Optional.of(pharmacy));
        when(medicineRepository.findById(8L)).thenReturn(Optional.of(medicine));
        when(inventoryRepository.findByPharmacyIdAndMedicineId(3L, 8L)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> {
            Inventory saved = invocation.getArgument(0);
            saved.setId(20L);
            return saved;
        });
        when(alertRepository.findByMedicineIdAndActiveTrue(8L)).thenReturn(java.util.List.of(alert));
        InventoryUpsertRequest request = new InventoryUpsertRequest();
        request.setMedicineId(8L);
        request.setStockQuantity(12);
        request.setMinimumStockLevel(5);
        request.setPrice(new BigDecimal("42.50"));

        inventoryService.upsertInventory(3L, request);

        verify(notificationRepository).save(any(Notification.class));
        verify(alert).setActive(false);
    }

    @Test
    void deleteRejectsInventoryOwnedByAnotherPharmacy() {
        Pharmacy owningPharmacy = pharmacy();
        Inventory inventory = Inventory.builder().id(4L).pharmacy(owningPharmacy).medicine(medicine()).build();
        when(inventoryRepository.findById(4L)).thenReturn(Optional.of(inventory));

        assertThrows(ResourceNotFoundException.class, () -> inventoryService.deleteInventory(99L, 4L));
        verify(inventoryRepository, never()).delete(any(Inventory.class));
    }

    @Test
    void deleteRemovesInventoryForOwningPharmacy() {
        Pharmacy owningPharmacy = pharmacy();
        Inventory inventory = Inventory.builder().id(4L).pharmacy(owningPharmacy).medicine(medicine()).build();
        when(inventoryRepository.findById(4L)).thenReturn(Optional.of(inventory));

        inventoryService.deleteInventory(3L, 4L);

        verify(inventoryRepository).delete(inventory);
    }

    private Pharmacy pharmacy() {
        return Pharmacy.builder().id(3L).name("Test Pharmacy").licenseNumber("LIC-3")
                .phone("1234567890").address("Main Road").area("Madhapur").city("Hyderabad")
                .state("Telangana").pincode("500081").latitude(17.44).longitude(78.39).build();
    }

    private Medicine medicine() {
        return Medicine.builder().id(8L).name("Paracetamol").genericName("Acetaminophen")
                .manufacturer("Test Pharma").category("Analgesic").dosageForm("Tablet")
                .strength("500mg").active(true).build();
    }
}

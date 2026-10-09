package com.medifind.service;

import com.medifind.dto.reservation.ReservationCreateRequest;
import com.medifind.entity.Inventory;
import com.medifind.entity.Medicine;
import com.medifind.entity.Pharmacy;
import com.medifind.entity.User;
import com.medifind.enums.ReservationStatus;
import com.medifind.exception.BadRequestException;
import com.medifind.repository.InventoryRepository;
import com.medifind.repository.NotificationRepository;
import com.medifind.repository.PharmacyRepository;
import com.medifind.repository.ReservationRepository;
import com.medifind.repository.UserRepository;
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
class ReservationServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private PharmacyRepository pharmacyRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private InventoryService inventoryService;
    @InjectMocks private ReservationService reservationService;

    @Test
    void createReservesAvailableQuantityAndReturnsPickupCode() {
        User user = User.builder().id(4L).firstName("Test").lastName("Patient").email("patient@example.com").build();
        Pharmacy pharmacy = Pharmacy.builder().id(3L).name("Verified Pharmacy").verified(true).build();
        Medicine medicine = Medicine.builder().id(8L).name("Paracetamol").genericName("Acetaminophen")
                .strength("500mg").dosageForm("Tablet").build();
        Inventory inventory = Inventory.builder().id(12L).pharmacy(pharmacy).medicine(medicine)
                .stockQuantity(10).price(new BigDecimal("25.00")).build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(inventoryRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(inventory));
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ReservationCreateRequest request = new ReservationCreateRequest();
        request.setInventoryId(12L);
        request.setQuantity(3);

        var result = reservationService.create(user.getEmail(), request);

        assertEquals("Paracetamol", result.getMedicineName());
        assertEquals(ReservationStatus.CONFIRMED, result.getStatus());
        assertEquals(new BigDecimal("75.00"), result.getTotalPrice());
        assertTrue(result.getReservationCode().startsWith("MF-"));
        verify(inventoryService).setStockQuantity(12L, 7);
    }

    @Test
    void createRejectsQuantityAboveAvailableStock() {
        User user = User.builder().id(4L).firstName("Test").lastName("Patient").email("patient@example.com").build();
        Pharmacy pharmacy = Pharmacy.builder().id(3L).name("Verified Pharmacy").verified(true).build();
        Inventory inventory = Inventory.builder().id(12L).pharmacy(pharmacy).medicine(Medicine.builder().id(8L).name("Medicine").build())
                .stockQuantity(1).price(new BigDecimal("25.00")).build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(inventoryRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(inventory));
        ReservationCreateRequest request = new ReservationCreateRequest();
        request.setInventoryId(12L);
        request.setQuantity(2);

        assertThrows(BadRequestException.class, () -> reservationService.create(user.getEmail(), request));
        verify(reservationRepository, never()).save(any());
        verify(inventoryService, never()).setStockQuantity(anyLong(), anyInt());
    }
}
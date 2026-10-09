package com.medifind.service;

import com.medifind.dto.review.PharmacyReviewRequest;
import com.medifind.entity.Inventory;
import com.medifind.entity.Medicine;
import com.medifind.entity.MedicineReservation;
import com.medifind.entity.Pharmacy;
import com.medifind.entity.PharmacyReview;
import com.medifind.entity.User;
import com.medifind.enums.ReservationStatus;
import com.medifind.exception.BadRequestException;
import com.medifind.repository.PharmacyRepository;
import com.medifind.repository.PharmacyReviewRepository;
import com.medifind.repository.ReservationRepository;
import com.medifind.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PharmacyReviewServiceTest {

    @Mock private PharmacyReviewRepository reviewRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private PharmacyRepository pharmacyRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private PharmacyReviewService reviewService;

    @Test
    void completedPickupCanBeReviewedAndRecalculatesPharmacyRating() {
        User user = User.builder().id(7L).firstName("Riya").lastName("Patient").email("riya@example.com").build();
        Pharmacy pharmacy = Pharmacy.builder().id(3L).name("Local Pharmacy").build();
        Inventory inventory = Inventory.builder().id(15L).pharmacy(pharmacy)
                .medicine(Medicine.builder().id(9L).name("Medicine").build()).build();
        MedicineReservation reservation = MedicineReservation.builder().id(41L).user(user).inventory(inventory)
                .status(ReservationStatus.PICKED_UP).build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(pharmacyRepository.findById(3L)).thenReturn(Optional.of(pharmacy));
        when(reservationRepository.findByIdAndUserId(41L, 7L)).thenReturn(Optional.of(reservation));
        when(reviewRepository.findByReservationId(41L)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(PharmacyReview.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewRepository.averageRatingByPharmacyId(3L)).thenReturn(4.5);
        when(reviewRepository.countByPharmacyId(3L)).thenReturn(2L);
        PharmacyReviewRequest request = new PharmacyReviewRequest();
        request.setReservationId(41L);
        request.setRating(5);
        request.setComment("Quick pickup.");

        var response = reviewService.submit(user.getEmail(), pharmacy.getId(), request);

        assertEquals("Riya", response.getCustomerName());
        assertEquals(5, response.getRating());
        assertEquals(4.5, pharmacy.getRating());
        assertEquals(2, pharmacy.getTotalReviews());
        verify(pharmacyRepository).save(pharmacy);
    }

    @Test
    void reservationMustBePickedUpAtTheReviewedPharmacy() {
        User user = User.builder().id(7L).firstName("Riya").lastName("Patient").email("riya@example.com").build();
        Pharmacy pharmacy = Pharmacy.builder().id(3L).name("Local Pharmacy").build();
        MedicineReservation reservation = MedicineReservation.builder().id(41L).user(user)
                .inventory(Inventory.builder().id(15L).pharmacy(pharmacy).medicine(Medicine.builder().id(9L).name("Medicine").build()).build())
                .status(ReservationStatus.CONFIRMED).build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(pharmacyRepository.findById(3L)).thenReturn(Optional.of(pharmacy));
        when(reservationRepository.findByIdAndUserId(41L, 7L)).thenReturn(Optional.of(reservation));
        PharmacyReviewRequest request = new PharmacyReviewRequest();
        request.setReservationId(41L);
        request.setRating(4);

        assertThrows(BadRequestException.class, () -> reviewService.submit(user.getEmail(), 3L, request));
        verify(reviewRepository, never()).save(any(PharmacyReview.class));
    }
}
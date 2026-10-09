package com.medifind.service;

import com.medifind.dto.review.PharmacyReviewDto;
import com.medifind.dto.review.PharmacyReviewRequest;
import com.medifind.entity.MedicineReservation;
import com.medifind.entity.Pharmacy;
import com.medifind.entity.PharmacyReview;
import com.medifind.entity.User;
import com.medifind.enums.ReservationStatus;
import com.medifind.exception.BadRequestException;
import com.medifind.exception.ResourceNotFoundException;
import com.medifind.repository.PharmacyRepository;
import com.medifind.repository.PharmacyReviewRepository;
import com.medifind.repository.ReservationRepository;
import com.medifind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PharmacyReviewService {

    private final PharmacyReviewRepository reviewRepository;
    private final ReservationRepository reservationRepository;
    private final PharmacyRepository pharmacyRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<PharmacyReviewDto> getReviews(Long pharmacyId) {
        if (!pharmacyRepository.existsById(pharmacyId)) throw new ResourceNotFoundException("Pharmacy", pharmacyId);
        return reviewRepository.findByPharmacyIdOrderByCreatedAtDesc(pharmacyId).stream().map(this::toDto).toList();
    }

    @Transactional
    public PharmacyReviewDto submit(String email, Long pharmacyId, PharmacyReviewRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", pharmacyId));
        MedicineReservation reservation = reservationRepository.findByIdAndUserId(request.getReservationId(), user.getId())
                .orElseThrow(() -> new BadRequestException("A completed reservation at this pharmacy is required to leave a review."));
        if (reservation.getStatus() != ReservationStatus.PICKED_UP
                || !reservation.getInventory().getPharmacy().getId().equals(pharmacyId)) {
            throw new BadRequestException("Only a completed pickup at this pharmacy can be reviewed.");
        }
        if (reviewRepository.findByReservationId(reservation.getId()).isPresent()) {
            throw new BadRequestException("This reservation has already been reviewed.");
        }

        PharmacyReview review = reviewRepository.save(PharmacyReview.builder()
                .pharmacy(pharmacy)
                .user(user)
                .reservation(reservation)
                .rating(request.getRating())
                .comment(request.getComment() == null || request.getComment().isBlank() ? null : request.getComment().trim())
                .build());
        pharmacy.setRating(reviewRepository.averageRatingByPharmacyId(pharmacyId));
        pharmacy.setTotalReviews(Math.toIntExact(reviewRepository.countByPharmacyId(pharmacyId)));
        pharmacyRepository.save(pharmacy);
        return toDto(review);
    }

    private PharmacyReviewDto toDto(PharmacyReview review) {
        return PharmacyReviewDto.builder()
                .id(review.getId())
                .pharmacyId(review.getPharmacy().getId())
                .customerName(review.getUser().getFirstName())
                .reservationId(review.getReservation().getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
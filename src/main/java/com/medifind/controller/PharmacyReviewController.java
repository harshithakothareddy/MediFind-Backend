package com.medifind.controller;

import com.medifind.dto.ApiResponse;
import com.medifind.dto.review.PharmacyReviewDto;
import com.medifind.dto.review.PharmacyReviewRequest;
import com.medifind.service.PharmacyReviewService;
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
@RequestMapping("/pharmacies/{pharmacyId}/reviews")
@RequiredArgsConstructor
public class PharmacyReviewController {

    private final PharmacyReviewService reviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PharmacyReviewDto>>> getReviews(@PathVariable Long pharmacyId) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getReviews(pharmacyId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<PharmacyReviewDto>> submit(
            @PathVariable Long pharmacyId,
            @Valid @RequestBody PharmacyReviewRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Review submitted",
                reviewService.submit(user.getUsername(), pharmacyId, request)));
    }
}
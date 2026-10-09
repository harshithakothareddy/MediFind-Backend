package com.medifind.service;

import com.medifind.dto.PageResponse;
import com.medifind.dto.pharmacy.PharmacyCreateRequest;
import com.medifind.dto.pharmacy.PharmacyDto;
import com.medifind.dto.pharmacy.PharmacyHoursDto;
import com.medifind.entity.Pharmacy;
import com.medifind.entity.PharmacyHours;
import com.medifind.entity.User;
import com.medifind.enums.VerificationStatus;
import com.medifind.exception.BadRequestException;
import com.medifind.exception.ResourceNotFoundException;
import com.medifind.repository.PharmacyRepository;
import com.medifind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PharmacyService {

    private final PharmacyRepository pharmacyRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponse<PharmacyDto> getAllPharmacies(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return toPageResponse(pharmacyRepository.findAll(pageable));
    }

    @Transactional(readOnly = true)
    public PharmacyDto getPharmacyById(Long id) {
        return toDto(findById(id), null);
    }

    @Transactional(readOnly = true)
    public PharmacyDto getPharmacyByOwner(Long ownerId) {
        Pharmacy p = pharmacyRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy not found for owner: " + ownerId));
        return toDto(p, null);
    }

    @Transactional(readOnly = true)
    public List<PharmacyDto> getNearbyPharmacies(double lat, double lng, double radiusKm) {
        return pharmacyRepository.findNearbyPharmacies(lat, lng, radiusKm)
                .stream()
                .map(p -> toDto(p, null))
                .toList();
    }

    @Transactional
    public PharmacyDto createPharmacy(PharmacyCreateRequest req, Long ownerId) {
        if (pharmacyRepository.findByOwnerId(ownerId).isPresent()) {
            throw new BadRequestException("A pharmacy is already registered under your account.");
        }
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerId));

        Pharmacy pharmacy = Pharmacy.builder()
                .owner(owner)
                .name(req.getName())
                .licenseNumber(req.getLicenseNumber())
                .ownerName(req.getOwnerName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .address(req.getAddress())
                .area(req.getArea())
                .city(req.getCity())
                .state(req.getState())
                .pincode(req.getPincode())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .description(req.getDescription())
                .open24Hours(req.getOpen24Hours() != null ? req.getOpen24Hours() : false)
                .verified(false)
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        return toDto(pharmacyRepository.save(pharmacy), null);
    }

    @Transactional
    public PharmacyDto verifyPharmacy(Long id, VerificationStatus status) {
        Pharmacy pharmacy = findById(id);
        pharmacy.setVerificationStatus(status);
        pharmacy.setVerified(status == VerificationStatus.VERIFIED);
        return toDto(pharmacyRepository.save(pharmacy), null);
    }

    @Transactional(readOnly = true)
    public PageResponse<PharmacyDto> getPharmaciesByStatus(VerificationStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return toPageResponse(pharmacyRepository.findByVerificationStatus(status, pageable));
    }

    private Pharmacy findById(Long id) {
        return pharmacyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", id));
    }

    public static PharmacyDto toDto(Pharmacy p, Double distanceKm) {
        List<PharmacyHoursDto> hours = p.getOperatingHours() == null ? List.of() :
                p.getOperatingHours().stream().map(h -> PharmacyHoursDto.builder()
                        .dayOfWeek(h.getDayOfWeek() != null ? h.getDayOfWeek().name() : null)
                        .openTime(h.getOpeningTime() != null ? h.getOpeningTime().toString() : null)
                        .closeTime(h.getClosingTime() != null ? h.getClosingTime().toString() : null)
                        .closed(h.getClosed())
                        .build()).toList();

        return PharmacyDto.builder()
                .id(p.getId())
                .name(p.getName())
                .licenseNumber(p.getLicenseNumber())
                .ownerName(p.getOwnerName())
                .email(p.getEmail())
                .phone(p.getPhone())
                .address(p.getAddress())
                .area(p.getArea())
                .city(p.getCity())
                .state(p.getState())
                .pincode(p.getPincode())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .description(p.getDescription())
                .rating(p.getRating())
                .totalReviews(p.getTotalReviews())
                .verified(p.getVerified())
                .verificationStatus(p.getVerificationStatus())
                .open24Hours(p.getOpen24Hours())
                .dataSource(p.getDataSource())
                .dataType(p.getDataType())
                .operatingHours(hours)
                .distanceKm(distanceKm)
                .createdAt(p.getCreatedAt())
                .build();
    }

    private PageResponse<PharmacyDto> toPageResponse(Page<Pharmacy> page) {
        return PageResponse.<PharmacyDto>builder()
                .content(page.getContent().stream().map(p -> toDto(p, null)).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}

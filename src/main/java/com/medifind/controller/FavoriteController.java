package com.medifind.controller;

import com.medifind.dto.ApiResponse;
import com.medifind.dto.medicine.MedicineDto;
import com.medifind.dto.pharmacy.PharmacyDto;
import com.medifind.entity.FavoriteMedicine;
import com.medifind.entity.FavoritePharmacy;
import com.medifind.entity.Medicine;
import com.medifind.entity.Pharmacy;
import com.medifind.entity.User;
import com.medifind.repository.FavoriteMedicineRepository;
import com.medifind.repository.FavoritePharmacyRepository;
import com.medifind.repository.MedicineRepository;
import com.medifind.repository.PharmacyRepository;
import com.medifind.repository.UserRepository;
import com.medifind.service.MedicineService;
import com.medifind.service.PharmacyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteMedicineRepository favoriteMedicineRepository;
    private final FavoritePharmacyRepository favoritePharmacyRepository;
    private final MedicineRepository medicineRepository;
    private final PharmacyRepository pharmacyRepository;
    private final UserRepository userRepository;

    private Long getUserId(UserDetails userDetails) {
        if (userDetails == null) return 1L;
        return userRepository.findByEmail(userDetails.getUsername())
                .map(User::getId)
                .orElse(1L);
    }

    @GetMapping("/medicines")
    public ResponseEntity<ApiResponse<List<MedicineDto>>> getMedicineFavorites(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        List<MedicineDto> list = favoriteMedicineRepository.findByUserId(userId, PageRequest.of(0, 50))
                .getContent().stream()
                .map(fav -> MedicineService.toDto(fav.getMedicine()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/medicines")
    @Transactional
    public ResponseEntity<ApiResponse<String>> addMedicineFavorite(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        Long medicineId = Long.parseLong(body.get("medicineId").toString());
        if (!favoriteMedicineRepository.existsByUserIdAndMedicineId(userId, medicineId)) {
            User user = userRepository.findById(userId).orElseThrow();
            Medicine medicine = medicineRepository.findById(medicineId).orElseThrow();
            favoriteMedicineRepository.save(FavoriteMedicine.builder().user(user).medicine(medicine).build());
        }
        return ResponseEntity.ok(ApiResponse.success("Added to favorites", "SUCCESS"));
    }

    @DeleteMapping("/medicines/{medicineId}")
    @Transactional
    public ResponseEntity<ApiResponse<String>> removeMedicineFavorite(
            @PathVariable Long medicineId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        favoriteMedicineRepository.deleteByUserIdAndMedicineId(userId, medicineId);
        return ResponseEntity.ok(ApiResponse.success("Removed from favorites", "SUCCESS"));
    }

    @GetMapping("/pharmacies")
    public ResponseEntity<ApiResponse<List<PharmacyDto>>> getPharmacyFavorites(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        List<PharmacyDto> list = favoritePharmacyRepository.findByUserId(userId, PageRequest.of(0, 50))
                .getContent().stream()
                .map(fav -> PharmacyService.toDto(fav.getPharmacy(), null))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/pharmacies")
    @Transactional
    public ResponseEntity<ApiResponse<String>> addPharmacyFavorite(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        Long pharmacyId = Long.parseLong(body.get("pharmacyId").toString());
        if (!favoritePharmacyRepository.existsByUserIdAndPharmacyId(userId, pharmacyId)) {
            User user = userRepository.findById(userId).orElseThrow();
            Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId).orElseThrow();
            favoritePharmacyRepository.save(FavoritePharmacy.builder().user(user).pharmacy(pharmacy).build());
        }
        return ResponseEntity.ok(ApiResponse.success("Added to favorites", "SUCCESS"));
    }

    @DeleteMapping("/pharmacies/{pharmacyId}")
    @Transactional
    public ResponseEntity<ApiResponse<String>> removePharmacyFavorite(
            @PathVariable Long pharmacyId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        favoritePharmacyRepository.deleteByUserIdAndPharmacyId(userId, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success("Removed from favorites", "SUCCESS"));
    }
}

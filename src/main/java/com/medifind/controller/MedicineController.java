package com.medifind.controller;

import com.medifind.dto.ApiResponse;
import com.medifind.dto.PageResponse;
import com.medifind.dto.medicine.MedicineCreateRequest;
import com.medifind.dto.medicine.MedicineDto;
import com.medifind.service.MedicineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MedicineDto>>> getAllMedicines(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy) {
        return ResponseEntity.ok(ApiResponse.success(medicineService.getAllMedicines(page, size, sortBy)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<MedicineDto>>> searchMedicines(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String searchTerm = !q.isBlank() ? q : query;
        if (searchTerm.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(medicineService.getAllMedicines(page, size, "name")));
        }
        return ResponseEntity.ok(ApiResponse.success(medicineService.searchMedicines(searchTerm, page, size)));
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<MedicineDto>>> getPopularMedicines() {
        return ResponseEntity.ok(ApiResponse.success(medicineService.getPopularMedicines()));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<String>>> getSuggestions(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false, defaultValue = "") String q) {
        String term = !query.isBlank() ? query : q;
        return ResponseEntity.ok(ApiResponse.success(medicineService.getSuggestions(term)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MedicineDto>> getMedicine(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(medicineService.getMedicineById(id)));
    }

    @GetMapping("/{id}/related")
    public ResponseEntity<ApiResponse<List<MedicineDto>>> getRelatedMedicines(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(medicineService.getRelatedMedicines(id)));
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<ApiResponse<?>> getMedicineAvailability(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(medicineService.getMedicineAvailability(id)));
    }

    @GetMapping("/{id}/alternatives")
    public ResponseEntity<ApiResponse<List<com.medifind.dto.medicine.GenericAlternativeDto>>> getGenericAlternatives(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(medicineService.getGenericAlternatives(id)));
    }

    @GetMapping("/{id}/generics")
    public ResponseEntity<ApiResponse<List<com.medifind.dto.medicine.GenericAlternativeDto>>> getGenerics(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(medicineService.getGenericAlternatives(id)));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(medicineService.getCategories()));
    }

    @GetMapping("/dosage-forms")
    public ResponseEntity<ApiResponse<List<String>>> getDosageForms() {
        return ResponseEntity.ok(ApiResponse.success(medicineService.getDosageForms()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MedicineDto>> createMedicine(
            @Valid @RequestBody MedicineCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Medicine added to catalog", medicineService.createMedicine(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MedicineDto>> updateMedicine(
            @PathVariable Long id,
            @Valid @RequestBody MedicineCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Medicine updated", medicineService.updateMedicine(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateMedicine(@PathVariable Long id) {
        medicineService.deactivateMedicine(id);
        return ResponseEntity.ok(ApiResponse.success("Medicine deactivated", null));
    }
}

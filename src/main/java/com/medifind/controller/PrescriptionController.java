package com.medifind.controller;

import com.medifind.dto.ApiResponse;
import com.medifind.dto.prescription.PrescriptionDocumentDto;
import com.medifind.dto.prescription.PrescriptionScanRequest;
import com.medifind.dto.prescription.PrescriptionScanResultDto;
import com.medifind.dto.prescription.PrescriptionReviewRequest;
import com.medifind.service.PrescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @PostMapping("/scan")
    public ResponseEntity<ApiResponse<PrescriptionScanResultDto>> scan(
            @RequestBody PrescriptionScanRequest request) {
        return ResponseEntity.ok(ApiResponse.success(prescriptionService.scanPrescription(request)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<PrescriptionDocumentDto>> upload(
            @RequestParam Long pharmacyId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success("Prescription uploaded for pharmacy review",
                prescriptionService.upload(user.getUsername(), pharmacyId, file)));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<PrescriptionDocumentDto>>> mine(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(prescriptionService.getForUser(user.getUsername())));
    }

    @GetMapping("/pharmacy")
    @PreAuthorize("hasAnyRole('PHARMACY', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<PrescriptionDocumentDto>>> pharmacy(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(prescriptionService.getForPharmacy(user.getUsername())));
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('PHARMACY', 'ADMIN')")
    public ResponseEntity<ApiResponse<PrescriptionDocumentDto>> review(
            @PathVariable Long id,
            @Valid @RequestBody PrescriptionReviewRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success("Prescription reviewed",
                prescriptionService.review(user.getUsername(), id, request.getStatus(), request.getNotes())));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> download(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        PrescriptionService.PrescriptionFile file = prescriptionService.getFile(user.getUsername(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.originalFilename(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(file.path()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
        prescriptionService.deletePending(user.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Prescription deleted", null));
    }
}
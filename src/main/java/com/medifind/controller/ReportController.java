package com.medifind.controller;

import com.medifind.dto.ApiResponse;
import com.medifind.dto.report.AvailabilityReportDto;
import com.medifind.dto.report.AvailabilityReportRequest;
import com.medifind.dto.report.ReportStatusUpdateRequest;
import com.medifind.entity.AvailabilityReport;
import com.medifind.entity.Medicine;
import com.medifind.entity.Notification;
import com.medifind.entity.Pharmacy;
import com.medifind.entity.User;
import com.medifind.enums.ReportStatus;
import com.medifind.enums.NotificationType;
import com.medifind.exception.ResourceNotFoundException;
import com.medifind.repository.MedicineRepository;
import com.medifind.repository.NotificationRepository;
import com.medifind.repository.PharmacyRepository;
import com.medifind.repository.ReportRepository;
import com.medifind.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportRepository reportRepository;
    private final PharmacyRepository pharmacyRepository;
    private final MedicineRepository medicineRepository;
    private final UserRepository userRepository;
        private final NotificationRepository notificationRepository;

    @PostMapping("/availability")
    @PreAuthorize("hasRole('USER')")
    @Transactional
    public ResponseEntity<ApiResponse<AvailabilityReportDto>> submitReport(
            @Valid @RequestBody AvailabilityReportRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        Pharmacy pharmacy = pharmacyRepository.findById(request.getPharmacyId())
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", request.getPharmacyId()));
        Medicine medicine = request.getMedicineId() == null ? null
                : medicineRepository.findById(request.getMedicineId())
                        .orElseThrow(() -> new ResourceNotFoundException("Medicine", request.getMedicineId()));

        AvailabilityReport report = reportRepository.save(AvailabilityReport.builder()
                .user(user)
                .pharmacy(pharmacy)
                .medicine(medicine)
                .reportType(request.getType())
                .description(request.getDescription().trim())
                .status(ReportStatus.PENDING)
                .build());
        return ResponseEntity.ok(ApiResponse.success("Report submitted", toDto(report)));
    }

    @GetMapping("/my-reports")
    @PreAuthorize("hasRole('USER')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<AvailabilityReportDto>>> getMyReports(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(reportRepository.findByUserId(getUser(userDetails).getId())
                .stream().map(this::toDto).toList()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<AvailabilityReportDto>>> getAllReports() {
        return ResponseEntity.ok(ApiResponse.success(reportRepository.findAll().stream().map(this::toDto).toList()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<AvailabilityReportDto>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ReportStatusUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        AvailabilityReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));
        report.setStatus(request.getStatus());
        report.setAdminNotes(request.getNotes() == null ? null : request.getNotes().trim());
        report.setResolvedAt(request.getStatus() == ReportStatus.RESOLVED || request.getStatus() == ReportStatus.REJECTED
                ? LocalDateTime.now() : null);
        report.setResolvedBy(getUser(userDetails));
        AvailabilityReport saved = reportRepository.save(report);
        notificationRepository.save(Notification.builder()
                .user(report.getUser())
                .type(NotificationType.REPORT_UPDATE)
                .title("Availability report updated")
                .message("An administrator has updated the status of your report.")
                .build());
        return ResponseEntity.ok(ApiResponse.success("Report status updated", toDto(saved)));
    }

    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<String>>> getReportTypes() {
        return ResponseEntity.ok(ApiResponse.success(Arrays.stream(com.medifind.enums.ReportType.values())
                .map(Enum::name).toList()));
    }

    private User getUser(UserDetails userDetails) {
        if (userDetails == null) throw new AccessDeniedException("Sign in to submit or view reports.");
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user could not be found."));
    }

    private AvailabilityReportDto toDto(AvailabilityReport report) {
        return AvailabilityReportDto.builder()
                .id(report.getId())
                .reporterName(report.getUser().getFullName())
                .pharmacyId(report.getPharmacy().getId())
                .pharmacyName(report.getPharmacy().getName())
                .medicineId(report.getMedicine() == null ? null : report.getMedicine().getId())
                .medicineName(report.getMedicine() == null ? null : report.getMedicine().getName())
                .issueType(report.getReportType())
                .details(report.getDescription())
                .status(report.getStatus())
                .adminNotes(report.getAdminNotes())
                .createdAt(report.getCreatedAt())
                .build();
    }
}

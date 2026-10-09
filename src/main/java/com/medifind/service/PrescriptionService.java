package com.medifind.service;

import com.medifind.dto.prescription.PrescriptionDocumentDto;
import com.medifind.dto.prescription.PrescriptionScanRequest;
import com.medifind.dto.prescription.PrescriptionScanResultDto;
import com.medifind.entity.Inventory;
import com.medifind.entity.Medicine;
import com.medifind.entity.Pharmacy;
import com.medifind.entity.PrescriptionDocument;
import com.medifind.entity.Notification;
import com.medifind.entity.User;
import com.medifind.enums.NotificationType;
import com.medifind.enums.PrescriptionStatus;
import com.medifind.enums.Role;
import com.medifind.exception.BadRequestException;
import com.medifind.exception.ResourceNotFoundException;
import com.medifind.repository.InventoryRepository;
import com.medifind.repository.MedicineRepository;
import com.medifind.repository.PharmacyRepository;
import com.medifind.repository.PrescriptionRepository;
import com.medifind.repository.NotificationRepository;
import com.medifind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;

    private final PrescriptionRepository prescriptionRepository;
    private final NotificationRepository notificationRepository;
    private final PharmacyRepository pharmacyRepository;
    private final UserRepository userRepository;
    private final MedicineRepository medicineRepository;
    private final InventoryRepository inventoryRepository;

    @Value("${app.prescriptions.storage-directory:private-uploads/prescriptions}")
    private String configuredStorageDirectory;

    @Transactional(readOnly = true)
    public PrescriptionScanResultDto scanPrescription(PrescriptionScanRequest request) {
        // Collect medicine names to match from the request
        List<String> inputNames = new ArrayList<>();
        if (request.getMedicineNames() != null && !request.getMedicineNames().isEmpty()) {
            inputNames.addAll(request.getMedicineNames());
        } else if (request.getPrescriptionText() != null && !request.getPrescriptionText().isBlank()) {
            // Simple tokenisation: split on newlines/commas and extract tokens > 3 chars
            String[] tokens = request.getPrescriptionText().split("[,\\n\\r]+");
            for (String t : tokens) {
                String cleaned = t.replaceAll("[^a-zA-Z0-9 ./-]", "").trim();
                if (!cleaned.isBlank() && cleaned.length() > 3) {
                    inputNames.add(cleaned);
                }
            }
        }

        // Match each input name to medicine catalog
        List<PrescriptionScanResultDto.ScannedMedicineItemDto> extractedMedicines = inputNames.stream()
                .map(rawText -> {
                    List<Medicine> matches = medicineRepository.findByNameContainingIgnoreCase(rawText.split(" ")[0]);
                    Optional<Medicine> best = matches.stream().findFirst();
                    return PrescriptionScanResultDto.ScannedMedicineItemDto.builder()
                            .rawText(rawText)
                            .matchedMedicineId(best.map(Medicine::getId).orElse(null))
                            .matchedMedicineName(best.map(Medicine::getName).orElse(null))
                            .genericName(best.map(Medicine::getGenericName).orElse(null))
                            .strength(best.map(Medicine::getStrength).orElse(null))
                            .dosageForm(best.map(Medicine::getDosageForm).orElse(null))
                            .quantity(1)
                            .matched(best.isPresent())
                            .build();
                })
                .toList();

        // Get IDs of matched medicines
        List<Long> matchedIds = extractedMedicines.stream()
                .filter(PrescriptionScanResultDto.ScannedMedicineItemDto::getMatched)
                .map(PrescriptionScanResultDto.ScannedMedicineItemDto::getMatchedMedicineId)
                .toList();

        if (matchedIds.isEmpty()) {
            return PrescriptionScanResultDto.builder()
                    .extractedMedicines(extractedMedicines)
                    .pharmacyMatches(List.of())
                    .build();
        }

        // Load all inventory for matched medicines
        List<Inventory> allInventory = inventoryRepository.findByMedicineIdIn(matchedIds);

        // Group by pharmacy
        Map<Long, List<Inventory>> byPharmacy = allInventory.stream()
                .collect(Collectors.groupingBy(inv -> inv.getPharmacy().getId()));

        int totalItems = matchedIds.size();

        List<PrescriptionScanResultDto.PharmacyFulfillmentDto> pharmacyMatches = byPharmacy.entrySet().stream()
                .map(entry -> {
                    Pharmacy ph = entry.getValue().get(0).getPharmacy();
                    List<Inventory> phInventory = entry.getValue();

                    // Build fulfilled item map (one per medicine)
                    Map<Long, Inventory> medicineInvMap = phInventory.stream()
                            .collect(Collectors.toMap(inv -> inv.getMedicine().getId(), inv -> inv, (a, b) -> a));

                    List<PrescriptionScanResultDto.FulfilledItemDto> items = matchedIds.stream()
                            .map(medId -> {
                                Inventory inv = medicineInvMap.get(medId);
                                boolean inStock = inv != null && inv.getStockQuantity() != null && inv.getStockQuantity() > 0;
                                return PrescriptionScanResultDto.FulfilledItemDto.builder()
                                        .medicineId(medId)
                                        .medicineName(inv != null ? inv.getMedicine().getName() : null)
                                        .inventoryId(inv != null ? inv.getId() : null)
                                        .inStock(inStock)
                                        .availableStock(inv != null ? inv.getStockQuantity() : 0)
                                        .price(inv != null ? inv.getPrice() : null)
                                        .build();
                            })
                            .toList();

                    int inStockCount = (int) items.stream().filter(PrescriptionScanResultDto.FulfilledItemDto::getInStock).count();
                    double matchPct = totalItems > 0 ? (inStockCount * 100.0 / totalItems) : 0.0;

                    BigDecimal totalPrice = items.stream()
                            .filter(PrescriptionScanResultDto.FulfilledItemDto::getInStock)
                            .map(i -> i.getPrice() != null ? i.getPrice() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return PrescriptionScanResultDto.PharmacyFulfillmentDto.builder()
                            .pharmacyId(ph.getId())
                            .pharmacyName(ph.getName())
                            .area(ph.getArea())
                            .address(ph.getAddress())
                            .phone(ph.getPhone())
                            .open24Hours(ph.getOpen24Hours())
                            .verified(ph.getVerified())
                            .rating(ph.getRating())
                            .itemsAvailableCount(inStockCount)
                            .totalItemsCount(totalItems)
                            .matchPercentage(Math.round(matchPct * 10.0) / 10.0)
                            .totalEstimatedPrice(totalPrice)
                            .items(items)
                            .build();
                })
                .sorted((a, b) -> Double.compare(b.getMatchPercentage(), a.getMatchPercentage()))
                .limit(5)
                .toList();

        return PrescriptionScanResultDto.builder()
                .extractedMedicines(extractedMedicines)
                .pharmacyMatches(pharmacyMatches)
                .build();
    }

    @Transactional
    public PrescriptionDocumentDto upload(String email, Long pharmacyId, MultipartFile file) {
        User user = getUser(email);
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", pharmacyId));
        if (!Boolean.TRUE.equals(pharmacy.getVerified())) {
            throw new BadRequestException("Prescriptions can only be sent to verified pharmacies.");
        }
        String contentType = validateUpload(file);
        String originalFilename = safeOriginalFilename(file.getOriginalFilename());
        String extension = switch (contentType) {
            case "application/pdf" -> ".pdf";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> throw new BadRequestException("Unsupported prescription file type.");
        };
        String storageKey = UUID.randomUUID() + extension;
        Path target = resolveStoragePath(storageKey);
        try {
            Files.createDirectories(storageRoot());
            file.transferTo(target);
        } catch (IOException exception) {
            throw new BadRequestException("The prescription could not be stored. Please try again.");
        }

        try {
            PrescriptionDocument document = prescriptionRepository.save(PrescriptionDocument.builder()
                    .user(user)
                    .pharmacy(pharmacy)
                    .originalFilename(originalFilename)
                    .storageKey(storageKey)
                    .contentType(contentType)
                    .fileSize(file.getSize())
                    .status(PrescriptionStatus.PENDING)
                    .build());
            return toDto(document);
        } catch (RuntimeException exception) {
            try { Files.deleteIfExists(target); } catch (IOException ignored) { }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<PrescriptionDocumentDto> getForUser(String email) {
        return prescriptionRepository.findByUserIdOrderByCreatedAtDesc(getUser(email).getId())
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<PrescriptionDocumentDto> getForPharmacy(String email) {
        User user = getUser(email);
        List<PrescriptionDocument> documents;
        if (user.getRole() == Role.ADMIN) {
            documents = prescriptionRepository.findAll();
        } else {
            Pharmacy pharmacy = pharmacyRepository.findByOwnerId(user.getId())
                    .orElseThrow(() -> new AccessDeniedException("No pharmacy is linked to this account."));
            documents = prescriptionRepository.findByPharmacyIdOrderByCreatedAtDesc(pharmacy.getId());
        }
        return documents.stream().map(this::toDto).toList();
    }

    @Transactional
    public PrescriptionDocumentDto review(String email, Long documentId, PrescriptionStatus status, String notes) {
        User reviewer = getUser(email);
        PrescriptionDocument document = findDocument(documentId);
        assertPharmacyAccess(reviewer, document);
        if (document.getStatus() != PrescriptionStatus.PENDING) {
            throw new BadRequestException("Only pending prescriptions can be reviewed.");
        }
        if (status == PrescriptionStatus.PENDING) {
            throw new BadRequestException("Choose approved or rejected for a review.");
        }
        document.setStatus(status);
        document.setReviewNotes(notes == null || notes.isBlank() ? null : notes.trim());
        document.setReviewedBy(reviewer);
        document.setReviewedAt(LocalDateTime.now());
        PrescriptionDocument saved = prescriptionRepository.save(document);
        notificationRepository.save(Notification.builder()
            .user(document.getUser())
            .type(NotificationType.PRESCRIPTION_UPDATE)
            .title("Prescription review updated")
            .message("A pharmacy has completed its review. Sign in to see the status.")
            .build());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public PrescriptionFile getFile(String email, Long documentId) {
        User requester = getUser(email);
        PrescriptionDocument document = findDocument(documentId);
        boolean owner = document.getUser().getId().equals(requester.getId());
        boolean pharmacyOwner = document.getPharmacy().getOwner() != null
                && document.getPharmacy().getOwner().getId().equals(requester.getId());
        if (!owner && !pharmacyOwner && requester.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("You are not allowed to access this prescription.");
        }
        Path file = resolveStoragePath(document.getStorageKey());
        if (!Files.isRegularFile(file)) throw new ResourceNotFoundException("Prescription file", documentId);
        return new PrescriptionFile(file, document.getOriginalFilename(), document.getContentType());
    }

    @Transactional
    public void deletePending(String email, Long documentId) {
        User owner = getUser(email);
        PrescriptionDocument document = prescriptionRepository.findByIdAndUserId(documentId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", documentId));
        if (document.getStatus() != PrescriptionStatus.PENDING) {
            throw new BadRequestException("Only pending prescriptions can be deleted.");
        }
        prescriptionRepository.delete(document);
        try { Files.deleteIfExists(resolveStoragePath(document.getStorageKey())); }
        catch (IOException exception) { throw new BadRequestException("The prescription file could not be deleted."); }
    }

    private void assertPharmacyAccess(User user, PrescriptionDocument document) {
        if (user.getRole() == Role.ADMIN) return;
        if (user.getRole() != Role.PHARMACY || document.getPharmacy().getOwner() == null
                || !document.getPharmacy().getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can only review prescriptions sent to your pharmacy.");
        }
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private PrescriptionDocument findDocument(Long id) {
        return prescriptionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Prescription", id));
    }

    private String validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Choose a prescription file to upload.");
        if (file.getSize() > MAX_FILE_SIZE) throw new BadRequestException("Prescription files must be 10 MB or smaller.");
        String contentType = file.getContentType();
        if (!List.of("application/pdf", "image/jpeg", "image/png").contains(contentType)) {
            throw new BadRequestException("Upload a PDF, JPEG, or PNG prescription.");
        }
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(8);
            boolean valid = switch (contentType) {
                case "application/pdf" -> header.length >= 5 && header[0] == '%' && header[1] == 'P'
                        && header[2] == 'D' && header[3] == 'F' && header[4] == '-';
                case "image/jpeg" -> header.length >= 3 && (header[0] & 0xff) == 0xff
                        && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff;
                case "image/png" -> header.length >= 8 && (header[0] & 0xff) == 0x89
                        && header[1] == 'P' && header[2] == 'N' && header[3] == 'G';
                default -> false;
            };
            if (!valid) throw new BadRequestException("The file contents do not match the selected file type.");
        } catch (IOException exception) {
            throw new BadRequestException("The uploaded file could not be read.");
        }
        return contentType;
    }

    private String safeOriginalFilename(String filename) {
        String cleaned = StringUtils.cleanPath(filename == null ? "prescription" : filename).replace('\\', '/');
        String basename = cleaned.substring(cleaned.lastIndexOf('/') + 1);
        if (basename.isBlank() || basename.contains("..") || basename.length() > 255) {
            throw new BadRequestException("Invalid prescription filename.");
        }
        return basename;
    }

    private Path storageRoot() {
        return Paths.get(configuredStorageDirectory).toAbsolutePath().normalize();
    }

    private Path resolveStoragePath(String storageKey) {
        Path root = storageRoot();
        Path path = root.resolve(storageKey).normalize();
        if (!path.startsWith(root)) throw new BadRequestException("Invalid prescription storage reference.");
        return path;
    }

    private PrescriptionDocumentDto toDto(PrescriptionDocument document) {
        return PrescriptionDocumentDto.builder()
                .id(document.getId())
                .pharmacyId(document.getPharmacy().getId())
                .pharmacyName(document.getPharmacy().getName())
                .patientName(document.getUser().getFullName())
                .originalFilename(document.getOriginalFilename())
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .status(document.getStatus())
                .reviewNotes(document.getReviewNotes())
                .createdAt(document.getCreatedAt())
                .reviewedAt(document.getReviewedAt())
                .build();
    }

    public record PrescriptionFile(Path path, String originalFilename, String contentType) { }
}
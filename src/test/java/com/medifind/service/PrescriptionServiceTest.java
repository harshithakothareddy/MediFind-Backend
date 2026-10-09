package com.medifind.service;

import com.medifind.entity.Pharmacy;
import com.medifind.entity.PrescriptionDocument;
import com.medifind.entity.User;
import com.medifind.enums.PrescriptionStatus;
import com.medifind.exception.BadRequestException;
import com.medifind.repository.PharmacyRepository;
import com.medifind.repository.NotificationRepository;
import com.medifind.repository.PrescriptionRepository;
import com.medifind.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private PharmacyRepository pharmacyRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private PrescriptionService prescriptionService;

    @TempDir Path tempDirectory;

    @BeforeEach
    void setPrivateStorageDirectory() {
        ReflectionTestUtils.setField(prescriptionService, "configuredStorageDirectory", tempDirectory.toString());
    }

    @Test
    void validPdfIsStoredOutsidePublicResourcesAndPersistsOnlyPrivateKey() throws Exception {
        User user = User.builder().id(7L).firstName("Asha").lastName("Patient").email("asha@example.com").build();
        Pharmacy pharmacy = Pharmacy.builder().id(3L).name("Verified Pharmacy").verified(true).build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(pharmacyRepository.findById(3L)).thenReturn(Optional.of(pharmacy));
        when(prescriptionRepository.save(any(PrescriptionDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("file", "my-prescription.pdf", "application/pdf",
                "%PDF-1.7\nprivate content".getBytes());

        var response = prescriptionService.upload(user.getEmail(), 3L, file);

        ArgumentCaptor<PrescriptionDocument> saved = ArgumentCaptor.forClass(PrescriptionDocument.class);
        verify(prescriptionRepository).save(saved.capture());
        assertEquals(PrescriptionStatus.PENDING, response.getStatus());
        assertEquals("my-prescription.pdf", response.getOriginalFilename());
        assertFalse(saved.getValue().getStorageKey().contains("my-prescription"));
        assertTrue(Files.isRegularFile(tempDirectory.resolve(saved.getValue().getStorageKey())));
    }

    @Test
    void uploadRejectsContentWhoseSignatureDoesNotMatchMimeType() {
        User user = User.builder().id(7L).email("asha@example.com").build();
        Pharmacy pharmacy = Pharmacy.builder().id(3L).name("Verified Pharmacy").verified(true).build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(pharmacyRepository.findById(3L)).thenReturn(Optional.of(pharmacy));
        MockMultipartFile file = new MockMultipartFile("file", "fake.png", "image/png", "not an image".getBytes());

        assertThrows(BadRequestException.class, () -> prescriptionService.upload(user.getEmail(), 3L, file));
        verify(prescriptionRepository, never()).save(any(PrescriptionDocument.class));
    }

    @Test
    void anotherPatientCannotDownloadPrescription() {
        User owner = User.builder().id(7L).firstName("Asha").lastName("Patient").email("asha@example.com").build();
        User requester = User.builder().id(8L).firstName("Ravi").lastName("Patient").email("ravi@example.com").build();
        User pharmacyOwner = User.builder().id(9L).firstName("Pharmacy").lastName("Owner").build();
        Pharmacy pharmacy = Pharmacy.builder().id(3L).name("Verified Pharmacy").owner(pharmacyOwner).build();
        PrescriptionDocument document = PrescriptionDocument.builder().id(11L).user(owner).pharmacy(pharmacy)
                .storageKey("random-key.pdf").originalFilename("rx.pdf").contentType("application/pdf").fileSize(10L).build();
        when(userRepository.findByEmail(requester.getEmail())).thenReturn(Optional.of(requester));
        when(prescriptionRepository.findById(11L)).thenReturn(Optional.of(document));

        assertThrows(AccessDeniedException.class, () -> prescriptionService.getFile(requester.getEmail(), 11L));
    }
}
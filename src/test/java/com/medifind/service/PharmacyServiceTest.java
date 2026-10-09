package com.medifind.service;

import com.medifind.entity.Pharmacy;
import com.medifind.repository.PharmacyRepository;
import com.medifind.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PharmacyServiceTest {

    @Mock private PharmacyRepository pharmacyRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private PharmacyService pharmacyService;

    @Test
    void nearbySearchPassesCoordinatesAndRadiusAndMapsResults() {
        Pharmacy pharmacy = Pharmacy.builder().id(12L).name("Local Medical Store")
                .licenseNumber("LIC-12").phone("1234567890").address("Main Road")
                .area("Madhapur").city("Hyderabad").state("Telangana").pincode("500081")
                .latitude(17.44).longitude(78.39).build();
        when(pharmacyRepository.findNearbyPharmacies(17.44, 78.39, 10)).thenReturn(List.of(pharmacy));

        var results = pharmacyService.getNearbyPharmacies(17.44, 78.39, 10);

        assertEquals(1, results.size());
        assertEquals("Local Medical Store", results.get(0).getName());
        verify(pharmacyRepository).findNearbyPharmacies(17.44, 78.39, 10);
    }
}

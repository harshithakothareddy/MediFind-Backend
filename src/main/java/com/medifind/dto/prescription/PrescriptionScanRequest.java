package com.medifind.dto.prescription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionScanRequest {
    private String prescriptionText;
    private String fileName;
    private List<String> medicineNames;
}

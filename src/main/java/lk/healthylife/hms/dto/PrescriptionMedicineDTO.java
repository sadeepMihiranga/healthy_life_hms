package lk.healthylife.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionMedicineDTO {

    private Long prescriptionMedicineId;
    @NotNull(message = "Prescription Id is mandatory")
    private Long prescriptionId;
    private Long symptomId;
    @NotNull(message = "Medicine Id is mandatory")
    private Long medicineId;
    private String remarks;
    private Short status;
    private Long branchId;
    private String branchName;
    private LocalDateTime createdDate;
    private String createdUserCode;
    private LocalDateTime lastUpdatedDate;
    private String lastUpdatedUserCode;
    private MedicineDTO medicine;
    private SymptomDTO symptom;
}

package lk.healthylife.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientMedicalTestDTO implements Paginated {

    private Long patientMedicalTestId;
    @NotNull(message = "Medical Test Id is mandatory")
    private Long medicalTestId;
    private String medicalTestName;
    private Long admissionId;
    @NotBlank(message = "Patient Code is mandatory")
    private String patientCode;
    private String patientName;
    private String testedBy;
    private String approvedBy;
    private Short testStatus;
    private String remarks;
    private Short status;
    private Long branchId;
    private String branchName;
    private LocalDateTime createdDate;
    private String createdUserCode;
    private LocalDateTime lastUpdatedDate;
    private String lastUpdatedUserCode;
    private PartyDTO patient;
    private MedicalTestDTO medicalTest;
}

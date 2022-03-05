package lk.healthylife.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientAdmissionDTO implements Paginated {

    private Long patientAdmissionId;
    @NotBlank(message = "Patient Code is mandatory")
    private String patientCode;
    private String patientName;
    @NotNull(message = "Room Id is mandatory")
    private Long roomId;
    private String roomNo;
    private String admissionApprovedDoctor;
    private LocalDateTime admittedDate;
    private LocalDateTime dischargedDate;
    private String dischargeApprovedDoctor;
    private String remarks;
    private Short status;
    private Long branchId;
    private String branchName;
    private LocalDateTime createdDate;
    private String createdUserCode;
    private LocalDateTime lastUpdatedDate;
    private String lastUpdatedUserCode;
    private List<PatientConditionDTO> conditions;
    private List<TreatmentAdviceDTO> treatmentAdvices;
}

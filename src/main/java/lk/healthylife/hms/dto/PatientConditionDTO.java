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
public class PatientConditionDTO {

    private Long patientConditionId;
    @NotNull(message = "Admission Id is mandatory")
    private Long admissionId;
    private Long patientSurgeryId;
    private Long symptomId;
    @NotBlank(message = "Condition When is mandatory")
    private String conditionWhen;
    private String description;
    private Short status;
    private Long branchId;
    private String branchName;
    private LocalDateTime createdDate;
    private String createdUserCode;
    private LocalDateTime lastUpdatedDate;
    private String lastUpdatedUserCode;
}

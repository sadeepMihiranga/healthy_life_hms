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
public class TreatmentAdviceDTO {

    private Long adviceId;
    @NotNull(message = "Admission Id is mandatory")
    private Long patientAdmissionId;
    @NotBlank(message = "Patient Code is mandatory")
    private String patientCode;
    @NotBlank(message = "Doctor Code is mandatory")
    private String doctorCode;
    @NotBlank(message = "Advice is mandatory")
    private String advice;
    private Short status;
    private Long branchId;
    private String branchName;
    private LocalDateTime createdDate;
    private String createdUserCode;
    private LocalDateTime lastUpdatedDate;
    private String lastUpdatedUserCode;
}

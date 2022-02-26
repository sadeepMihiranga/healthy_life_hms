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
public class DoctorSurgeryDTO {

    private Long doctorSurgeryId;
    @NotNull(message = "Patient Surgery Id is mandatory")
    private Long patientSurgeryId;
    @NotBlank(message = "Doctor Code is mandatory")
    private String doctorCode;
    private String remarks;
    private Short status;
    private Long branchId;
    private String branchName;
    private LocalDateTime createdDate;
    private String createdUserCode;
    private LocalDateTime lastUpdatedDate;
    private String lastUpdatedUserCode;
}

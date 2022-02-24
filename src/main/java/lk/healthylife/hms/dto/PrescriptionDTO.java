package lk.healthylife.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionDTO implements Paginated {

    private Long prescriptionId;
    @NotBlank(message = "Doctor Code is mandatory")
    private String doctorCode;
    private String doctorName;
    @NotBlank(message = "Patient Code is mandatory")
    private String patientCode;
    private String patientName;
    private String patientNic;
    private String remarks;
    private Short status;
    private Long branchId;
    private String branchName;
    private LocalDateTime createdDate;
    private String createdUserCode;
    private LocalDateTime lastUpdatedDate;
    private String lastUpdatedUserCode;
    private List<PrescriptionMedicineDTO> prescriptionMedicines;
}

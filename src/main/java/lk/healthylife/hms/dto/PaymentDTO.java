package lk.healthylife.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO implements Paginated {

    private Long paymentId;
    private String patientCode;
    private String patientName;
    private String type;
    private String typeName;
    private BigDecimal amount;
    private String description;
    private Long admissionId;
    private Long prescriptionId;
    private Long patientMedicalTestId;
    private String canceledReason;
    private Short status;
    private Long branchId;
    private String branchName;
    private LocalDateTime createdDate;
    private String createdUserCode;
    private LocalDateTime lastUpdatedDate;
    private String lastUpdatedUserCode;
}

package lk.healthylife.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalTestDTO implements Paginated {

    private Long medicalTestId;
    @NotBlank(message = "Medical Test Name is mandatory")
    private String name;
    @NotBlank(message = "Description is mandatory")
    private String description;
    private BigDecimal fee;
    @NotBlank(message = "Medical Test Type is mandatory")
    private String type;
    private String typeName;
    private Short status;
    private Long branchId;
    private String branchName;
    private LocalDateTime createdDate;
    private String createdUserCode;
    private LocalDateTime lastUpdatedDate;
    private String lastUpdatedUserCode;
}

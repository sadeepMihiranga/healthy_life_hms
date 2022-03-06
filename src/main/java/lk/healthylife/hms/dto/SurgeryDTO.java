package lk.healthylife.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurgeryDTO implements Paginated {

    private Long surgeryId;
    @NotBlank(message = "Surgery Name is mandatory")
    private String name;
    @NotBlank(message = "Surgery Type is mandatory")
    private String type;
    private String typeName;
    private String description;
    private Short status;
    @NotNull(message = "Surgery Fee is mandatory")
    private BigDecimal fee;
    private BigDecimal estimatedTimeInHours;
    private LocalDateTime createdDate;
    private String createdUserCode;
    private LocalDateTime lastUpdatedDate;
    private String lastUpdatedUserCode;
}

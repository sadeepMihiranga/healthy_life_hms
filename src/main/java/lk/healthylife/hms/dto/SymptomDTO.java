package lk.healthylife.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SymptomDTO {

    private Long symptomId;
    @NotBlank(message = "Symptom Name is mandatory")
    private String name;
    @NotBlank(message = "Symptom Type is mandatory")
    private String type;
    private String typeName;
    private String description;
    private LocalDateTime createdDate;
    private String createdUserCode;
    private LocalDateTime lastUpdatedDate;
    private String lastUpdatedUserCode;
}

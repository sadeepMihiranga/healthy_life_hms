package lk.healthylife.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonReferenceDTO {

    private String cmrfCode;
    private String referenceType;
    private String description;
    private String stringValue;
    private String numberValue;
    private Short status;
}

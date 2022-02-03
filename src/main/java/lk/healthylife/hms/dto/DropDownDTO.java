package lk.healthylife.hms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DropDownDTO {

    @JsonProperty("value")
    private String key;
    @JsonProperty("label")
    private String value;
    private String description;
    private Short status;
}

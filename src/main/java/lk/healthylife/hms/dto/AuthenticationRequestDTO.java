package lk.healthylife.hms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@JsonIgnoreProperties
public class AuthenticationRequestDTO {

    private String username;
    private String password;
}

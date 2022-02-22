package lk.healthylife.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacilityDTO implements Paginated {

    private Long facilityId;
    @NotBlank(message = "Facility Name is mandatory")
    private String name;
    private String description;
    private Short status;
    private Long branchId;
    private String branchName;
    private LocalDateTime createdDate;
    private String createdUserCode;
    private LocalDateTime lastUpdatedDate;
    private String lastUpdatedUserCode;

    public FacilityDTO(Long facilityId) {
        this.facilityId = facilityId;
    }
}

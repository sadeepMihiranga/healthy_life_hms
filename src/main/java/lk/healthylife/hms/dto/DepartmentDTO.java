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
public class DepartmentDTO implements Paginated {

    private String departmentCode;
    @NotBlank(message = "Department Name is mandatory")
    private String name;
    private String description;
    private Short status;
    private String departmentHead;
    private String departmentHeadName;
    private LocalDateTime createdDate;
    private String createdUserCode;
    private LocalDateTime lastUpdatedDate;
    private String lastUpdatedUserCode;
    private List<DepartmentLocationDTO> departmentLocations;
    private List<DepartmentFacilityDTO> departmentFacilities;
    private List<FacilityDTO> facilities;

    public DepartmentDTO(String departmentCode) {
        this.departmentCode = departmentCode;
    }
}

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
public class RoomDTO implements Paginated {

    private Long roomId;
    @NotBlank(message = "Room No is mandatory")
    private String roomNo;
    @NotBlank(message = "Room Type is mandatory")
    private String roomType;
    private String roomTypeName;
    private Short status;
    private BigDecimal perDayCharge;
    private String description;
    private Long branchId;
    private String branchName;
    private LocalDateTime createdDate;
    private String createdUserCode;
    private LocalDateTime lastUpdatedDate;
    private String lastUpdatedUserCode;
    private DepartmentDTO department;

    public RoomDTO(Long roomId) {
        this.roomId = roomId;
    }
}

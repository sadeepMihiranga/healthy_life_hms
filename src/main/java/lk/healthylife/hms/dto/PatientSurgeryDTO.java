package lk.healthylife.hms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientSurgeryDTO implements Paginated {

    private Long patientSurgeryId;
    @NotNull(message = "Surgery Id is mandatory")
    private Long surgeryId;
    private String surgeryName;
    @NotBlank(message = "Patient Code is mandatory")
    private String patientCode;
    private String patientName;
    @NotNull(message = "Admission Id is mandatory")
    private Long admissionId;
    @NotNull(message = "Operation Room Id is mandatory")
    private Long operationRoomId;
    private String operationRoomNo;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedDateTime;
    private LocalDateTime endedDateTime;
    private Short status;
    private Long branchId;
    private String branchName;
    private LocalDateTime createdDate;
    private String createdUserCode;
    private LocalDateTime lastUpdatedDate;
    private String lastUpdatedUserCode;
    private List<DoctorSurgeryDTO> doctorsInSurgery;
    private SurgeryDTO surgery;
    private RoomDTO operationRoom;
    private PartyDTO patient;

}

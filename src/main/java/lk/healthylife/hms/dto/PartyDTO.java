package lk.healthylife.hms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartyDTO implements Paginated {

    private String partyCode;
    private String name;
    private String initials;
    @NotBlank(message = "First Name is mandatory")
    private String firstName;
    private String lastName;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;
    private String address1;
    private String address2;
    private String address3;
    @NotBlank(message = "Gender is mandatory")
    private String gender;
    private String genderName;
    private String nic;
    private String passport;
    @NotBlank(message = "Blood Group is mandatory")
    private String bloodGroup;
    private String specialization;
    private String specializationName;
    @NotBlank(message = "Party Type is mandatory")
    private String type;
    private String departmentCode;
    private String departmentName;
    private Long branchId;
    private String branchName;
    private String managedBy;
    private String managedByName;
    private Short status;
    private LocalDateTime createdDate;
    private String createdUserCode;
    private LocalDateTime lastUpdatedDate;
    private String lastUpdatedUserCode;
    private List<PartyContactDTO> contactList;
    private UserDTO user;
}

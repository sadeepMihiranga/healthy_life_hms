package lk.healthylife.hms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO implements Paginated {

    private Long id;
    @NotBlank(message = "Party Code is required")
    private String partyCode;
    private String displayName;
    @NotBlank(message = "Username is required")
    private String username;
    @NotBlank(message = "Password is required")
    private String password;
    private Short status;
    private List<RoleDTO> roles;
    private List<FunctionDTO> functions;
    private List<PartyContactDTO> contactList;
    private List<BranchDTO> branches;
}

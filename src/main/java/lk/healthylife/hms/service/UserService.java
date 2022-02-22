package lk.healthylife.hms.service;

import lk.healthylife.hms.dto.PaginatedEntity;
import lk.healthylife.hms.dto.UserDTO;
import lk.healthylife.hms.entity.TMsRole;
import lk.healthylife.hms.entity.TMsRoleFunction;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

public interface UserService {

    UserDTO createUser(UserDTO userDTO);

    UserDTO getUserById(Long userId);

    List<UserDTO> getAllUsers();

    PaginatedEntity userPaginatedSearch(String username, String partyCode, Integer page, Integer size);

    UserDTO getUserByUsername(String username);

    UserDTO getUserByPartyCode(String partyCode);

    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;

    List<TMsRoleFunction> getPermissionsByRole(Long roleId);

    Long removeUserById(Long userId);

    Boolean removeUserByPartyCode(String partyCode, Boolean isPartyValidated);

    Boolean assignRoleToUser(Long userId, List<String> roles);

    TMsRole createRole(TMsRole role);

    UserDTO updateUser(Long userId, UserDTO userDTO);
}

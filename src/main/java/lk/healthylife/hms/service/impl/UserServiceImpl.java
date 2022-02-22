package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.config.EntityValidator;
import lk.healthylife.hms.config.repository.*;
import lk.healthylife.hms.dto.BranchDTO;
import lk.healthylife.hms.dto.FunctionDTO;
import lk.healthylife.hms.dto.PaginatedEntity;
import lk.healthylife.hms.dto.UserDTO;
import lk.healthylife.hms.entity.*;
import lk.healthylife.hms.exception.*;
import lk.healthylife.hms.mapper.RoleMapper;
import lk.healthylife.hms.mapper.UserMapper;
import lk.healthylife.hms.security.User;
import lk.healthylife.hms.service.PartyContactService;
import lk.healthylife.hms.service.UserService;
import lk.healthylife.hms.util.constant.Constants;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static lk.healthylife.hms.util.constant.Constants.STATUS_ACTIVE;
import static lk.healthylife.hms.util.constant.Constants.STATUS_INACTIVE;

@Slf4j
@Service
public class UserServiceImpl extends EntityValidator implements UserService, UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleFunctionRepository roleFunctionRepository;
    private final PartyRepository partyRepository;
    private final UserBranchRepository userBranchRepository;
    private final BranchRepository branchRepository;

    private final PartyContactService partyContactService;

    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           UserRoleRepository userRoleRepository,
                           RoleFunctionRepository roleFunctionRepository,
                           PartyRepository partyRepository,
                           UserBranchRepository userBranchRepository,
                           BranchRepository branchRepository,
                           PartyContactService partyContactService,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleFunctionRepository = roleFunctionRepository;
        this.partyRepository = partyRepository;
        this.userBranchRepository = userBranchRepository;
        this.branchRepository = branchRepository;
        this.partyContactService = partyContactService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    @Override
    public UserDTO createUser(UserDTO userDTO) {

        validateEntity(userDTO);

        final TMsParty tMsParty = partyRepository
                .findByPrtyCodeAndPrtyStatus(userDTO.getPartyCode(), STATUS_ACTIVE.getShortValue());

        if(tMsParty == null)
            throw new DataNotFoundException("Invalid Party Code " + userDTO.getPartyCode());

        final TMsUser userExists = userRepository
                .findByUserUsernameAndUserStatus(userDTO.getUsername(), STATUS_ACTIVE.getShortValue());

        if(userExists != null)
            throw new InvalidDataException("An User already exists with " + userDTO.getUsername());

        final TMsUser tMsUser = UserMapper.INSTANCE.dtoToEntity(userDTO);
        tMsUser.setUserPassword(passwordEncoder.encode(tMsUser.getUserPassword()));
        tMsUser.setParty(tMsParty);
        tMsUser.setUserStatus(STATUS_ACTIVE.getShortValue());

        final TMsUser createdUser = persistEntity(tMsUser);

        if(userDTO.getRoles() != null) {
            userDTO.getRoles().forEach(roleDTO -> {
                final TMsRole tMsRole = roleRepository
                        .findByRoleNameAndRoleStatus(roleDTO.getName(), STATUS_ACTIVE.getShortValue());

                if(tMsRole == null)
                    throw new InvalidDataException("Received Role " + roleDTO.getName() + " is invalid");

                TMsUserRole tMsUserRole = userRoleRepository
                        .findByUser_UserIdAndRole_RoleIdAndUsrlStatus(createdUser.getUserId(), tMsRole.getRoleId(), STATUS_ACTIVE.getShortValue());

                if(tMsUserRole != null)
                    throw new DuplicateRecordException("Requested User and Role combination is already there");

                tMsUserRole = new TMsUserRole();
                tMsUserRole.setUser(createdUser);
                tMsUserRole.setRole(tMsRole);
                tMsUserRole.setUsrlStatus(STATUS_ACTIVE.getShortValue());

                userRoleRepository.save(tMsUserRole);
            });
        }

        if(userDTO.getBranches() != null) {
            userDTO.getBranches().forEach(branchDTO -> {
                if(branchDTO != null && branchDTO.getBranchId() != null) {
                    final TRfBranch tRfBranch = branchRepository
                            .findByBrnhIdAndBrnhStatus(branchDTO.getBranchId(), STATUS_ACTIVE.getShortValue());

                    if(tRfBranch == null)
                        throw new DataNotFoundException("Branch not found for the given Id " + branchDTO.getBranchId());

                    TMsUserBranch tMsUserBranch = new TMsUserBranch();

                    tMsUserBranch.setUser(createdUser);
                    tMsUserBranch.setBranch(tRfBranch);
                    tMsUserBranch.setUsbrStatus(STATUS_ACTIVE.getShortValue());

                    userBranchRepository.save(tMsUserBranch);
                }
            });
        }

        return getUserById(createdUser.getUserId());
    }

    @Transactional
    @Override
    public TMsRole createRole(TMsRole tMsRole) {
        return roleRepository.save(tMsRole);
    }

    @Transactional
    @Override
    public UserDTO updateUser(Long userId, UserDTO userDTO) {

        TMsUser tMsUser = validateUserById(userId);

        TMsUser userWithSameName = userRepository
                .findByUserUsernameAndUserStatus(userDTO.getUsername(), STATUS_ACTIVE.getShortValue());

        if(userWithSameName != null && (userWithSameName.getUserId() != tMsUser.getUserId()))
            throw new InvalidDataException("Requested Username " + userDTO.getUsername() + " is already in use");

        List<String> newRoleList = new ArrayList<>();

        tMsUser.setUserUsername(userDTO.getUsername());
        persistEntity(tMsUser);

        final Integer inactivatedUserRoleCount = userRoleRepository
                .inactiveByUserId(tMsUser.getUserId(), STATUS_INACTIVE.getShortValue());

        userDTO.getRoles().forEach(roleDTO -> {
            newRoleList.add(roleDTO.getName());
        });

        assignRoleToUser(userId, newRoleList);

        final Integer inactivatedUserBranchCount = userBranchRepository
                .inactiveByUserId(tMsUser.getUserId(), STATUS_INACTIVE.getShortValue());

        assignBranchToUser(userDTO, tMsUser);

        return null;
    }

    private void assignBranchToUser(UserDTO userDTO, TMsUser tMsUser) {

        if(userDTO.getBranches() != null) {

            List<TMsUserBranch> tMsUserBranchList = new ArrayList<>();

            userDTO.getBranches().forEach(branchDTO -> {
                if(branchDTO != null && branchDTO.getBranchId() != null) {
                    final TRfBranch tRfBranch = branchRepository
                            .findByBrnhIdAndBrnhStatus(branchDTO.getBranchId(), STATUS_ACTIVE.getShortValue());

                    if(tRfBranch == null)
                        throw new DataNotFoundException("Branch not found for the given Id " + branchDTO.getBranchId());

                    TMsUserBranch existingUserAndBranch = userBranchRepository
                            .findAllByUser_UserIdAndBranch_BrnhId(tMsUser.getUserId(), tRfBranch.getBrnhId());

                    if(existingUserAndBranch != null) {

                        existingUserAndBranch.setUsbrStatus(STATUS_ACTIVE.getShortValue());
                        tMsUserBranchList.add(existingUserAndBranch);

                    } else {
                        TMsUserBranch tMsUserBranch = new TMsUserBranch();

                        tMsUserBranch.setUser(tMsUser);
                        tMsUserBranch.setBranch(tRfBranch);
                        tMsUserBranch.setUsbrStatus(STATUS_ACTIVE.getShortValue());

                        tMsUserBranchList.add(tMsUserBranch);
                    }
                }
            });

            userBranchRepository.saveAll(tMsUserBranchList);
        }
    }

    private TMsUser validateUserById(Long userId) {
        if(userId == null)
            throw new NoRequiredInfoException("User Id is required");

        final TMsUser tMsUser = userRepository.findByUserIdAndUserStatus(userId, STATUS_ACTIVE.getShortValue());

        if(tMsUser == null)
            throw new DataNotFoundException("User not found for Id " + userId);

        return tMsUser;
    }

    @Transactional
    @Override
    public Boolean assignRoleToUser(Long userId, List<String> roles) {

        if(roles == null || roles.isEmpty())
            throw new NoRequiredInfoException("Roles required");

        TMsUser tMsUser = validateUserById(userId);

        List<TMsUserRole> tMsUserRoleList = new ArrayList<>();

        roles.forEach(roleName -> {

            final TMsRole tMsRole = roleRepository.findByRoleNameAndRoleStatus(roleName, STATUS_ACTIVE.getShortValue());

            if(tMsRole == null)
                throw new DataNotFoundException("Role " + roleName + " not found");

            final TMsUserRole existingUserRole = userRoleRepository
                    .findByUser_UserIdAndRole_RoleId(tMsUser.getUserId(), tMsRole.getRoleId());

            if(existingUserRole != null) {

                existingUserRole.setUsrlStatus(STATUS_ACTIVE.getShortValue());
                tMsUserRoleList.add(existingUserRole);

            } else {

                TMsUserRole tRfUserRole = new TMsUserRole();
                tRfUserRole.setRole(tMsRole);
                tRfUserRole.setUser(tMsUser);
                tRfUserRole.setUsrlStatus(STATUS_ACTIVE.getShortValue());

                tMsUserRoleList.add(tRfUserRole);
            }
        });

        userRoleRepository.saveAll(tMsUserRoleList);

        return true;
    }

    @Override
    public UserDTO getUserByUsername(String username) {
        final TMsUser tMsUser = userRepository.findByUserUsernameAndUserStatus(username, STATUS_ACTIVE.getShortValue());

        if(tMsUser == null)
            throw new DataNotFoundException("User " + username + " not found");

        final List<TMsUserRole> tRfUserRoleList = userRoleRepository
                .findAllByUser_UserIdAndUsrlStatus(tMsUser.getUserId(), STATUS_ACTIVE.getShortValue());

        final UserDTO userDTO = UserMapper.INSTANCE.entityToDTO(tMsUser);
        userDTO.setRoles(tRfUserRoleList.stream().map(tRfUserRole -> RoleMapper.INSTANCE.entityToDTO(tRfUserRole.getRole())).collect(Collectors.toList()));

        return userDTO;
    }

    @Override
    public UserDTO getUserByPartyCode(String partyCode) {
        final TMsUser tMsUser = userRepository.findByParty_PrtyCodeAndUserStatus(partyCode, STATUS_ACTIVE.getShortValue());

        if(tMsUser == null)
            throw new DataNotFoundException("User " + partyCode + " not found");

        final List<TMsUserRole> tRfUserRoleList = userRoleRepository
                .findAllByUser_UserIdAndUsrlStatus(tMsUser.getUserId(), STATUS_ACTIVE.getShortValue());

        final UserDTO userDTO = UserMapper.INSTANCE.entityToDTO(tMsUser);
        userDTO.setRoles(tRfUserRoleList.stream().map(tRfUserRole -> RoleMapper.INSTANCE.entityToDTO(tRfUserRole.getRole())).collect(Collectors.toList()));

        return userDTO;
    }

    @Override
    public UserDTO getUserById(Long userId) {

        TMsUser tMsUser = validateUserById(userId);

        final List<TMsUserRole> tRfUserRoleList = userRoleRepository
                .findAllByUser_UserIdAndUsrlStatus(tMsUser.getUserId(), STATUS_ACTIVE.getShortValue());

        final UserDTO userDTO = UserMapper.INSTANCE.entityToDTO(tMsUser);

        userDTO.setPassword(null);
        userDTO.setRoles(tRfUserRoleList.stream().map(tRfUserRole -> RoleMapper.INSTANCE.entityToDTO(tRfUserRole.getRole())).collect(Collectors.toList()));
        populateUserReferenceData(userDTO);

        return userDTO;
    }

    private void populateUserReferenceData(UserDTO userDTO) {

        if(!Strings.isNullOrEmpty(userDTO.getPartyCode()))
            userDTO.setContactList(partyContactService.getContactsByPartyCode(userDTO.getPartyCode(), true));

        userDTO.setFunctions(getFunctionsByRoles(userDTO));

        List<BranchDTO> branchList = new ArrayList<>();

        final List<TMsUserBranch> userBranches = userBranchRepository
                .findAllByUser_UserIdAndUsbrStatus(userDTO.getId(), STATUS_ACTIVE.getShortValue());

        userBranches.forEach(tMsUserBranch -> {
            BranchDTO branchDTO = new BranchDTO();

            branchDTO.setBranchId(tMsUserBranch.getBranch().getBrnhId());
            branchDTO.setMame(tMsUserBranch.getBranch().getBrnhName());

            branchList.add(branchDTO);
        });

        userDTO.setBranches(branchList);
    }

    private List<FunctionDTO> getFunctionsByRoles(UserDTO userDTO) {

        List<FunctionDTO> functionDTOList = new ArrayList<>();

        userDTO.getRoles().forEach(roleDTO -> {
            final List<TMsRoleFunction> functionList = getPermissionsByRole(roleDTO.getId());

            functionList.forEach(tMsRoleFunction -> {
                functionDTOList.add(
                        new FunctionDTO(
                                tMsRoleFunction.getFunction().getFuncId(),
                                tMsRoleFunction.getFunction().getDunsDescription(),
                                tMsRoleFunction.getRofuStatus()
                        )
                );
            });
        });

        return functionDTOList;
    }

    @Override
    public List<UserDTO> getAllUsers() {

        List<UserDTO> userDTOList = new ArrayList<>();

        final List<TMsUser> userList = userRepository.findAllByUserStatus(STATUS_ACTIVE.getShortValue());

        userList.forEach(user -> {

            UserDTO userDTO = new UserDTO();

            final List<TMsUserRole> tRfUserRoleList = userRoleRepository
                    .findAllByUser_UserIdAndUsrlStatus(user.getUserId(), STATUS_ACTIVE.getShortValue());

            userDTO.setId(user.getUserId());
            userDTO.setPartyCode(user.getParty().getPrtyCode());
            userDTO.setDisplayName(user.getParty().getPrtyFirstName());
            userDTO.setUsername(user.getUserUsername());
            userDTO.setRoles(tRfUserRoleList.stream().map(tRfUserRole -> RoleMapper.INSTANCE.entityToDTO(tRfUserRole.getRole())).collect(Collectors.toList()));

            userDTOList.add(userDTO);
        });

        return userDTOList;
    }

    @Override
    public PaginatedEntity userPaginatedSearch(String username, String partyCode, Integer page, Integer size) {

        PaginatedEntity paginatedUserList = null;
        List<UserDTO> userList = null;

        validatePaginateIndexes(page, size);

        partyCode = partyCode.isEmpty() ? null : partyCode;

        final Page<TMsUser> tMsUserPage = userRepository
                .getActiveUsers(username, partyCode, STATUS_ACTIVE.getShortValue(), captureBranchIds(),
                        PageRequest.of(page - 1, size));

        if (tMsUserPage.getSize() == 0)
            return null;

        paginatedUserList = new PaginatedEntity();
        userList = new ArrayList<>();

        for (TMsUser tMsUser : tMsUserPage) {
            tMsUser.setUserPassword(null);
            userList.add(UserMapper.INSTANCE.entityToDTO(tMsUser));
        }

        for(UserDTO userDTO : userList) {

            final List<TMsUserRole> tRfUserRoleList = userRoleRepository
                    .findAllByUser_UserIdAndUsrlStatus(userDTO.getId(), STATUS_ACTIVE.getShortValue());

            userDTO.setRoles(tRfUserRoleList.stream().map(tRfUserRole ->
                    RoleMapper.INSTANCE.entityToDTO(tRfUserRole.getRole())).collect(Collectors.toList()));
            populateUserReferenceData(userDTO);
        }

        paginatedUserList.setTotalNoOfPages(tMsUserPage.getTotalPages());
        paginatedUserList.setTotalNoOfRecords(tMsUserPage.getTotalElements());
        paginatedUserList.setEntities(userList);

        return paginatedUserList;
    }

    @Override
    public List<TMsRoleFunction> getPermissionsByRole(Long roleId) {
        final List<TMsRoleFunction> tMsRoleFunctionList = roleFunctionRepository.findAllByRoleRoleId(roleId);
        return tMsRoleFunctionList;
    }

    @Override
    public Long removeUserById(Long userId) {

        TMsUser tMsUser = validateUserById(userId);

        return removeUser(tMsUser);
    }

    @Override
    public Boolean removeUserByPartyCode(String partyCode, Boolean isPartyValidated) {

        if (Strings.isNullOrEmpty(partyCode))
            throw new InvalidDataException("Party Code is required");

        if(!isPartyValidated) {
            final TMsParty tMsParty = partyRepository.findByPrtyCodeAndPrtyStatus(partyCode, STATUS_ACTIVE.getShortValue());

            if(tMsParty == null)
                throw new DataNotFoundException("Party not found for the Code : " + partyCode);
        }

        TMsUser tMsUser = userRepository.findByParty_PrtyCodeAndUserStatus(partyCode, STATUS_ACTIVE.getShortValue());

        if(tMsUser == null)
            return true;

        final Long userId = removeUser(tMsUser);

        if(userId != null)
            return true;
        else
            return false;
    }

    private Long removeUser(TMsUser tMsUser) {

        tMsUser.setUserStatus(Constants.STATUS_INACTIVE.getShortValue());

        final List<TMsUserRole> tMsUserRoleList = userRoleRepository
                .findAllByUser_UserIdAndUsrlStatus(tMsUser.getUserId(), STATUS_ACTIVE.getShortValue());

        tMsUserRoleList.forEach(tMsUserRole -> {
            tMsUserRole.setUsrlStatus(Constants.STATUS_INACTIVE.getShortValue());

            userRoleRepository.save(tMsUserRole);
        });

        List<TMsUserBranch> tMsUserBranchList = userBranchRepository
                .findAllByUser_UserIdAndUsbrStatus(tMsUser.getUserId(), STATUS_ACTIVE.getShortValue());

        tMsUserBranchList.forEach(tMsUserBranch -> {
            tMsUserBranch.setUsbrStatus(Constants.STATUS_INACTIVE.getShortValue());

            userBranchRepository.save(tMsUserBranch);
        });

        return persistEntity(tMsUser).getUserId();
    }

    @Transactional
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        final UserDTO userInDb = getUserByUsername(username);

        if(userInDb == null)
            throw new UsernameNotFoundException("User not found");
        else
            log.info("User {} found in the Database", username);

        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();

        final List<TMsUserRole> tRfUserRoleList = userRoleRepository
                .findAllByUser_UserIdAndUsrlStatus(userInDb.getId(), STATUS_ACTIVE.getShortValue());

        Collection<FunctionDTO> permittedFunctions = new ArrayList<>();

        tRfUserRoleList.forEach(tRfUserRole -> {
            authorities.add(new SimpleGrantedAuthority(tRfUserRole.getRole().getRoleName()));

            final List<TMsRoleFunction> functionList = getPermissionsByRole(tRfUserRole.getRole().getRoleId());

            functionList.forEach(tMsRoleFunction -> {
                permittedFunctions.add(
                        new FunctionDTO(
                                tMsRoleFunction.getFunction().getFuncId(),
                                tMsRoleFunction.getFunction().getDunsDescription(),
                                tMsRoleFunction.getRofuStatus()
                        )
                );
            });
        });

        List<Long> branchList = new ArrayList<>();
        final List<TMsUserBranch> userBranches = userBranchRepository
                .findAllByUser_UserIdAndUsbrStatus(userInDb.getId(), STATUS_ACTIVE.getShortValue());

        userBranches.forEach(tMsUserBranch -> {
            branchList.add(tMsUserBranch.getBranch().getBrnhId());
        });

        User user = new User();
        user.setId(userInDb.getId());
        user.setName(userInDb.getDisplayName());
        user.setPartyCode(userInDb.getPartyCode());
        user.setBranches(branchList);
        user.setUsername(userInDb.getUsername());
        user.setPassword(userInDb.getPassword());
        user.setAuthorities(authorities);
        user.setPermittedFunctions(permittedFunctions);

        return user;
    }

    private TMsUser persistEntity(TMsUser tMsUser) {
        try {
            validateEntity(tMsUser);
            return userRepository.save(tMsUser);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new TransactionConflictException("Transaction Updated by Another User.");
        } catch (Exception e) {
            log.error("Error while persisting : " + e.getMessage());
            throw new OperationException(e.getMessage());
        }
    }
}

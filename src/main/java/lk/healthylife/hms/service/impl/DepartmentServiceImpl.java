package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.config.AuditorAwareImpl;
import lk.healthylife.hms.config.EntityValidator;
import lk.healthylife.hms.config.repository.NumberGeneratorRepository;
import lk.healthylife.hms.dto.*;
import lk.healthylife.hms.exception.InvalidDataException;
import lk.healthylife.hms.exception.NoRequiredInfoException;
import lk.healthylife.hms.exception.OperationException;
import lk.healthylife.hms.service.DepartmentFacilityService;
import lk.healthylife.hms.service.DepartmentLocationService;
import lk.healthylife.hms.service.DepartmentService;
import lk.healthylife.hms.service.PartyService;
import lk.healthylife.hms.util.constant.CommonReferenceCodes;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;
import javax.transaction.Transactional;
import java.math.BigInteger;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static lk.healthylife.hms.util.constant.Constants.STATUS_ACTIVE;
import static lk.healthylife.hms.util.constant.Constants.STATUS_INACTIVE;

@Slf4j
@Service
public class DepartmentServiceImpl extends EntityValidator implements DepartmentService {

    private final DepartmentLocationService departmentLocationService;
    private final DepartmentFacilityService departmentFacilityService;
    private final PartyService partyService;

    private final NumberGeneratorRepository numberGeneratorRepository;

    private final AuditorAwareImpl auditorAware;
    private final DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    public DepartmentServiceImpl(DepartmentLocationService departmentLocationService,
                                 DepartmentFacilityService departmentFacilityService,
                                 PartyService partyService,
                                 NumberGeneratorRepository numberGeneratorRepository,
                                 AuditorAwareImpl auditorAware,
                                 DataSource dataSource) {
        this.departmentLocationService = departmentLocationService;
        this.departmentFacilityService = departmentFacilityService;
        this.partyService = partyService;
        this.numberGeneratorRepository = numberGeneratorRepository;
        this.auditorAware = auditorAware;
        this.dataSource = dataSource;
    }

    @Override
    public List<DepartmentDTO> getAllDepartmentsDropdown() {

        List<DepartmentDTO> departmentDTOList = new ArrayList<>();

        final String queryString = "SELECT DPMT_CODE, DPMT_NAME FROM T_MS_DEPARTMENT \n" +
                "WHERE DPMT_STATUS = :status \n" +
                "AND DPMT_BRANCH_ID IN (:branchIdList) \n" +
                "ORDER BY CREATED_DATE";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());

        List<Map<String,Object>> result = extractResultSet(query);

        if(result.size() == 0)
            return Collections.emptyList();

        for (Map<String,Object> department : result) {

            DepartmentDTO departmentDTO = new DepartmentDTO();

            departmentDTO.setDepartmentCode(extractValue(String.valueOf(department.get("DPMT_CODE"))));
            departmentDTO.setName(extractValue(String.valueOf(department.get("DPMT_NAME"))));

            departmentDTOList.add(departmentDTO);
        }

        return departmentDTOList;
    }

    @Transactional
    @Override
    public DepartmentDTO createDepartment(DepartmentDTO departmentDTO) {

        String departmentCode = null;
        validateEntity(departmentDTO);

        if(departmentDTO.getName().length() > 40 || departmentDTO.getName().length() < 5)
            throw new InvalidDataException("Department Name is invalid");

        if(!Strings.isNullOrEmpty(departmentDTO.getDepartmentHead())) {
            PartyDTO departmentHead = partyService.getPartyByPartyCode(departmentDTO.getDepartmentHead());

            if(!departmentHead.getType().equals(CommonReferenceCodes.PARTY_TYPE_DOCTOR.getValue()))
                throw new InvalidDataException("Department Head should be a Doctor");
        }

        try {
            departmentCode = numberGeneratorRepository.generateNumber("DP", "Y", "#", "#",
                    "#", "#", "#", "#");
        } catch (Exception e) {
            log.error("Error while creating a Department Code : " + e.getMessage());
            throw new OperationException("Error while creating a Department Code");
        }

        if(Strings.isNullOrEmpty(departmentCode))
            throw new OperationException("Department Code not generated");

        try (Connection connection = dataSource.getConnection()) {

            CallableStatement statement = connection
                    .prepareCall("{CALL INSERT INTO T_MS_DEPARTMENT (DPMT_CODE, DPMT_NAME, DPMT_DESCRIPTION, \n" +
                            "DPMT_HEAD, DPMT_STATUS, CREATED_DATE, CREATED_USER_CODE, DPMT_BRANCH_ID)\n" +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)}");

            statement.setString(1, departmentCode);
            statement.setString(2, departmentDTO.getName());
            statement.setString(3, Strings.isNullOrEmpty(departmentDTO.getDescription()) ? null : departmentDTO.getDescription());
            statement.setString(4, Strings.isNullOrEmpty(departmentDTO.getDepartmentHead()) ? null : departmentDTO.getDepartmentHead());
            statement.setShort(5, STATUS_ACTIVE.getShortValue());
            statement.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(7, auditorAware.getCurrentAuditor().get());
            statement.setLong(8, captureBranchIds().get(0));

            int updateCount = statement.executeUpdate();

        } catch (Exception e) {
            log.error("Error while persisting Department : " + e.getMessage());
            throw new OperationException("Error while persisting Department");
        }

        for(DepartmentLocationDTO departmentLocation : departmentDTO.getDepartmentLocations())
            departmentLocationService.addLocationToDepartment(departmentCode, departmentLocation);

        for(FacilityDTO facility : departmentDTO.getFacilities()) {
            DepartmentFacilityDTO departmentFacilityDTO = new DepartmentFacilityDTO();
            departmentFacilityDTO.setFacility(facility);

            departmentFacilityService.addFacilityToDepartment(departmentCode, departmentFacilityDTO);
        }

        return getDepartmentByCode(departmentCode);
    }

    @Transactional
    @Override
    public DepartmentDTO updateDepartment(String departmentCode, DepartmentDTO departmentDTO) {

        validateDepartmentCode(departmentCode);

        final Query query = entityManager.createNativeQuery("UPDATE T_MS_DEPARTMENT SET DPMT_NAME = :name, DPMT_DESCRIPTION = :description, \n" +
                        "DPMT_HEAD = :departmentHead, LAST_MOD_DATE = :lastModDate, LAST_MOD_USER_CODE = :lastModUser \n" +
                        "WHERE DPMT_CODE = :departmentCode AND DPMT_STATUS = :status AND DPMT_BRANCH_ID IN (:branchIdList)")
                .setParameter("name", departmentDTO.getName())
                .setParameter("description", departmentDTO.getDescription())
                .setParameter("departmentHead", departmentDTO.getDepartmentHead())
                .setParameter("lastModDate", LocalDateTime.now())
                .setParameter("lastModUser", auditorAware.getCurrentAuditor().get())
                .setParameter("departmentCode", departmentCode)
                .setParameter("status", STATUS_ACTIVE.getShortValue())
                .setParameter("branchIdList", captureBranchIds());

        query.executeUpdate();

        return getDepartmentByCode(departmentCode);
    }

    @Transactional
    @Override
    public Boolean removeDepartment(String departmentCode) {

        validateDepartmentCode(departmentCode);

        final Query query = entityManager.createNativeQuery("UPDATE T_MS_DEPARTMENT SET DPMT_STATUS = ? \n" +
                        "WHERE DPMT_CODE = ? AND DPMT_STATUS = ?")
                .setParameter(1, STATUS_INACTIVE.getShortValue())
                .setParameter(2, departmentCode)
                .setParameter(3, STATUS_ACTIVE.getShortValue());

        return query.executeUpdate() == 0 ? false : true;
    }

    @Override
    public PaginatedEntity departmentPaginatedSearch(String departmentCode, String departmentName, Integer page, Integer size) {

        PaginatedEntity paginatedDepartmentList = null;
        List<DepartmentDTO> departmentList = null;

        page = validatePaginateIndexes(page, size);

        departmentCode = departmentCode.isEmpty() ? null : departmentCode;

        String countQueryString = "SELECT COUNT(*) FROM T_MS_DEPARTMENT \n" +
                "WHERE DPMT_STATUS = :status \n" +
                "AND (:departmentCode IS NULL OR (:departmentCode IS NOT NULL) AND DPMT_CODE = :departmentCode) \n" +
                "AND (upper(DPMT_NAME) LIKE ('%'||upper(:departmentName)||'%')) \n" +
                "AND DPMT_BRANCH_ID IN (:branchIdList)";

        Query query = entityManager.createNativeQuery(countQueryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("departmentCode", departmentCode);
        query.setParameter("departmentName", departmentName);
        query.setParameter("branchIdList", captureBranchIds());

        final int selectedRecordCount = ((Number) query.getSingleResult()).intValue();

        query = null;

        final String queryString = "SELECT d.DPMT_CODE, d.DPMT_NAME, d.DPMT_DESCRIPTION, d.DPMT_HEAD, d.DPMT_STATUS, d.CREATED_DATE, \n" +
                "d.CREATED_USER_CODE, d.LAST_MOD_DATE, d.LAST_MOD_USER_CODE, p.PRTY_NAME AS DPMT_HEAD_NAME \n" +
                "FROM T_MS_DEPARTMENT d \n" +
                "LEFT JOIN T_MS_PARTY p ON d.DPMT_HEAD = p.PRTY_CODE \n" +
                "WHERE d.DPMT_STATUS = :status \n" +
                "AND (:departmentCode IS NULL OR (:departmentCode IS NOT NULL) AND d.DPMT_CODE = :departmentCode) \n" +
                "AND (upper(d.DPMT_NAME) LIKE ('%'||upper(:departmentName)||'%')) \n" +
                "AND d.DPMT_BRANCH_ID IN (:branchIdList) \n" +
                "ORDER BY d.CREATED_DATE OFFSET :page ROWS FETCH NEXT :size ROWS ONLY";

        query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("departmentCode", departmentCode);
        query.setParameter("departmentName", departmentName);
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("size", size);
        query.setParameter("page", size * page);

        List<Map<String,Object>> result = extractResultSet(query);

        paginatedDepartmentList = new PaginatedEntity();
        departmentList = new ArrayList<>();

        for (Map<String,Object> department : result) {

            DepartmentDTO departmentDTO = new DepartmentDTO();

            createDTO(departmentDTO, department);

            departmentList.add(departmentDTO);
        }

        paginatedDepartmentList
                .setTotalNoOfPages(getTotalNoOfPages(selectedRecordCount, size));
        paginatedDepartmentList.setTotalNoOfRecords(Long.valueOf(selectedRecordCount));
        paginatedDepartmentList.setEntities(departmentList);

        return paginatedDepartmentList;
    }

    @Override
    public DepartmentDTO getDepartmentByCode(String departmentCode) {

        DepartmentDTO departmentDTO = null;

        validateDepartmentCode(departmentCode);

        final String queryString = "SELECT d.DPMT_CODE, d.DPMT_NAME, d.DPMT_DESCRIPTION, d.DPMT_HEAD, d.DPMT_STATUS, d.CREATED_DATE,\n" +
                "d.CREATED_USER_CODE, d.LAST_MOD_DATE, d.LAST_MOD_USER_CODE, p.PRTY_NAME AS DPMT_HEAD_NAME \n" +
                "FROM T_MS_DEPARTMENT d \n" +
                "LEFT JOIN T_MS_PARTY p ON d.DPMT_HEAD = p.PRTY_CODE \n" +
                "WHERE d.DPMT_CODE = :departmentCode AND d.DPMT_STATUS = :status AND d.DPMT_BRANCH_ID IN (:branchIdList)";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("departmentCode", departmentCode);

        List<Map<String,Object>> result = extractResultSet(query);

        if (result.size() == 0)
            return null;

        for (Map<String,Object> department : result) {

            departmentDTO = new DepartmentDTO();

            createDTO(departmentDTO, department);

            departmentDTO.setDepartmentLocations(departmentLocationService.getDepartmentLocationsByDepartmentCode(departmentCode));
            departmentDTO.setDepartmentFacilities(departmentFacilityService.getDepartmentFacilitiesByDepartmentCode(departmentCode));
        }

        return departmentDTO;
    }

    private void createDTO(DepartmentDTO departmentDTO, Map<String,Object> department) {

        departmentDTO.setDepartmentCode(extractValue(String.valueOf(department.get("DPMT_CODE"))));
        departmentDTO.setName(extractValue(String.valueOf(department.get("DPMT_NAME"))));
        departmentDTO.setDescription(extractValue(String.valueOf(department.get("DPMT_DESCRIPTION"))));
        departmentDTO.setStatus(extractShortValue(String.valueOf(department.get("DPMT_STATUS"))));
        departmentDTO.setCreatedDate(extractDateTime(String.valueOf(department.get("CREATED_DATE"))));
        departmentDTO.setCreatedUserCode(extractValue(String.valueOf(department.get("CREATED_USER_CODE"))));
        departmentDTO.setLastUpdatedDate(extractDateTime(String.valueOf(department.get("LAST_MOD_DATE"))));
        departmentDTO.setLastUpdatedUserCode(extractValue(String.valueOf(department.get("LAST_MOD_USER_CODE"))));
        departmentDTO.setDepartmentHead(extractValue(String.valueOf(department.get("DPMT_HEAD"))));
        departmentDTO.setDepartmentHeadName(extractValue(String.valueOf(department.get("DPMT_HEAD_NAME"))));
    }

    private void validateDepartmentCode(String departmentCode) {
        if(Strings.isNullOrEmpty(departmentCode))
            throw new NoRequiredInfoException("Department Code is Required");
    }
}

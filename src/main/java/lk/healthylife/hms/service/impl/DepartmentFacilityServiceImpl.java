package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.config.AuditorAwareImpl;
import lk.healthylife.hms.config.EntityValidator;
import lk.healthylife.hms.dto.DepartmentDTO;
import lk.healthylife.hms.dto.DepartmentFacilityDTO;
import lk.healthylife.hms.dto.FacilityDTO;
import lk.healthylife.hms.exception.NoRequiredInfoException;
import lk.healthylife.hms.exception.OperationException;
import lk.healthylife.hms.service.DepartmentFacilityService;
import lombok.extern.slf4j.Slf4j;
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
public class DepartmentFacilityServiceImpl extends EntityValidator implements DepartmentFacilityService {

    private final AuditorAwareImpl auditorAware;
    private final DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    public DepartmentFacilityServiceImpl(AuditorAwareImpl auditorAware, DataSource dataSource, EntityManager entityManager) {
        this.auditorAware = auditorAware;
        this.dataSource = dataSource;
        this.entityManager = entityManager;
    }

    @Override
    public DepartmentFacilityDTO addFacilityToDepartment(String departmentCode, DepartmentFacilityDTO departmentFacilityDTO) {

        BigInteger insertedRowId = null;

        validateEntity(departmentFacilityDTO);

        try (Connection connection = dataSource.getConnection()) {

            CallableStatement statement = connection
                    .prepareCall("{CALL INSERT INTO T_MS_DEPARTMENT_FACILITY(DPFC_DEPARTMENT_CODE, DPFC_FACILITY_ID, \n" +
                    "DPFC_STATUS, DPFC_BRANCH_ID, CREATED_DATE, CREATED_USER_CODE)\n" +
                    "VALUES(?, ?, ?, ?, ?, ?) RETURNING DPFC_ID INTO ?}");

            statement.setString(1, departmentCode);
            statement.setLong(2, departmentFacilityDTO.getFacility().getFacilityId());
            statement.setShort(3, STATUS_ACTIVE.getShortValue());
            statement.setLong(4, captureBranchIds().get(0));
            statement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(6, auditorAware.getCurrentAuditor().get());

            statement.registerOutParameter( 7, Types.BIGINT);

            int updateCount = statement.executeUpdate();

            if (updateCount > 0)
                insertedRowId = BigInteger.valueOf(statement.getLong(7));

        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error while persisting Department Facility : " + e.getMessage());
            throw new OperationException("Error while persisting Department Facility");
        }

        return getDepartmentFacilityById(insertedRowId.longValue());
    }

    @Override
    public Boolean removeFacilityFromDepartment(String departmentCode, Long departmentFacilityId) {

        final Query query = entityManager.createNativeQuery("UPDATE T_MS_DEPARTMENT_FACILITY SET DPFC_STATUS = :statusInActive\n" +
                        "WHERE DPFC_STATUS = :statusActive AND DPFC_FACILITY_ID = :departmentFacilityId AND DPFC_BRANCH_ID IN (:branchIdList)")
                .setParameter("statusInActive", STATUS_INACTIVE.getShortValue())
                .setParameter("departmentFacilityId", departmentFacilityId)
                .setParameter("statusActive", STATUS_ACTIVE.getShortValue())
                .setParameter("branchIdList", captureBranchIds());

        return query.executeUpdate() == 0 ? false : true;
    }

    @Override
    public DepartmentFacilityDTO getDepartmentFacilityById(Long departmentFacilityId) {

        DepartmentFacilityDTO departmentFacilityDTO = null;

        validateDepartmentFacilityId(departmentFacilityId);

        final String queryString = "SELECT dl.DPFC_ID, dl.DPFC_DEPARTMENT_CODE, dl.DPFC_FACILITY_ID, dl.DPFC_STATUS, dl.DPFC_BRANCH_ID,\n" +
                "dl.CREATED_DATE, dl.LAST_MOD_DATE, dl.CREATED_USER_CODE, dl.LAST_MOD_USER_CODE, br.BRNH_NAME AS BRANCH_NAME\n" +
                "FROM T_MS_DEPARTMENT_FACILITY dl\n" +
                "INNER JOIN T_RF_BRANCH br ON dl.DPFC_BRANCH_ID = br.BRNH_ID\n" +
                "WHERE dl.DPFC_ID = :departmentFacilityId AND dl.DPFC_STATUS = :status AND dl.DPFC_BRANCH_ID IN (:branchIdList)";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("departmentFacilityId", departmentFacilityId);

        List<Map<String,Object>> result = extractResultSet(query);

        if (result.size() == 0)
            return null;

        for (Map<String,Object> departmentFacility : result) {

            departmentFacilityDTO = new DepartmentFacilityDTO();

            createDTO(departmentFacilityDTO, departmentFacility);
        }

        return departmentFacilityDTO;
    }

    @Override
    public List<DepartmentFacilityDTO> getDepartmentFacilitiesByDepartmentCode(String departmentCode) {

        List<DepartmentFacilityDTO> departmentFacilityList = new ArrayList<>();

        final String queryString = "SELECT dl.DPFC_ID, dl.DPFC_DEPARTMENT_CODE, dl.DPFC_FACILITY_ID, dl.DPFC_STATUS, dl.DPFC_BRANCH_ID,\n" +
                "dl.CREATED_DATE, dl.LAST_MOD_DATE, dl.CREATED_USER_CODE, dl.LAST_MOD_USER_CODE, br.BRNH_NAME AS BRANCH_NAME\n" +
                "FROM T_MS_DEPARTMENT_FACILITY dl\n" +
                "INNER JOIN T_RF_BRANCH br ON dl.DPFC_BRANCH_ID = br.BRNH_ID\n" +
                "WHERE dl.DPFC_STATUS = :status AND dl.DPFC_DEPARTMENT_CODE = :departmentCode AND dl.DPFC_BRANCH_ID IN (:branchIdList)";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("departmentCode", departmentCode);
        query.setParameter("branchIdList", captureBranchIds());

        List<Map<String,Object>> result = extractResultSet(query);

        if(result.size() == 0)
            return Collections.emptyList();

        for (Map<String,Object> departmentFacility : result) {

            DepartmentFacilityDTO departmentFacilityDTO = new DepartmentFacilityDTO();

            createDTO(departmentFacilityDTO, departmentFacility);

            departmentFacilityList.add(departmentFacilityDTO);
        }

        return departmentFacilityList;
    }

    private void validateDepartmentFacilityId(Long departmentFacilityId) {
        if(departmentFacilityId == null)
            throw new NoRequiredInfoException("Department Facility Id is Required");
    }

    private void createDTO(DepartmentFacilityDTO departmentFacilityDTO, Map<String,Object> departmentFacility) {

        departmentFacilityDTO.setDepartmentFacilityId(extractLongValue(String.valueOf(departmentFacility.get("DPFC_ID"))));
        departmentFacilityDTO.setFacility(new FacilityDTO(extractLongValue(String.valueOf(departmentFacility.get("DPFC_FACILITY_ID")))));
        departmentFacilityDTO.setDepartment(new DepartmentDTO(extractValue(String.valueOf(departmentFacility.get("DPFC_DEPARTMENT_CODE")))));
        departmentFacilityDTO.setStatus(extractShortValue(String.valueOf(departmentFacility.get("DPFC_STATUS"))));
        departmentFacilityDTO.setCreatedDate(extractDateTime(String.valueOf(departmentFacility.get("CREATED_DATE"))));
        departmentFacilityDTO.setCreatedUserCode(extractValue(String.valueOf(departmentFacility.get("CREATED_USER_CODE"))));
        departmentFacilityDTO.setLastUpdatedDate(extractDateTime(String.valueOf(departmentFacility.get("LAST_MOD_DATE"))));
        departmentFacilityDTO.setLastUpdatedUserCode(extractValue(String.valueOf(departmentFacility.get("LAST_MOD_USER_CODE"))));
        departmentFacilityDTO.setBranchId(extractLongValue(String.valueOf(departmentFacility.get("DPFC_BRANCH_ID"))));
        departmentFacilityDTO.setBranchName(extractValue(String.valueOf(departmentFacility.get("BRANCH_NAME"))));
    }
}

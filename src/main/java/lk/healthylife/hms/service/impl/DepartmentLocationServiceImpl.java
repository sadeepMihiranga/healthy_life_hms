package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.config.AuditorAwareImpl;
import lk.healthylife.hms.config.EntityValidator;
import lk.healthylife.hms.dto.DepartmentDTO;
import lk.healthylife.hms.dto.DepartmentLocationDTO;
import lk.healthylife.hms.dto.RoomDTO;
import lk.healthylife.hms.exception.NoRequiredInfoException;
import lk.healthylife.hms.exception.OperationException;
import lk.healthylife.hms.service.DepartmentLocationService;
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
public class DepartmentLocationServiceImpl extends EntityValidator implements DepartmentLocationService {

    private final AuditorAwareImpl auditorAware;
    private final DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    public DepartmentLocationServiceImpl(AuditorAwareImpl auditorAware,
                                         DataSource dataSource) {
        this.auditorAware = auditorAware;
        this.dataSource = dataSource;
    }

    @Override
    public DepartmentLocationDTO addLocationToDepartment(String departmentCode, DepartmentLocationDTO departmentLocationDTO) {

        BigInteger insertedRowId = null;

        try (Connection connection = dataSource.getConnection()) {

            CallableStatement statement = connection.prepareCall("{CALL INSERT INTO T_RF_DEPARTMENT_LOCATION (DPLC_NAME, " +
                    "DPLC_ROOM_ID, DPLC_DEPARTMENT_CODE, CREATED_DATE, CREATED_USER_CODE, DPLC_STATUS, DPLC_BRANCH_ID)\n" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING DPLC_ID INTO ?}");

            statement.setString(1, Strings.isNullOrEmpty(departmentLocationDTO.getName()) ? null : departmentLocationDTO.getName());
            statement.setLong(2, departmentLocationDTO.getRoom().getRoomId());
            statement.setString(3, departmentCode);
            statement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(5, auditorAware.getCurrentAuditor().get());
            statement.setShort(6, STATUS_ACTIVE.getShortValue());
            statement.setLong(7, captureBranchIds().get(0));

            statement.registerOutParameter( 8, Types.BIGINT);

            int updateCount = statement.executeUpdate();

            if (updateCount > 0)
                insertedRowId = BigInteger.valueOf(statement.getLong(8));

        } catch (Exception e) {
            log.error("Error while persisting Department Location : " + e.getMessage());
            throw new OperationException("Error while persisting Department Location");
        }

        return getDepartmentLocationById(insertedRowId.longValue());
    }

    @Transactional
    @Override
    public Boolean removeLocationFromDepartment(String departmentCode, Long departmentLocationId) {

        final Query query = entityManager.createNativeQuery("UPDATE T_RF_DEPARTMENT_LOCATION SET DPLC_STATUS = :statusInActive \n" +
                        "WHERE DPLC_ID = :departmentLocationId AND DPLC_STATUS = :statusActive AND DPLC_BRANCH_ID IN (:branchIdList)")
                .setParameter("statusInActive", STATUS_INACTIVE.getShortValue())
                .setParameter("departmentLocationId", departmentLocationId)
                .setParameter("statusActive", STATUS_ACTIVE.getShortValue())
                .setParameter("branchIdList", captureBranchIds());

        return query.executeUpdate() == 0 ? false : true;
    }

    @Override
    public DepartmentLocationDTO getDepartmentLocationById(Long departmentLocationId) {

        DepartmentLocationDTO departmentLocationDTO = null;

        validateDepartmentLocationId(departmentLocationId);

        final String queryString = "SELECT DPLC_ID, DPLC_NAME, DPLC_ROOM_ID, DPLC_DEPARTMENT_CODE, CREATED_DATE,\n" +
                "CREATED_USER_CODE, DPLC_STATUS, LAST_MOD_DATE, LAST_MOD_USER_CODE, DPLC_BRANCH_ID\n" +
                "FROM T_RF_DEPARTMENT_LOCATION WHERE DPLC_ID = :departmentLocationId AND DPLC_STATUS = :status AND DPLC_BRANCH_ID IN (:branchIdList)";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("departmentLocationId", departmentLocationId);

        List<Map<String,Object>> result = extractResultSet(query);

        if (result.size() == 0)
            return null;

        for (Map<String,Object> departmentLocation : result) {

            departmentLocationDTO = new DepartmentLocationDTO();

            createDTO(departmentLocationDTO, departmentLocation);
        }

        return departmentLocationDTO;
    }

    @Override
    public List<DepartmentLocationDTO> getDepartmentLocationsByDepartmentCode(String departmentCode) {

        List<DepartmentLocationDTO> departmentLocationList = new ArrayList<>();

        final String queryString = "SELECT DPLC_ID, DPLC_NAME, DPLC_ROOM_ID, DPLC_DEPARTMENT_CODE, CREATED_DATE,\n" +
                "CREATED_USER_CODE, DPLC_STATUS, LAST_MOD_DATE, LAST_MOD_USER_CODE, DPLC_BRANCH_ID\n" +
                "FROM T_RF_DEPARTMENT_LOCATION WHERE DPLC_STATUS = :status AND DPLC_BRANCH_ID IN (:branchIdList)";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());

        List<Map<String,Object>> result = extractResultSet(query);

        if(result.size() == 0)
            return Collections.emptyList();

        for (Map<String,Object> departmentLocation : result) {

            DepartmentLocationDTO departmentLocationDTO = new DepartmentLocationDTO();

            createDTO(departmentLocationDTO, departmentLocation);

            departmentLocationList.add(departmentLocationDTO);
        }

        return departmentLocationList;
    }

    private void validateDepartmentLocationId(Long departmentLocationId) {
        if(departmentLocationId == null)
            throw new NoRequiredInfoException("Department Location Id is Required");
    }

    private void createDTO(DepartmentLocationDTO departmentLocationDTO, Map<String,Object> departmentLocation) {

        departmentLocationDTO.setDepartmentLocationId(extractLongValue(String.valueOf(departmentLocation.get("DPLC_ID"))));
        departmentLocationDTO.setName(extractValue(String.valueOf(departmentLocation.get("DPLC_NAME"))));
        departmentLocationDTO.setRoom(new RoomDTO(extractLongValue(String.valueOf(departmentLocation.get("DPLC_ROOM_ID")))));
        departmentLocationDTO.setDepartment(new DepartmentDTO(extractValue(String.valueOf(departmentLocation.get("DPLC_DEPARTMENT_CODE")))));
        departmentLocationDTO.setStatus(extractShortValue(String.valueOf(departmentLocation.get("DPLC_STATUS"))));
        departmentLocationDTO.setCreatedDate(extractDateTime(String.valueOf(departmentLocation.get("CREATED_DATE"))));
        departmentLocationDTO.setCreatedUserCode(extractValue(String.valueOf(departmentLocation.get("CREATED_USER_CODE"))));
        departmentLocationDTO.setLastUpdatedDate(extractDateTime(String.valueOf(departmentLocation.get("LAST_MOD_DATE"))));
        departmentLocationDTO.setLastUpdatedUserCode(extractValue(String.valueOf(departmentLocation.get("LAST_MOD_USER_CODE"))));
        departmentLocationDTO.setBranchId(extractLongValue(String.valueOf(departmentLocation.get("DPLC_BRANCH_ID"))));
    }
}

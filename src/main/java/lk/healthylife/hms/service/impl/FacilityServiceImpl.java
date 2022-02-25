package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.config.AuditorAwareImpl;
import lk.healthylife.hms.config.EntityValidator;
import lk.healthylife.hms.dto.FacilityDTO;
import lk.healthylife.hms.dto.PaginatedEntity;
import lk.healthylife.hms.exception.NoRequiredInfoException;
import lk.healthylife.hms.exception.OperationException;
import lk.healthylife.hms.service.FacilityService;
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
public class FacilityServiceImpl extends EntityValidator implements FacilityService {

    private final AuditorAwareImpl auditorAware;
    private final DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    public FacilityServiceImpl(AuditorAwareImpl auditorAware,
                               DataSource dataSource) {
        this.auditorAware = auditorAware;
        this.dataSource = dataSource;
    }

    @Override
    public List<FacilityDTO> getAllFacilitiesDropdown() {

        List<FacilityDTO> facilityList = new ArrayList<>();

        final String queryString = "SELECT FCLT_ID, FCLT_NAME\n" +
                "FROM T_MS_FACILITY\n" +
                "WHERE FCLT_STATUS = :status AND FCLT_BRANCH_ID IN (:branchIdList)\n" +
                "ORDER BY CREATED_DATE";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());

        List<Map<String,Object>> result = extractResultSet(query);

        if(result.size() == 0)
            return Collections.emptyList();

        for (Map<String,Object> room : result) {

            FacilityDTO facilityDTO = new FacilityDTO();

            facilityDTO.setFacilityId(extractLongValue(String.valueOf(room.get("FCLT_ID"))));
            facilityDTO.setName(extractValue(String.valueOf(room.get("FCLT_NAME"))));

            facilityList.add(facilityDTO);
        }

        return facilityList;
    }

    @Transactional
    @Override
    public FacilityDTO createFacility(FacilityDTO facilityDTO) {

        BigInteger insertedRowId = null;

        validateEntity(facilityDTO);

        try (Connection connection = dataSource.getConnection()) {

            CallableStatement statement = connection
                    .prepareCall("{CALL INSERT INTO T_MS_FACILITY(FCLT_NAME, FCLT_DESCRIPTION, FCLT_STATUS,\n" +
                            "CREATED_DATE, CREATED_USER_CODE, FCLT_BRANCH_ID) VALUES (?, ?, ?, ?, ?, ?) RETURNING FCLT_ID INTO ?}");

            statement.setString(1, facilityDTO.getName());
            statement.setString(2, facilityDTO.getDescription());
            statement.setShort(3, STATUS_ACTIVE.getShortValue());
            statement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(5, auditorAware.getCurrentAuditor().get());
            statement.setLong(6, captureBranchIds().get(0));

            statement.registerOutParameter( 7, Types.BIGINT);

            int updateCount = statement.executeUpdate();

            if (updateCount > 0)
                insertedRowId = BigInteger.valueOf(statement.getLong(7));

        } catch (Exception e) {
            log.error("Error while persisting Facility : " + e.getMessage());
            throw new OperationException("Error while persisting Facility");
        }

        return getFacilityById(insertedRowId.longValue());
    }

    @Transactional
    @Override
    public FacilityDTO updateFacility(Long facilityId, FacilityDTO facilityDTO) {

        validateFacilityId(facilityId);

        validateEntity(facilityDTO);

        final Query query = entityManager
                .createNativeQuery("UPDATE T_MS_FACILITY SET FCLT_NAME = :name, FCLT_DESCRIPTION = :description," +
                        "LAST_MOD_DATE = :lastModDate, LAST_MOD_USER_CODE = :lastModUser\n" +
                        "WHERE FCLT_ID = :facilityId AND FCLT_STATUS = :status AND FCLT_BRANCH_ID IN (:branchIdList)")
                .setParameter("name", facilityDTO.getName())
                .setParameter("description", facilityDTO.getDescription())
                .setParameter("lastModDate", LocalDateTime.now())
                .setParameter("lastModUser", auditorAware.getCurrentAuditor().get())
                .setParameter("status", STATUS_ACTIVE.getShortValue())
                .setParameter("facilityId", facilityId)
                .setParameter("branchIdList", captureBranchIds());

        query.executeUpdate();

        return getFacilityById(facilityId);
    }

    @Override
    public FacilityDTO getFacilityById(Long facilityId) {

        FacilityDTO facilityDTO = null;

        validateFacilityId(facilityId);

        final String queryString = "SELECT fc.FCLT_ID, fc.FCLT_NAME, fc.FCLT_DESCRIPTION, fc.FCLT_STATUS, fc.CREATED_DATE,\n" +
                "fc.CREATED_USER_CODE, fc.FCLT_BRANCH_ID, fc.LAST_MOD_DATE, fc.LAST_MOD_USER_CODE, br.BRNH_NAME AS BRANCH_NAME\n" +
                "FROM T_MS_FACILITY fc\n" +
                "INNER JOIN T_RF_BRANCH br ON fc.FCLT_BRANCH_ID = br.BRNH_ID\n" +
                "WHERE fc.FCLT_ID = :facilityId AND fc.FCLT_STATUS = :status AND fc.FCLT_BRANCH_ID IN (:branchIdList)";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("facilityId", facilityId);

        List<Map<String,Object>> result = extractResultSet(query);

        if (result.size() == 0)
            return null;

        for (Map<String,Object> facility : result) {

            facilityDTO = new FacilityDTO();

            createDTO(facilityDTO, facility);
        }

        return facilityDTO;
    }

    @Override
    public PaginatedEntity facilityPaginatedSearch(String facilityName, Integer page, Integer size) {

        PaginatedEntity paginatedFacilityList = null;
        List<FacilityDTO> facilityList = null;

        validatePaginateIndexes(page, size);
        page = page == 1 ? 0 : page;

        final String countQueryString = "SELECT COUNT(FCLT_ID)\n" +
                "FROM T_MS_FACILITY \n" +
                "WHERE FCLT_STATUS = :status AND FCLT_BRANCH_ID IN (:branchIdList)\n" +
                "AND (upper(FCLT_NAME) LIKE ('%'||upper(:facilityName)||'%'))";

        Query query = entityManager.createNativeQuery(countQueryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("facilityName", facilityName);
        query.setParameter("branchIdList", captureBranchIds());

        final int selectedRecordCount = ((Number) query.getSingleResult()).intValue();

        query = null;

        final String queryString = "SELECT fc.FCLT_ID, fc.FCLT_NAME, fc.FCLT_DESCRIPTION, fc.FCLT_STATUS, fc.CREATED_DATE,\n" +
                "fc.CREATED_USER_CODE, fc.FCLT_BRANCH_ID, fc.LAST_MOD_DATE, fc.LAST_MOD_USER_CODE, br.BRNH_NAME AS BRANCH_NAME\n" +
                "FROM T_MS_FACILITY fc\n" +
                "INNER JOIN T_RF_BRANCH br ON fc.FCLT_BRANCH_ID = br.BRNH_ID\n" +
                "WHERE fc.FCLT_STATUS = :status AND fc.FCLT_BRANCH_ID IN (:branchIdList)\n" +
                "AND (upper(fc.FCLT_NAME) LIKE ('%'||upper(:facilityName)||'%'))\n" +
                "ORDER BY fc.CREATED_DATE OFFSET :page ROWS FETCH NEXT :size ROWS ONLY";

        query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("facilityName", facilityName);
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("size", size);
        query.setParameter("page", size * page);

        List<Map<String,Object>> result = extractResultSet(query);

        paginatedFacilityList = new PaginatedEntity();
        facilityList = new ArrayList<>();

        for (Map<String,Object> facility : result) {

            FacilityDTO facilityDTO = new FacilityDTO();

            createDTO(facilityDTO, facility);

            facilityList.add(facilityDTO);
        }

        paginatedFacilityList
                .setTotalNoOfPages(selectedRecordCount == 0 ? 0 : selectedRecordCount < size ? 1 : selectedRecordCount / size);
        paginatedFacilityList.setTotalNoOfRecords(Long.valueOf(selectedRecordCount));
        paginatedFacilityList.setEntities(facilityList);

        return paginatedFacilityList;
    }

    @Transactional
    @Override
    public Boolean removeFacility(Long facilityId) {

        validateFacilityId(facilityId);

        final Query query = entityManager.createNativeQuery("UPDATE T_MS_FACILITY SET FCLT_STATUS = :statusInActive\n" +
                        "WHERE FCLT_ID = :facilityId AND FCLT_STATUS = :statusActive AND FCLT_BRANCH_ID IN (:branchIdList)")
                .setParameter("statusInActive", STATUS_INACTIVE.getShortValue())
                .setParameter("facilityId", facilityId)
                .setParameter("statusActive", STATUS_ACTIVE.getShortValue())
                .setParameter("branchIdList", captureBranchIds());

        return query.executeUpdate() == 0 ? false : true;
    }

    private void validateFacilityId(Long facilityId) {
        if(facilityId == null)
            throw new NoRequiredInfoException("Facility Id is Required");
    }

    private void createDTO(FacilityDTO facilityDTO, Map<String,Object> facility) {

        facilityDTO.setFacilityId(extractLongValue(String.valueOf(facility.get("FCLT_ID"))));
        facilityDTO.setName(extractValue(String.valueOf(facility.get("FCLT_NAME"))));
        facilityDTO.setDescription(extractValue(String.valueOf(facility.get("FCLT_DESCRIPTION"))));
        facilityDTO.setStatus(extractShortValue(String.valueOf(facility.get("FCLT_STATUS"))));
        facilityDTO.setCreatedDate(extractDateTime(String.valueOf(facility.get("CREATED_DATE"))));
        facilityDTO.setCreatedUserCode(extractValue(String.valueOf(facility.get("CREATED_USER_CODE"))));
        facilityDTO.setLastUpdatedDate(extractDateTime(String.valueOf(facility.get("LAST_MOD_DATE"))));
        facilityDTO.setLastUpdatedUserCode(extractValue(String.valueOf(facility.get("LAST_MOD_USER_CODE"))));
        facilityDTO.setBranchId(extractLongValue(String.valueOf(facility.get("FCLT_BRANCH_ID"))));
        facilityDTO.setBranchName(extractValue(String.valueOf(facility.get("BRANCH_NAME"))));
    }
}

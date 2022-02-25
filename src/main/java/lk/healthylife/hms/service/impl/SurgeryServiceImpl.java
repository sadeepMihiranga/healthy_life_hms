package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.config.AuditorAwareImpl;
import lk.healthylife.hms.config.EntityValidator;
import lk.healthylife.hms.dto.PaginatedEntity;
import lk.healthylife.hms.dto.SurgeryDTO;
import lk.healthylife.hms.exception.NoRequiredInfoException;
import lk.healthylife.hms.exception.OperationException;
import lk.healthylife.hms.service.CommonReferenceService;
import lk.healthylife.hms.service.SurgeryService;
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

import static lk.healthylife.hms.util.constant.CommonReferenceTypeCodes.SURGERY_TYPES;
import static lk.healthylife.hms.util.constant.Constants.STATUS_ACTIVE;
import static lk.healthylife.hms.util.constant.Constants.STATUS_INACTIVE;

@Slf4j
@Service
public class SurgeryServiceImpl  extends EntityValidator implements SurgeryService {

    private final CommonReferenceService commonReferenceService;

    private final AuditorAwareImpl auditorAware;
    private final DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    public SurgeryServiceImpl(CommonReferenceService commonReferenceService,
                              AuditorAwareImpl auditorAware,
                              DataSource dataSource,
                              EntityManager entityManager) {
        this.commonReferenceService = commonReferenceService;
        this.auditorAware = auditorAware;
        this.dataSource = dataSource;
        this.entityManager = entityManager;
    }

    @Override
    public List<SurgeryDTO> getAllSurgeriesDropdown() {

        List<SurgeryDTO> surgeryDTOList = new ArrayList<>();

        final String queryString = "SELECT SRGY_ID, SRGY_NAME FROM T_MS_SURGERY WHERE SRGY_STATUS = :status" +
                " ORDER BY CREATED_DATE";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());

        List<Map<String,Object>> result = extractResultSet(query);

        if(result.size() == 0)
            return Collections.emptyList();

        for (Map<String,Object> room : result) {

            SurgeryDTO surgeryDTO = new SurgeryDTO();

            surgeryDTO.setSurgeryId(extractLongValue(String.valueOf(room.get("SRGY_ID"))));
            surgeryDTO.setName(extractValue(String.valueOf(room.get("SRGY_NAME"))));

            surgeryDTOList.add(surgeryDTO);
        }

        return surgeryDTOList;
    }

    @Override
    public SurgeryDTO createSurgery(SurgeryDTO surgeryDTO) {

        BigInteger insertedRowId = null;

        validateEntity(surgeryDTO);

        validateSurgeryReferenceDetailsOnPersist(surgeryDTO);

        try (Connection connection = dataSource.getConnection()) {

            CallableStatement statement = connection
                    .prepareCall("{CALL INSERT INTO T_MS_SURGERY(SRGY_TYPE, SRGY_NAME, SRGY_DESCRIPTION, SRGY_FEE,\n" +
                            "SRGY_ESTIMATED_TIME, SRGY_STATUS, CREATED_DATE, CREATED_USER_CODE)\n" +
                            "VALUES(?, ?, ?, ?, ?, ?, ?, ?) RETURNING SRGY_ID INTO ?}");

            statement.setString(1, surgeryDTO.getType());
            statement.setString(2, surgeryDTO.getName());
            statement.setString(3, surgeryDTO.getDescription());
            statement.setBigDecimal(4, surgeryDTO.getFee());
            statement.setBigDecimal(5, surgeryDTO.getEstimatedTimeInHours());
            statement.setShort(6, STATUS_ACTIVE.getShortValue());
            statement.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(8, auditorAware.getCurrentAuditor().get());

            statement.registerOutParameter(9, Types.BIGINT);

            int updateCount = statement.executeUpdate();

            if (updateCount > 0)
                insertedRowId = BigInteger.valueOf(statement.getLong(9));

        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error while persisting Surgery : " + e.getMessage());
            throw new OperationException("Error while persisting Surgery");
        }

        return getSurgeryById(insertedRowId.longValue());
    }

    @Override
    public SurgeryDTO getSurgeryById(Long surgeryId) {

        SurgeryDTO surgeryDTO = null;

        validateSurgeryId(surgeryId);

        final String queryString = "SELECT s.SRGY_ID, s.SRGY_TYPE, s.SRGY_NAME, s.SRGY_DESCRIPTION, s.SRGY_FEE, s.SRGY_ESTIMATED_TIME,\n" +
                "s.SRGY_STATUS, s.CREATED_DATE, s.CREATED_USER_CODE, type.CMRF_DESCRIPTION AS TYPE_NAME, s.LAST_MOD_DATE, s.LAST_MOD_USER_CODE\n" +
                "FROM T_MS_SURGERY s\n" +
                "LEFT JOIN T_RF_COMMON_REFERENCE type ON s.SRGY_TYPE = type.CMRF_CODE\n" +
                "WHERE s.SRGY_ID = :surgeryId AND s.SRGY_STATUS = :status";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("surgeryId", surgeryId);

        List<Map<String,Object>> result = extractResultSet(query);

        if (result.size() == 0)
            return null;

        for (Map<String,Object> facility : result) {

            surgeryDTO = new SurgeryDTO();

            createDTO(surgeryDTO, facility);
        }

        return surgeryDTO;
    }

    @Override
    public PaginatedEntity surgeryPaginatedSearch(String name, String type, Integer page, Integer size) {

        PaginatedEntity paginatedSurgeryList = null;
        List<SurgeryDTO> surgeryList = null;

        validatePaginateIndexes(page, size);
        page = page == 1 ? 0 : page;

        final String countQueryString = "SELECT COUNT(SRGY_ID)\n" +
                "FROM T_MS_SURGERY \n" +
                "WHERE SRGY_STATUS = :status\n" +
                "AND (upper(SRGY_NAME) LIKE ('%'||upper(:name)||'%'))\n" +
                "AND (:type IS NULL OR (:type IS NOT NULL) AND SRGY_TYPE = :type)";

        Query query = entityManager.createNativeQuery(countQueryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("name", name);
        query.setParameter("type", type);

        final int selectedRecordCount = ((Number) query.getSingleResult()).intValue();

        query = null;

        final String queryString = "SELECT s.SRGY_ID, s.SRGY_TYPE, s.SRGY_NAME, s.SRGY_DESCRIPTION, s.SRGY_FEE, s.SRGY_ESTIMATED_TIME,\n" +
                "s.SRGY_STATUS, s.CREATED_DATE, s.CREATED_USER_CODE, type.CMRF_DESCRIPTION AS TYPE_NAME, s.LAST_MOD_DATE, s.LAST_MOD_USER_CODE \n" +
                "FROM T_MS_SURGERY s\n" +
                "LEFT JOIN T_RF_COMMON_REFERENCE type ON s.SRGY_TYPE = type.CMRF_CODE\n" +
                "WHERE s.SRGY_STATUS = :status\n" +
                "AND (upper(s.SRGY_NAME) LIKE ('%'||upper(:name)||'%'))\n" +
                "AND (:type IS NULL OR (:type IS NOT NULL) AND s.SRGY_TYPE = :type)\n" +
                "ORDER BY s.CREATED_DATE OFFSET :page ROWS FETCH NEXT :size ROWS ONLY";

        query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("name", name);
        query.setParameter("type", type);
        query.setParameter("size", size);
        query.setParameter("page", size * page);

        List<Map<String,Object>> result = extractResultSet(query);

        paginatedSurgeryList = new PaginatedEntity();
        surgeryList = new ArrayList<>();

        for (Map<String,Object> surgery : result) {

            SurgeryDTO surgeryDTO = new SurgeryDTO();

            createDTO(surgeryDTO, surgery);

            surgeryList.add(surgeryDTO);
        }

        paginatedSurgeryList
                .setTotalNoOfPages(selectedRecordCount == 0 ? 0 : selectedRecordCount < size ? 1 : selectedRecordCount / size);
        paginatedSurgeryList.setTotalNoOfRecords(Long.valueOf(selectedRecordCount));
        paginatedSurgeryList.setEntities(surgeryList);

        return paginatedSurgeryList;
    }

    @Transactional
    @Override
    public Boolean removeSurgery(Long surgeryId) {

        validateSurgeryId(surgeryId);

        final Query query = entityManager.createNativeQuery("UPDATE T_MS_SURGERY SET SRGY_STATUS = :statusInActive\n" +
                        "WHERE SRGY_ID = :surgeryId AND SRGY_STATUS = :statusActive")
                .setParameter("statusInActive", STATUS_INACTIVE.getShortValue())
                .setParameter("surgeryId", surgeryId)
                .setParameter("statusActive", STATUS_ACTIVE.getShortValue());

        return query.executeUpdate() == 0 ? false : true;
    }

    @Transactional
    @Override
    public SurgeryDTO updateSurgery(Long surgeryId, SurgeryDTO surgeryDTO) {

        validateEntity(surgeryDTO);

        validateSurgeryId(surgeryId);

        final Query query = entityManager
                .createNativeQuery("UPDATE T_MS_SURGERY SET SRGY_TYPE = :type, SRGY_NAME = :name, SRGY_DESCRIPTION = :description,\n" +
                        "SRGY_FEE = :fee, SRGY_ESTIMATED_TIME = :estimatedTime, LAST_MOD_DATE = :lastModDate, LAST_MOD_USER_CODE = :lastModUser \n" +
                        "WHERE SRGY_ID = :surgeryId AND SRGY_STATUS = :status")
                .setParameter("type", surgeryDTO.getType())
                .setParameter("description", surgeryDTO.getDescription())
                .setParameter("name", surgeryDTO.getName())
                .setParameter("fee", surgeryDTO.getFee())
                .setParameter("estimatedTime", surgeryDTO.getEstimatedTimeInHours())
                .setParameter("lastModDate", LocalDateTime.now())
                .setParameter("lastModUser", auditorAware.getCurrentAuditor().get())
                .setParameter("status", STATUS_ACTIVE.getShortValue())
                .setParameter("surgeryId", surgeryId);

        query.executeUpdate();

        return getSurgeryById(surgeryId);
    }

    private void validateSurgeryReferenceDetailsOnPersist(SurgeryDTO surgeryDTO) {

        if(surgeryDTO.getType() != null)
            commonReferenceService
                    .getByCmrfCodeAndCmrtCode(SURGERY_TYPES.getValue(), surgeryDTO.getType());
    }

    private void validateSurgeryId(Long surgeryId) {
        if(surgeryId == null)
            throw new NoRequiredInfoException("Surgery Id is Required");
    }

    private void createDTO(SurgeryDTO surgeryDTO, Map<String,Object> surgery) {

        surgeryDTO.setSurgeryId(extractLongValue(String.valueOf(surgery.get("SRGY_ID"))));
        surgeryDTO.setName(extractValue(String.valueOf(surgery.get("SRGY_NAME"))));
        surgeryDTO.setDescription(extractValue(String.valueOf(surgery.get("SRGY_DESCRIPTION"))));
        surgeryDTO.setType(extractValue(String.valueOf(surgery.get("SRGY_TYPE"))));
        surgeryDTO.setFee(extractDecimalValue(String.valueOf(surgery.get("SRGY_FEE"))));
        surgeryDTO.setEstimatedTimeInHours(extractDecimalValue(String.valueOf(surgery.get("SRGY_ESTIMATED_TIME"))));
        surgeryDTO.setStatus(extractShortValue(String.valueOf(surgery.get("SRGY_STATUS"))));
        surgeryDTO.setCreatedDate(extractDateTime(String.valueOf(surgery.get("CREATED_DATE"))));
        surgeryDTO.setCreatedUserCode(extractValue(String.valueOf(surgery.get("CREATED_USER_CODE"))));
        surgeryDTO.setLastUpdatedDate(extractDateTime(String.valueOf(surgery.get("LAST_MOD_DATE"))));
        surgeryDTO.setLastUpdatedUserCode(extractValue(String.valueOf(surgery.get("LAST_MOD_USER_CODE"))));
    }
}

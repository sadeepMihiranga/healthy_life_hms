package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.config.AuditorAwareImpl;
import lk.healthylife.hms.config.EntityValidator;
import lk.healthylife.hms.dto.MedicalTestDTO;
import lk.healthylife.hms.dto.MedicineDTO;
import lk.healthylife.hms.dto.PaginatedEntity;
import lk.healthylife.hms.exception.NoRequiredInfoException;
import lk.healthylife.hms.exception.OperationException;
import lk.healthylife.hms.service.CommonReferenceService;
import lk.healthylife.hms.service.MedicalTestService;
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

import static lk.healthylife.hms.util.constant.CommonReferenceTypeCodes.*;
import static lk.healthylife.hms.util.constant.Constants.STATUS_ACTIVE;
import static lk.healthylife.hms.util.constant.Constants.STATUS_INACTIVE;

@Slf4j
@Service
public class MedicalTestServiceImpl extends EntityValidator implements MedicalTestService {

    private final DataSource dataSource;

    private final CommonReferenceService commonReferenceService;

    private final AuditorAwareImpl auditorAware;

    @PersistenceContext
    private EntityManager entityManager;

    public MedicalTestServiceImpl(DataSource dataSource,
                                  CommonReferenceService commonReferenceService,
                                  AuditorAwareImpl auditorAware) {
        this.dataSource = dataSource;
        this.commonReferenceService = commonReferenceService;
        this.auditorAware = auditorAware;
    }

    @Override
    public List<MedicalTestDTO> getAllMedicalTestsDropdown() {

        List<MedicalTestDTO> medicalTestList = new ArrayList<>();

        final String queryString = "SELECT MDTS_ID, MDTS_NAME FROM T_MS_MEDICAL_TEST \n" +
                "WHERE MDTS_STATUS = :status AND MDTS_BRANCH_ID IN (:branchIdList)\n" +
                "ORDER BY CREATED_DATE";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());

        List<Map<String,Object>> result = extractResultSet(query);

        if(result.size() == 0)
            return Collections.emptyList();

        for (Map<String,Object> room : result) {

            MedicalTestDTO medicalTestDTO = new MedicalTestDTO();

            medicalTestDTO.setMedicalTestId(extractLongValue(String.valueOf(room.get("MDTS_ID"))));
            medicalTestDTO.setName(extractValue(String.valueOf(room.get("MDTS_NAME"))));

            medicalTestList.add(medicalTestDTO);
        }

        return medicalTestList;
    }

    @Override
    public MedicalTestDTO insertMedicalTest(MedicalTestDTO medicalTestDTO) {

        BigInteger insertedRowId = null;

        validateEntity(medicalTestDTO);

        validateMedicalReferenceDetailsOnPersist(medicalTestDTO);

        try (Connection connection = dataSource.getConnection()) {

            CallableStatement statement = connection
                    .prepareCall("{CALL INSERT INTO T_MS_MEDICAL_TEST(MDTS_NAME, MDTS_DESCRIPTION, MDTS_FEE, MDTS_TYPE,\n" +
                    "MDTS_STATUS, MDTS_BRANCH_ID, CREATED_DATE, CREATED_USER_CODE)\n" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING MDTS_ID INTO ?}");

            statement.setString(1, medicalTestDTO.getName());
            statement.setString(2, medicalTestDTO.getDescription());
            statement.setBigDecimal(3, medicalTestDTO.getFee());
            statement.setString(4, medicalTestDTO.getType());
            statement.setShort(5, STATUS_ACTIVE.getShortValue());
            statement.setLong(6, captureBranchIds().get(0));
            statement.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(8, auditorAware.getCurrentAuditor().get());

            statement.registerOutParameter( 9, Types.BIGINT);

            int updateCount = statement.executeUpdate();

            if (updateCount > 0)
                insertedRowId = BigInteger.valueOf(statement.getLong(9));

        } catch (Exception e) {
            log.error("Error while persisting Medical Test : " + e.getMessage());
            throw new OperationException("Error while persisting Medical Test");
        }

        return getMedicalTestById(insertedRowId.longValue());
    }

    @Override
    public MedicalTestDTO getMedicalTestById(Long medicalTestId) {

        MedicalTestDTO medicalTestDTO = null;

        validateMedicalTestId(medicalTestId);

        String queryString = "SELECT mt.MDTS_ID, mt.MDTS_NAME, mt.MDTS_DESCRIPTION, mt.MDTS_FEE, mt.MDTS_STATUS, mt.MDTS_TYPE, \n" +
                "mt.MDTS_BRANCH_ID, mt.CREATED_DATE, mt.CREATED_USER_CODE, mt.LAST_MOD_DATE, mt.LAST_MOD_USER_CODE,\n" +
                "type.CMRF_DESCRIPTION AS MEDICAL_TEST_TYPE\n" +
                "FROM T_MS_MEDICAL_TEST mt\n" +
                "LEFT JOIN T_RF_COMMON_REFERENCE type ON mt.MDTS_TYPE = type.CMRF_CODE\n" +
                "WHERE mt.MDTS_ID = :medicalTestId AND mt.MDTS_STATUS = :status AND mt.MDTS_BRANCH_ID IN (:branchIdList)\n";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("medicalTestId", medicalTestId);

        List<Map<String,Object>> result = extractResultSet(query);

        if (result.size() == 0)
            return null;

        for (Map<String,Object> medicalTest : result) {

            medicalTestDTO = new MedicalTestDTO();

            createDTO(medicalTestDTO, medicalTest);
        }

        return medicalTestDTO;
    }

    @Transactional
    @Override
    public MedicalTestDTO updateMedicalTest(Long medicalTestId, MedicalTestDTO medicalTestDTO) {

        validateMedicalTestId(medicalTestId);

        validateMedicalReferenceDetailsOnPersist(medicalTestDTO);

        final Query query = entityManager
                .createNativeQuery("UPDATE T_MS_MEDICAL_TEST SET MDTS_NAME = :name, MDTS_DESCRIPTION = :description,\n" +
                        "MDTS_TYPE = :type, MDTS_FEE = :fee, LAST_MOD_DATE = :lastModDate, LAST_MOD_USER_CODE = :lastModUser\n" +
                        "WHERE MDTS_ID = :medicalTestId AND MDTS_STATUS = :status AND MDTS_BRANCH_ID IN (:branchIdList)")
                .setParameter("name", medicalTestDTO.getName())
                .setParameter("type", medicalTestDTO.getType())
                .setParameter("description", medicalTestDTO.getDescription())
                .setParameter("fee", medicalTestDTO.getFee())
                .setParameter("lastModDate", LocalDateTime.now())
                .setParameter("lastModUser", auditorAware.getCurrentAuditor().get())
                .setParameter("status", STATUS_ACTIVE.getShortValue())
                .setParameter("medicalTestId", medicalTestId)
                .setParameter("branchIdList", captureBranchIds());

        query.executeUpdate();

        return getMedicalTestById(medicalTestId);
    }

    @Transactional
    @Override
    public Boolean removeMedicalTest(Long medicalTestId) {

        validateMedicalTestId(medicalTestId);

        final Query query = entityManager.createNativeQuery("UPDATE T_MS_MEDICAL_TEST SET MDTS_STATUS = :statusInActive\n" +
                        "WHERE MDTS_ID = :medicalTestId AND MDTS_STATUS = :statusActive AND MDTS_BRANCH_ID IN (:branchIdList)")
                .setParameter("statusInActive", STATUS_INACTIVE.getShortValue())
                .setParameter("medicalTestId", medicalTestId)
                .setParameter("statusActive", STATUS_ACTIVE.getShortValue())
                .setParameter("branchIdList", captureBranchIds());

        return query.executeUpdate() == 0 ? false : true;
    }

    @Override
    public PaginatedEntity medicalTestPaginatedSearch(String name, String type, Integer page, Integer size) {

        PaginatedEntity paginatedMediccalTestList = null;
        List<MedicalTestDTO> medicalTestList = null;

        page = validatePaginateIndexes(page, size);

        type = type.isEmpty() ? null : type;

        final String countQueryString = "SELECT COUNT(MDTS_ID)" +
                "FROM T_MS_MEDICAL_TEST\n" +
                "WHERE MDTS_STATUS = :status AND MDTS_BRANCH_ID IN (:branchIdList)\n" +
                "AND (:type IS NULL OR (:type IS NOT NULL) AND MDTS_TYPE = :type)\n" +
                "AND (upper(MDTS_NAME) LIKE ('%'||upper(:name)||'%'))";

        Query query = entityManager.createNativeQuery(countQueryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("type", type);
        query.setParameter("name", name);
        query.setParameter("branchIdList", captureBranchIds());

        final int selectedRecordCount = ((Number) query.getSingleResult()).intValue();

        query = null;

        final String queryString = "SELECT mt.MDTS_ID, mt.MDTS_NAME, mt.MDTS_DESCRIPTION, mt.MDTS_FEE, mt.MDTS_STATUS, mt.MDTS_TYPE, \n" +
                "mt.MDTS_BRANCH_ID, mt.CREATED_DATE, mt.CREATED_USER_CODE, mt.LAST_MOD_DATE, mt.LAST_MOD_USER_CODE,\n" +
                "type.CMRF_DESCRIPTION AS MEDICAL_TEST_TYPE\n" +
                "FROM T_MS_MEDICAL_TEST mt\n" +
                "LEFT JOIN T_RF_COMMON_REFERENCE type ON mt.MDTS_TYPE = type.CMRF_CODE\n" +
                "WHERE mt.MDTS_STATUS = :status AND mt.MDTS_BRANCH_ID IN (:branchIdList)\n" +
                "AND (:type IS NULL OR (:type IS NOT NULL) AND mt.MDTS_TYPE = :type)\n" +
                "AND (upper(mt.MDTS_NAME) LIKE ('%'||upper(:name)||'%'))\n" +
                "ORDER BY mt.CREATED_DATE OFFSET :page ROWS FETCH NEXT :size ROWS ONLY";

        query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("type", type);
        query.setParameter("name", name);
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("size", size);
        query.setParameter("page", size * page);

        List<Map<String,Object>> result = extractResultSet(query);

        paginatedMediccalTestList = new PaginatedEntity();
        medicalTestList = new ArrayList<>();

        for (Map<String,Object> medicalTest : result) {

            MedicalTestDTO medicalTestDTO = new MedicalTestDTO();

            createDTO(medicalTestDTO, medicalTest);

            medicalTestList.add(medicalTestDTO);
        }

        paginatedMediccalTestList
                .setTotalNoOfPages(getTotalNoOfPages(selectedRecordCount, size));
        paginatedMediccalTestList.setTotalNoOfRecords(Long.valueOf(selectedRecordCount));
        paginatedMediccalTestList.setEntities(medicalTestList);

        return paginatedMediccalTestList;
    }

    private void validateMedicalReferenceDetailsOnPersist(MedicalTestDTO medicalTestDTO) {
        if(medicalTestDTO.getType() != null)
            commonReferenceService
                    .getByCmrfCodeAndCmrtCode(MEDICAL_TEST_TYPES.getValue(), medicalTestDTO.getType());
    }

    private void validateMedicalTestId(Long medicalTestId) {
        if(medicalTestId == null)
            throw new NoRequiredInfoException("Medical Test Id is Required");
    }

    private void createDTO(MedicalTestDTO medicalTestDTO, Map<String,Object> medicalTest) {

        medicalTestDTO.setMedicalTestId(extractLongValue(String.valueOf(medicalTest.get("MDTS_ID"))));
        medicalTestDTO.setName(extractValue(String.valueOf(medicalTest.get("MDTS_NAME"))));
        medicalTestDTO.setDescription(extractValue(String.valueOf(medicalTest.get("MDTS_DESCRIPTION"))));
        medicalTestDTO.setFee(extractDecimalValue(String.valueOf(medicalTest.get("MDTS_FEE"))));
        medicalTestDTO.setType(extractValue(String.valueOf(medicalTest.get("MDTS_TYPE"))));
        medicalTestDTO.setTypeName(extractValue(String.valueOf(medicalTest.get("MEDICAL_TEST_TYPE"))));
        medicalTestDTO.setStatus(extractShortValue(String.valueOf(medicalTest.get("MDTS_STATUS"))));
        medicalTestDTO.setCreatedDate(extractDateTime(String.valueOf(medicalTest.get("CREATED_DATE"))));
        medicalTestDTO.setCreatedUserCode(extractValue(String.valueOf(medicalTest.get("CREATED_USER_CODE"))));
        medicalTestDTO.setLastUpdatedDate(extractDateTime(String.valueOf(medicalTest.get("LAST_MOD_DATE"))));
        medicalTestDTO.setLastUpdatedUserCode(extractValue(String.valueOf(medicalTest.get("LAST_MOD_USER_CODE"))));
        medicalTestDTO.setBranchId(extractLongValue(String.valueOf(medicalTest.get("MDTS_BRANCH_ID"))));
    }
}

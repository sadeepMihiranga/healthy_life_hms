package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.config.AuditorAwareImpl;
import lk.healthylife.hms.config.EntityValidator;
import lk.healthylife.hms.dto.PaginatedEntity;
import lk.healthylife.hms.dto.PatientMedicalTestDTO;
import lk.healthylife.hms.exception.NoRequiredInfoException;
import lk.healthylife.hms.exception.OperationException;
import lk.healthylife.hms.service.MedicalTestService;
import lk.healthylife.hms.service.PartyService;
import lk.healthylife.hms.service.PatientMedicalTestService;
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
import java.util.List;
import java.util.Map;

import static lk.healthylife.hms.util.constant.Constants.STATUS_ACTIVE;
import static lk.healthylife.hms.util.constant.Constants.STATUS_INACTIVE;

@Slf4j
@Service
public class PatientMedicalTestServiceImpl extends EntityValidator implements PatientMedicalTestService {

    private final DataSource dataSource;

    private final AuditorAwareImpl auditorAware;

    private final PartyService partyService;
    private final MedicalTestService medicalTestService;

    @PersistenceContext
    private EntityManager entityManager;

    public PatientMedicalTestServiceImpl(DataSource dataSource,
                                         AuditorAwareImpl auditorAware,
                                         PartyService partyService,
                                         MedicalTestService medicalTestService) {
        this.dataSource = dataSource;
        this.auditorAware = auditorAware;
        this.partyService = partyService;
        this.medicalTestService = medicalTestService;
    }

    @Override
    public PatientMedicalTestDTO insertPatientMedicalTest(PatientMedicalTestDTO patientMedicalTestDTO) {

        BigInteger insertedRowId = null;

        validateEntity(patientMedicalTestDTO);

        validatePatientMedicalReferenceDetailsOnPersist(patientMedicalTestDTO);

        try (Connection connection = dataSource.getConnection()) {

            CallableStatement statement = connection
                    .prepareCall("{CALL INSERT INTO T_TR_PATIENT_MEDICAL_TEST(PMDT_MEDICAL_TEST_ID, PMDT_PATIENT_CODE,\n" +
                            "PMDT_TESTED_BY, PMDT_STATUS, CREATED_DATE, CREATED_USER_CODE, PMDT_ADMISSION_ID, \n" +
                            "PMDT_BRANCH_ID, PMDT_TEST_STATUS, PMDT_REMARKS) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING PMDT_ID INTO ?}");

            statement.setLong(1, patientMedicalTestDTO.getMedicalTestId());
            statement.setString(2, patientMedicalTestDTO.getPatientCode());
            statement.setString(3, auditorAware.getCurrentAuditor().get());
            statement.setShort(4, STATUS_ACTIVE.getShortValue());
            statement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(6, auditorAware.getCurrentAuditor().get());
            statement.setLong(7, patientMedicalTestDTO.getAdmissionId());
            statement.setLong(8, captureBranchIds().get(0));
            statement.setShort(9, patientMedicalTestDTO.getTestStatus());
            statement.setString(10, patientMedicalTestDTO.getRemarks());

            statement.registerOutParameter( 11, Types.BIGINT);

            int updateCount = statement.executeUpdate();

            if (updateCount > 0)
                insertedRowId = BigInteger.valueOf(statement.getLong(11));

        } catch (Exception e) {
            log.error("Error while persisting Patient Medical Test : " + e.getMessage());
            throw new OperationException("Error while persisting Patient Medical Test");
        }

        return getPatientMedicalTestById(insertedRowId.longValue());
    }

    @Override
    public PatientMedicalTestDTO getPatientMedicalTestById(Long patientMedicalTestId) {

        PatientMedicalTestDTO patientMedicalTestDTO = null;

        validatePatientMedicalTestId(patientMedicalTestId);

        String queryString = "SELECT pmt.PMDT_ID, pmt.PMDT_MEDICAL_TEST_ID, pmt.PMDT_PATIENT_CODE,\n" +
                "pmt.PMDT_TESTED_BY, pmt.PMDT_STATUS, pmt.CREATED_DATE, pmt.CREATED_USER_CODE, pmt.PMDT_ADMISSION_ID, \n" +
                "pmt.PMDT_BRANCH_ID, pmt.PMDT_TEST_STATUS, pmt.PMDT_REMARKS, pmt.LAST_MOD_DATE, pmt.LAST_MOD_USER_CODE, pmt.PMDT_APPROVED_BY\n" +
                "FROM T_TR_PATIENT_MEDICAL_TEST pmt\n" +
                "WHERE pmt.PMDT_ID = :patientMedicalTestId AND pmt.PMDT_STATUS = :status AND pmt.PMDT_BRANCH_ID IN (:branchIdList)\n";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("patientMedicalTestId", patientMedicalTestId);

        List<Map<String,Object>> result = extractResultSet(query);

        if (result.size() == 0)
            return null;

        for (Map<String,Object> patientMedicalTest : result) {

            patientMedicalTestDTO = new PatientMedicalTestDTO();

            createDTO(patientMedicalTestDTO, patientMedicalTest);

            patientMedicalTestDTO.setPatient(partyService.getPartyByPartyCode(patientMedicalTestDTO.getPatientCode()));
            patientMedicalTestDTO.setMedicalTest(medicalTestService.getMedicalTestById(patientMedicalTestDTO.getMedicalTestId()));
        }

        return patientMedicalTestDTO;
    }

    @Override
    public PaginatedEntity patientMedicalTestPaginatedSearch(String patientName, String testName, Integer page, Integer size) {

        PaginatedEntity paginatedPatientMedicalTestList = null;
        List<PatientMedicalTestDTO> patientMedicalTestList = null;

        page = validatePaginateIndexes(page, size);

        final String countQueryString = "SELECT COUNT(pmt.PMDT_ID)\n" +
                "FROM T_TR_PATIENT_MEDICAL_TEST pmt\n" +
                "INNER JOIN T_MS_MEDICAL_TEST mt ON pmt.PMDT_MEDICAL_TEST_ID = mt.MDTS_ID\n" +
                "INNER JOIN T_MS_PARTY patinet ON pmt.PMDT_PATIENT_CODE = patinet.PRTY_CODE\n" +
                "WHERE pmt.PMDT_STATUS = :status AND pmt.PMDT_BRANCH_ID IN (:branchIdList)\n" +
                "AND (upper(mt.MDTS_NAME) LIKE ('%'||upper(:testName)||'%'))\n" +
                "AND (upper(patinet.PRTY_NAME) LIKE ('%'||upper(:patientName)||'%'))";

        Query query = entityManager.createNativeQuery(countQueryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("patientName", patientName);
        query.setParameter("testName", testName);
        query.setParameter("branchIdList", captureBranchIds());

        final int selectedRecordCount = ((Number) query.getSingleResult()).intValue();

        query = null;

        final String queryString = "SELECT pmt.PMDT_ID, pmt.PMDT_MEDICAL_TEST_ID, pmt.PMDT_PATIENT_CODE, pmt.PMDT_TESTED_BY,\n" +
                "pmt.PMDT_STATUS, pmt.CREATED_DATE, pmt.CREATED_USER_CODE, pmt.PMDT_ADMISSION_ID,\n" +
                "pmt.PMDT_BRANCH_ID, pmt.PMDT_TEST_STATUS, pmt.PMDT_REMARKS, pmt.LAST_MOD_DATE,\n" +
                "pmt.LAST_MOD_USER_CODE, pmt.PMDT_APPROVED_BY, mt.MDTS_NAME AS TEST_NAME, patinet.PRTY_NAME AS PATIENT_NAME\n" +
                "FROM T_TR_PATIENT_MEDICAL_TEST pmt\n" +
                "INNER JOIN T_MS_MEDICAL_TEST mt ON pmt.PMDT_MEDICAL_TEST_ID = mt.MDTS_ID\n" +
                "INNER JOIN T_MS_PARTY patinet ON pmt.PMDT_PATIENT_CODE = patinet.PRTY_CODE\n" +
                "WHERE pmt.PMDT_STATUS = :status AND pmt.PMDT_BRANCH_ID IN (:branchIdList)\n" +
                "AND (upper(mt.MDTS_NAME) LIKE ('%'||upper(:testName)||'%'))\n" +
                "AND (upper(patinet.PRTY_NAME) LIKE ('%'||upper(:patientName)||'%'))\n" +
                "ORDER BY pmt.CREATED_DATE OFFSET :page ROWS FETCH NEXT :size ROWS ONLY";

        query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("patientName", patientName);
        query.setParameter("testName", testName);
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("size", size);
        query.setParameter("page", size * page);

        List<Map<String,Object>> result = extractResultSet(query);

        paginatedPatientMedicalTestList = new PaginatedEntity();
        patientMedicalTestList = new ArrayList<>();

        for (Map<String,Object> patientMedicalTest : result) {

            PatientMedicalTestDTO patientMedicalTestDTO = new PatientMedicalTestDTO();

            createDTO(patientMedicalTestDTO, patientMedicalTest);

            patientMedicalTestList.add(patientMedicalTestDTO);
        }

        paginatedPatientMedicalTestList
                .setTotalNoOfPages(getTotalNoOfPages(selectedRecordCount, size));
        paginatedPatientMedicalTestList.setTotalNoOfRecords(Long.valueOf(selectedRecordCount));
        paginatedPatientMedicalTestList.setEntities(patientMedicalTestList);

        return paginatedPatientMedicalTestList;
    }

    @Transactional
    @Override
    public PatientMedicalTestDTO updatePatientMedicalTest(Long patientMedicalTestId, PatientMedicalTestDTO patientMedicalTestDTO) {

        validatePatientMedicalTestId(patientMedicalTestId);

        validatePatientMedicalReferenceDetailsOnPersist(patientMedicalTestDTO);

        final Query query = entityManager
                .createNativeQuery("UPDATE T_TR_PATIENT_MEDICAL_TEST SET PMDT_MEDICAL_TEST_ID = :medicalTestId,\n" +
                        "PMDT_PATIENT_CODE = :patientCode, PMDT_TEST_STATUS = :testStatus,\n" +
                        "PMDT_REMARKS = :remarks, LAST_MOD_DATE = :lastModDate, LAST_MOD_USER_CODE =:lastModUser\n" +
                        "WHERE PMDT_ID = :patientMedicalTestId AND PMDT_STATUS = :status AND PMDT_BRANCH_ID IN (:branchIdList) ")
                .setParameter("medicalTestId", patientMedicalTestDTO.getMedicalTestId())
                .setParameter("patientCode", patientMedicalTestDTO.getPatientCode())
                .setParameter("testStatus", patientMedicalTestDTO.getTestStatus())
                .setParameter("remarks", patientMedicalTestDTO.getRemarks())
                .setParameter("lastModDate", LocalDateTime.now())
                .setParameter("lastModUser", auditorAware.getCurrentAuditor().get())
                .setParameter("status", STATUS_ACTIVE.getShortValue())
                .setParameter("patientMedicalTestId", patientMedicalTestId)
                .setParameter("branchIdList", captureBranchIds());

        query.executeUpdate();

        return getPatientMedicalTestById(patientMedicalTestId);
    }

    @Transactional
    @Override
    public Boolean removeMedicalTest(Long patientMedicalTestId) {
        validatePatientMedicalTestId(patientMedicalTestId);

        final Query query = entityManager.createNativeQuery("UPDATE T_TR_PATIENT_MEDICAL_TEST SET PMDT_STATUS = :statusInActive\n" +
                        "WHERE PMDT_ID = :patientMedicalTestId AND PMDT_STATUS = :statusActive AND PMDT_BRANCH_ID IN (:branchIdList)")
                .setParameter("statusInActive", STATUS_INACTIVE.getShortValue())
                .setParameter("patientMedicalTestId", patientMedicalTestId)
                .setParameter("statusActive", STATUS_ACTIVE.getShortValue())
                .setParameter("branchIdList", captureBranchIds());

        return query.executeUpdate() == 0 ? false : true;
    }

    @Transactional
    @Override
    public PatientMedicalTestDTO approveMedicalTest(Long patientMedicalTestId) {
        validatePatientMedicalTestId(patientMedicalTestId);

        final Query query = entityManager
                .createNativeQuery("UPDATE T_TR_PATIENT_MEDICAL_TEST SET PMDT_APPROVED_BY = :approvedBy,\n" +
                        "LAST_MOD_DATE = :lastModDate, LAST_MOD_USER_CODE =:lastModUser\n" +
                        "WHERE PMDT_ID = :patientMedicalTestId AND PMDT_STATUS = :status AND PMDT_BRANCH_ID IN (:branchIdList) ")
                .setParameter("approvedBy",  auditorAware.getCurrentAuditor().get())
                .setParameter("lastModDate", LocalDateTime.now())
                .setParameter("lastModUser", auditorAware.getCurrentAuditor().get())
                .setParameter("status", STATUS_ACTIVE.getShortValue())
                .setParameter("patientMedicalTestId", patientMedicalTestId)
                .setParameter("branchIdList", captureBranchIds());

        query.executeUpdate();

        return getPatientMedicalTestById(patientMedicalTestId);
    }

    private void validatePatientMedicalReferenceDetailsOnPersist(PatientMedicalTestDTO patientMedicalTestDTO) {
        if(Strings.isNullOrEmpty(patientMedicalTestDTO.getPatientCode())) {
            partyService
                    .getPartyByPartyCode(patientMedicalTestDTO.getPatientCode());
        }
    }

    private void validatePatientMedicalTestId(Long patientMedicalTestId) {
        if(patientMedicalTestId == null)
            throw new NoRequiredInfoException("Patient Medical Test Id is Required");
    }

    private void createDTO(PatientMedicalTestDTO patientMedicalTestDTO, Map<String,Object> patientMedicalTest) {

        patientMedicalTestDTO.setPatientMedicalTestId(extractLongValue(String.valueOf(patientMedicalTest.get("PMDT_ID"))));
        patientMedicalTestDTO.setMedicalTestId(extractLongValue(String.valueOf(patientMedicalTest.get("PMDT_MEDICAL_TEST_ID"))));
        patientMedicalTestDTO.setMedicalTestName(extractValue(String.valueOf(patientMedicalTest.get("TEST_NAME"))));
        patientMedicalTestDTO.setPatientCode(extractValue(String.valueOf(patientMedicalTest.get("PMDT_PATIENT_CODE"))));
        patientMedicalTestDTO.setPatientName(extractValue(String.valueOf(patientMedicalTest.get("PATIENT_NAME"))));
        patientMedicalTestDTO.setTestedBy(extractValue(String.valueOf(patientMedicalTest.get("PMDT_TESTED_BY"))));
        patientMedicalTestDTO.setApprovedBy(extractValue(String.valueOf(patientMedicalTest.get("PMDT_APPROVED_BY"))));
        patientMedicalTestDTO.setAdmissionId(extractLongValue(String.valueOf(patientMedicalTest.get("PMDT_ADMISSION_ID"))));
        patientMedicalTestDTO.setRemarks(extractValue(String.valueOf(patientMedicalTest.get("PMDT_REMARKS"))));
        patientMedicalTestDTO.setTestStatus(extractShortValue(String.valueOf(patientMedicalTest.get("PMDT_TEST_STATUS"))));
        patientMedicalTestDTO.setStatus(extractShortValue(String.valueOf(patientMedicalTest.get("PMDT_STATUS"))));
        patientMedicalTestDTO.setCreatedDate(extractDateTime(String.valueOf(patientMedicalTest.get("CREATED_DATE"))));
        patientMedicalTestDTO.setCreatedUserCode(extractValue(String.valueOf(patientMedicalTest.get("CREATED_USER_CODE"))));
        patientMedicalTestDTO.setLastUpdatedDate(extractDateTime(String.valueOf(patientMedicalTest.get("LAST_MOD_DATE"))));
        patientMedicalTestDTO.setLastUpdatedUserCode(extractValue(String.valueOf(patientMedicalTest.get("LAST_MOD_USER_CODE"))));
        patientMedicalTestDTO.setBranchId(extractLongValue(String.valueOf(patientMedicalTest.get("PMDT_BRANCH_ID"))));
    }
}

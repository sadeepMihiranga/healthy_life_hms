package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.config.AuditorAwareImpl;
import lk.healthylife.hms.config.EntityValidator;
import lk.healthylife.hms.dto.PatientConditionDTO;
import lk.healthylife.hms.exception.NoRequiredInfoException;
import lk.healthylife.hms.exception.OperationException;
import lk.healthylife.hms.service.PatientConditionService;
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
import java.util.List;
import java.util.Map;

import static lk.healthylife.hms.util.constant.Constants.STATUS_ACTIVE;
import static lk.healthylife.hms.util.constant.Constants.STATUS_INACTIVE;

@Slf4j
@Service
public class PatientConditionServiceImpl extends EntityValidator implements PatientConditionService {

    private final AuditorAwareImpl auditorAware;
    private final DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    public PatientConditionServiceImpl(AuditorAwareImpl auditorAware,
                                       DataSource dataSource) {
        this.auditorAware = auditorAware;
        this.dataSource = dataSource;
    }

    @Transactional
    @Override
    public PatientConditionDTO addPatientCondition(PatientConditionDTO patientConditionDTO) {

        BigInteger insertedRowId = null;

        validateEntity(patientConditionDTO);

        try (Connection connection = dataSource.getConnection()) {

            CallableStatement statement = connection
                    .prepareCall("{CALL INSERT INTO T_TR_PATIENT_CONDITION (PTCD_ADMISSION_ID, PTCD_CONDITION_WHEN,\n" +
                            "PTCD_SYMPTOM_ID, PTCD_DESCRIPTION, PTCD_STATUS, CREATED_DATE, CREATED_USER_CODE, PTCD_BRANCH_ID)\n" +
                            "VALUES(?, ?, ?, ?, ?, ?, ?, ?) RETURNING PTCD_ID INTO ?}");

            statement.setLong(1, patientConditionDTO.getAdmissionId());
            statement.setString(2, patientConditionDTO.getConditionWhen());
            statement.setLong(3, patientConditionDTO.getSymptomId());
            statement.setString(4, patientConditionDTO.getDescription());
            statement.setShort(5, STATUS_ACTIVE.getShortValue());
            statement.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(7, auditorAware.getCurrentAuditor().get());
            statement.setLong(8, captureBranchIds().get(0));

            statement.registerOutParameter( 9, Types.BIGINT);

            int updateCount = statement.executeUpdate();

            if (updateCount > 0)
                insertedRowId = BigInteger.valueOf(statement.getLong(9));

        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error while persisting Patient Condition : " + e.getMessage());
            throw new OperationException("Error while persisting Patient Condition");
        }

        return getPatientConditionById(insertedRowId.longValue());
    }

    @Override
    public PatientConditionDTO getPatientConditionById(Long patientConditionId) {

        PatientConditionDTO patientConditionDTO = null;

        validatePatientConditionId(patientConditionId);

        final String queryString = "SELECT pc.PTCD_ID, pc.PTCD_ADMISSION_ID, pc.PTCD_PATIENT_SURGERY_ID, pc.PTCD_CONDITION_WHEN,\n" +
                "pc.PTCD_SYMPTOM_ID, pc.PTCD_DESCRIPTION, pc.PTCD_STATUS, pc.CREATED_DATE, pc.CREATED_USER_CODE,\n" +
                "pc.LAST_MOD_DATE, pc.LAST_MOD_USER_CODE, br.BRNH_NAME AS BRANCH_NAME\n" +
                "FROM T_TR_PATIENT_CONDITION pc\n" +
                "INNER JOIN T_RF_BRANCH br ON pc.PTCD_BRANCH_ID = br.BRNH_ID\n" +
                "WHERE pc.PTCD_ID = :patientConditionId AND pc.PTCD_STATUS = :status AND pc.PTCD_BRANCH_ID IN (:branchIdList)";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("patientConditionId", patientConditionId);

        List<Map<String,Object>> result = extractResultSet(query);

        if (result.size() == 0)
            return null;

        for (Map<String,Object> patientCondition : result) {
            patientConditionDTO = new PatientConditionDTO();
            createDTO(patientConditionDTO, patientCondition);
        }

        return patientConditionDTO;
    }

    @Override
    public List<PatientConditionDTO> getPatientConditionListByAdmission(Long admissionId) {

        List<PatientConditionDTO> patientConditionDTOList = new ArrayList<>();

        validatePatientConditionId(admissionId);

        final String queryString = "SELECT pc.PTCD_ID, pc.PTCD_ADMISSION_ID, pc.PTCD_PATIENT_SURGERY_ID, pc.PTCD_CONDITION_WHEN,\n" +
                "pc.PTCD_SYMPTOM_ID, pc.PTCD_DESCRIPTION, pc.PTCD_STATUS, pc.CREATED_DATE, pc.CREATED_USER_CODE,\n" +
                "pc.LAST_MOD_DATE, pc.LAST_MOD_USER_CODE, br.BRNH_NAME AS BRANCH_NAME\n" +
                "FROM T_TR_PATIENT_CONDITION pc\n" +
                "INNER JOIN T_RF_BRANCH br ON pc.PTCD_BRANCH_ID = br.BRNH_ID\n" +
                "WHERE pc.PTCD_ADMISSION_ID = :admissionId AND pc.PTCD_STATUS = :status AND pc.PTCD_BRANCH_ID IN (:branchIdList)";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("admissionId", admissionId);

        List<Map<String,Object>> result = extractResultSet(query);

        if (result.size() == 0)
            return null;

        for (Map<String,Object> patientCondition : result) {
            PatientConditionDTO patientConditionDTO = new PatientConditionDTO();
            createDTO(patientConditionDTO, patientCondition);
            patientConditionDTOList.add(patientConditionDTO);
        }

        return patientConditionDTOList;
    }

    @Override
    public Boolean removePatientCondition(Long patientConditionId) {

        validatePatientConditionId(patientConditionId);

        final Query query = entityManager.createNativeQuery("UPDATE T_TR_PATIENT_CONDITION SET PTCD_STATUS = :statusInActive\n" +
                        "WHERE PTCD_ID = :patientConditionId AND PTCD_STATUS = :statusActive AND PTCD_BRANCH_ID IN (:branchIdList)")
                .setParameter("statusInActive", STATUS_INACTIVE.getShortValue())
                .setParameter("patientConditionId", patientConditionId)
                .setParameter("statusActive", STATUS_ACTIVE.getShortValue())
                .setParameter("branchIdList", captureBranchIds());

        return query.executeUpdate() == 0 ? false : true;
    }

    @Override
    public Boolean removePatientConditionsByAdmission(Long admissionId) {

        final Query query = entityManager.createNativeQuery("UPDATE T_TR_PATIENT_CONDITION SET PTCD_STATUS = :statusInActive\n" +
                        "WHERE PTCD_ADMISSION_ID = :admissionId AND PTCD_STATUS = :statusActive AND PTCD_BRANCH_ID IN (:branchIdList)")
                .setParameter("statusInActive", STATUS_INACTIVE.getShortValue())
                .setParameter("statusActive", STATUS_ACTIVE.getShortValue())
                .setParameter("admissionId", admissionId)
                .setParameter("branchIdList", captureBranchIds());

        return query.executeUpdate() == 0 ? false : true;
    }

    private void validatePatientConditionId(Long patientConditionId) {
        if(patientConditionId == null)
            throw new NoRequiredInfoException("Patient Condition Id is Required");
    }

    private void createDTO(PatientConditionDTO patientConditionDTO, Map<String,Object> admission) {

        patientConditionDTO.setPatientConditionId(extractLongValue(String.valueOf(admission.get("PTCD_ID"))));
        patientConditionDTO.setAdmissionId(extractLongValue(String.valueOf(admission.get("PTCD_ADMISSION_ID"))));
        patientConditionDTO.setPatientSurgeryId(extractLongValue(String.valueOf(admission.get("PTCD_PATIENT_SURGERY_ID"))));
        patientConditionDTO.setSymptomId(extractLongValue(String.valueOf(admission.get("PTCD_SYMPTOM_ID"))));
        patientConditionDTO.setConditionWhen(extractValue(String.valueOf(admission.get("PTCD_CONDITION_WHEN"))));
        patientConditionDTO.setDescription(extractValue(String.valueOf(admission.get("PTCD_DESCRIPTION"))));
        patientConditionDTO.setStatus(extractShortValue(String.valueOf(admission.get("PTCD_STATUS"))));
        patientConditionDTO.setCreatedDate(extractDateTime(String.valueOf(admission.get("CREATED_DATE"))));
        patientConditionDTO.setCreatedUserCode(extractValue(String.valueOf(admission.get("CREATED_USER_CODE"))));
        patientConditionDTO.setLastUpdatedDate(extractDateTime(String.valueOf(admission.get("LAST_MOD_DATE"))));
        patientConditionDTO.setLastUpdatedUserCode(extractValue(String.valueOf(admission.get("LAST_MOD_USER_CODE"))));
        patientConditionDTO.setBranchId(extractLongValue(String.valueOf(admission.get("PTAD_BRANCH_ID"))));
        patientConditionDTO.setBranchName(extractValue(String.valueOf(admission.get("BRANCH_NAME"))));
    }
}

package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.config.AuditorAwareImpl;
import lk.healthylife.hms.config.EntityValidator;
import lk.healthylife.hms.dto.TreatmentAdviceDTO;
import lk.healthylife.hms.exception.NoRequiredInfoException;
import lk.healthylife.hms.exception.OperationException;
import lk.healthylife.hms.service.TreatmentAdviceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;
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
public class TreatmentAdviceServiceImpl extends EntityValidator implements TreatmentAdviceService {

    private final AuditorAwareImpl auditorAware;
    private final DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    public TreatmentAdviceServiceImpl(AuditorAwareImpl auditorAware, DataSource dataSource) {
        this.auditorAware = auditorAware;
        this.dataSource = dataSource;
    }

    @Override
    public TreatmentAdviceDTO addTreatmentAdvice(Long admissionId, TreatmentAdviceDTO treatmentAdviceDTO) {

        BigInteger insertedRowId = null;

        validateEntity(treatmentAdviceDTO);

        try (Connection connection = dataSource.getConnection()) {

            CallableStatement statement = connection
                    .prepareCall("{CALL INSERT INTO T_TR_PATIENT_TREATMENT_ADVICE(PTRA_ADMISSION_ID, PTRA_PATIENT_CODE, \n" +
                            "PTRA_DOCTOR_CODE, PTRA_ADVICE, PTRA_STATUS, CREATED_DATE, CREATED_USER_CODE, PTRA_BRANCH_ID)\n" +
                            "VALUES(?, ?, ?, ?, ?, ?, ?, ?) RETURNING PTRA_ID INTO ?}");

            statement.setLong(1, admissionId);
            statement.setString(2, treatmentAdviceDTO.getPatientCode());
            statement.setString(3, treatmentAdviceDTO.getDoctorCode());
            statement.setString(4, treatmentAdviceDTO.getAdvice());
            statement.setShort(5, STATUS_ACTIVE.getShortValue());
            statement.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(7, auditorAware.getCurrentAuditor().get());
            statement.setLong(8, captureBranchIds().get(0));

            statement.registerOutParameter( 9, Types.BIGINT);

            int updateCount = statement.executeUpdate();

            if (updateCount > 0)
                insertedRowId = BigInteger.valueOf(statement.getLong(9));

        } catch (Exception e) {
            log.error("Error while persisting Treatment Advice : " + e.getMessage());
            throw new OperationException("Error while persisting Treatment Advice");
        }

        return getTreatmentAdviceById(insertedRowId.longValue());
    }

    @Override
    public TreatmentAdviceDTO getTreatmentAdviceById(Long adviceId) {
        TreatmentAdviceDTO treatmentAdviceDTO = null;

        validateTreatmentAdviceId(adviceId);

        final String queryString = "SELECT pta.PTRA_ID, pta.PTRA_ADMISSION_ID, pta.PTRA_PATIENT_CODE, pta.PTRA_DOCTOR_CODE, pta.PTRA_ADVICE, \n" +
                "pta.PTRA_STATUS, pta.CREATED_DATE, pta.CREATED_USER_CODE, pta.LAST_MOD_DATE, pta.LAST_MOD_USER_CODE, pta.PTRA_BRANCH_ID,\n" +
                "br.BRNH_NAME AS BRANCH_NAME\n" +
                "FROM T_TR_PATIENT_TREATMENT_ADVICE pta\n" +
                "INNER JOIN T_RF_BRANCH br ON pta.PTRA_BRANCH_ID = br.BRNH_ID\n" +
                "WHERE pta.PTRA_ID = :adviceId AND pta.PTRA_STATUS = :status AND pta.PTRA_BRANCH_ID IN (:branchIdList)";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("adviceId", adviceId);

        List<Map<String,Object>> result = extractResultSet(query);

        if (result.size() == 0)
            return null;

        for (Map<String,Object> advice : result) {
            treatmentAdviceDTO = new TreatmentAdviceDTO();
            createDTO(treatmentAdviceDTO, advice);
        }

        return treatmentAdviceDTO;
    }

    @Override
    public Boolean removeTreatmentAdvicesByAdmission(Long admissionId) {
        final Query query = entityManager.createNativeQuery("UPDATE T_TR_PATIENT_TREATMENT_ADVICE SET PTRA_STATUS = :statusInActive\n" +
                        "WHERE PTRA_STATUS = :statusActive AND PTRA_ADMISSION_ID = :admissionId AND PTRA_BRANCH_ID IN (:branchIdList)")
                .setParameter("statusInActive", STATUS_INACTIVE.getShortValue())
                .setParameter("statusActive", STATUS_ACTIVE.getShortValue())
                .setParameter("admissionId", admissionId)
                .setParameter("branchIdList", captureBranchIds());

        return query.executeUpdate() == 0 ? false : true;
    }

    @Override
    public Boolean removeTreatmentAdvice(Long adviceId) {
        final Query query = entityManager.createNativeQuery("UPDATE T_TR_PATIENT_TREATMENT_ADVICE SET PTRA_STATUS = :statusInActive\n" +
                        "WHERE PTRA_STATUS = :statusActive AND PTRA_ID = :adviceId AND PTRA_BRANCH_ID IN (:branchIdList)")
                .setParameter("statusInActive", STATUS_INACTIVE.getShortValue())
                .setParameter("statusActive", STATUS_ACTIVE.getShortValue())
                .setParameter("adviceId", adviceId)
                .setParameter("branchIdList", captureBranchIds());

        return query.executeUpdate() == 0 ? false : true;
    }

    @Override
    public List<TreatmentAdviceDTO> getAdvicesByAdmissionId(Long adviceId) {
        List<TreatmentAdviceDTO> treatmentAdviceDTOList = new ArrayList<>();

        validateTreatmentAdviceId(adviceId);

        final String queryString = "SELECT pta.PTRA_ID, pta.PTRA_ADMISSION_ID, pta.PTRA_PATIENT_CODE, pta.PTRA_DOCTOR_CODE, pta.PTRA_ADVICE, \n" +
                "pta.PTRA_STATUS, pta.CREATED_DATE, pta.CREATED_USER_CODE, pta.LAST_MOD_DATE, pta.LAST_MOD_USER_CODE, pta.PTRA_BRANCH_ID,\n" +
                "br.BRNH_NAME AS BRANCH_NAME\n" +
                "FROM T_TR_PATIENT_TREATMENT_ADVICE pta\n" +
                "INNER JOIN T_RF_BRANCH br ON pta.PTRA_BRANCH_ID = br.BRNH_ID\n" +
                "WHERE pta.PTRA_ID = :adviceId AND pta.PTRA_STATUS = :status AND pta.PTRA_BRANCH_ID IN (:branchIdList)";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("adviceId", adviceId);

        List<Map<String,Object>> result = extractResultSet(query);

        if (result.size() == 0)
            return null;

        for (Map<String,Object> advice : result) {
            TreatmentAdviceDTO treatmentAdviceDTO = new TreatmentAdviceDTO();
            createDTO(treatmentAdviceDTO, advice);
            treatmentAdviceDTOList.add(treatmentAdviceDTO);
        }

        return treatmentAdviceDTOList;
    }

    private void validateTreatmentAdviceId(Long adviceId) {
        if(adviceId == null)
            throw new NoRequiredInfoException("Treatment Advice Id is Required");
    }

    private void createDTO(TreatmentAdviceDTO treatmentAdviceDTO, Map<String,Object> advice) {

        treatmentAdviceDTO.setAdviceId(extractLongValue(String.valueOf(advice.get("PTRA_ID"))));
        treatmentAdviceDTO.setPatientAdmissionId(extractLongValue(String.valueOf(advice.get("PTRA_ADMISSION_ID"))));
        treatmentAdviceDTO.setPatientCode(extractValue(String.valueOf(advice.get("PTRA_PATIENT_CODE"))));
        treatmentAdviceDTO.setDoctorCode(extractValue(String.valueOf(advice.get("PTRA_DOCTOR_CODE"))));
        treatmentAdviceDTO.setAdvice(extractValue(String.valueOf(advice.get("PTRA_ADVICE"))));
        treatmentAdviceDTO.setStatus(extractShortValue(String.valueOf(advice.get("PTRA_STATUS"))));
        treatmentAdviceDTO.setCreatedDate(extractDateTime(String.valueOf(advice.get("CREATED_DATE"))));
        treatmentAdviceDTO.setCreatedUserCode(extractValue(String.valueOf(advice.get("CREATED_USER_CODE"))));
        treatmentAdviceDTO.setLastUpdatedDate(extractDateTime(String.valueOf(advice.get("LAST_MOD_DATE"))));
        treatmentAdviceDTO.setLastUpdatedUserCode(extractValue(String.valueOf(advice.get("LAST_MOD_USER_CODE"))));
        treatmentAdviceDTO.setBranchId(extractLongValue(String.valueOf(advice.get("PTRA_BRANCH_ID"))));
        treatmentAdviceDTO.setBranchName(extractValue(String.valueOf(advice.get("BRANCH_NAME"))));
    }
}

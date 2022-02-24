package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.config.AuditorAwareImpl;
import lk.healthylife.hms.config.EntityValidator;
import lk.healthylife.hms.dto.CommonReferenceDTO;
import lk.healthylife.hms.dto.MedicineDTO;
import lk.healthylife.hms.dto.PrescriptionMedicineDTO;
import lk.healthylife.hms.exception.NoRequiredInfoException;
import lk.healthylife.hms.exception.OperationException;
import lk.healthylife.hms.service.CommonReferenceService;
import lk.healthylife.hms.service.MedicineService;
import lk.healthylife.hms.service.PrescriptionMedicineService;
import lk.healthylife.hms.service.SymptomService;
import lk.healthylife.hms.util.constant.CommonReferenceCodes;
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
import static lk.healthylife.hms.util.constant.CommonReferenceCodes.*;
import static lk.healthylife.hms.util.constant.CommonReferenceTypeCodes.*;

@Slf4j
@Service
public class PrescriptionMedicineServiceImpl extends EntityValidator implements PrescriptionMedicineService {

    private final AuditorAwareImpl auditorAware;
    private final DataSource dataSource;

    private final MedicineService medicineService;
    private final SymptomService symptomService;
    private final CommonReferenceService commonReferenceService;

    @PersistenceContext
    private EntityManager entityManager;

    public PrescriptionMedicineServiceImpl(AuditorAwareImpl auditorAware,
                                           DataSource dataSource,
                                           MedicineService medicineService,
                                           SymptomService symptomService,
                                           CommonReferenceService commonReferenceService) {
        this.auditorAware = auditorAware;
        this.dataSource = dataSource;
        this.medicineService = medicineService;
        this.symptomService = symptomService;
        this.commonReferenceService = commonReferenceService;
    }

    @Transactional
    @Override
    public PrescriptionMedicineDTO createPrescriptionMedicine(PrescriptionMedicineDTO prescriptionMedicineDTO){

        BigInteger insertedRowId = null;

        validateEntity(prescriptionMedicineDTO);

        try (Connection connection = dataSource.getConnection()) {

            CallableStatement statement = connection
                    .prepareCall("{CALL INSERT INTO T_TR_PRESCRIPTION_MEDICINE (PRMD_PRESCRIPTION_ID, PRMD_SYMPTOM_ID,\n" +
                            "PRMD_MEDICINE_ID, PRMD_REMARKS, PRMD_STATUS, CREATED_DATE, CREATED_USER_CODE, PRMD_BRANCH_ID,\n" +
                            "PRMD_MODE, PRMD_PRESCRIBE_DURATION_DAYS, PRMD_QUANTITY, PRMD_MEAL_TIME)\n" +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING PRMD_ID INTO ?}");

            statement.setLong(1, prescriptionMedicineDTO.getPrescriptionId());
            statement.setLong(2, prescriptionMedicineDTO.getSymptomId());
            statement.setLong(3, prescriptionMedicineDTO.getMedicineId());
            statement.setString(4, prescriptionMedicineDTO.getRemarks());
            statement.setShort(5, STATUS_ACTIVE.getShortValue());
            statement.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(7, auditorAware.getCurrentAuditor().get());
            statement.setLong(8, captureBranchIds().get(0));
            statement.setString(9, prescriptionMedicineDTO.getPrescribeMode());
            statement.setInt(10, prescriptionMedicineDTO.getPrescribeDurationDays());
            statement.setInt(11, calculatePrescribedMedicineQuantity(prescriptionMedicineDTO));
            statement.setString(12, prescriptionMedicineDTO.getPrescribeMealTime());

            statement.registerOutParameter( 13, Types.BIGINT);

            int updateCount = statement.executeUpdate();

            if (updateCount > 0)
                insertedRowId = BigInteger.valueOf(statement.getLong(13));

        } catch (Exception e) {
            log.error("Error while persisting Prescription Medicine : " + e.getMessage());
            throw new OperationException("Error while persisting Prescription Medicine");
        }

        return getPrescriptionMedicineById(insertedRowId.longValue());
    }

    private Integer calculatePrescribedMedicineQuantity(PrescriptionMedicineDTO prescriptionMedicineDTO) {
        CommonReferenceDTO commonReferenceDTO = commonReferenceService
                .getByCmrfCodeAndCmrtCode(PRESCRIBE_MODES.getValue(), prescriptionMedicineDTO.getPrescribeMode());

        MedicineDTO medicineDTO = medicineService.getMedicineById(prescriptionMedicineDTO.getMedicineId());

        return commonReferenceDTO.getNumberValue() * prescriptionMedicineDTO.getPrescribeDurationDays() * medicineDTO.getDose().intValue();
    }

    @Override
    public PrescriptionMedicineDTO getPrescriptionMedicineById(Long prescriptionMedicineId) {

        PrescriptionMedicineDTO prescriptionMedicineDTO = null;

        validatePrescriptionMedicineId(prescriptionMedicineId);

        final String queryString = "SELECT pm.PRMD_ID, pm.PRMD_PRESCRIPTION_ID, pm.PRMD_SYMPTOM_ID, pm.PRMD_MEDICINE_ID, pm.PRMD_REMARKS,\n" +
                "pm.PRMD_STATUS, pm.CREATED_DATE, pm.CREATED_USER_CODE, pm.LAST_MOD_USER_CODE, br.BRNH_NAME AS BRANCH_NAME, pm.PRMD_BRANCH_ID,\n" +
                "pm.PRMD_MODE, pm.PRMD_PRESCRIBE_DURATION_DAYS, pm.PRMD_QUANTITY, pm.PRMD_MEAL_TIME,\n" +
                "prescribedMode.CMRF_DESCRIPTION AS PRESCRIBE_MODE, mealTime.CMRF_DESCRIPTION AS PRESCRIBE_MEAL_TIME\n" +
                "FROM T_TR_PRESCRIPTION_MEDICINE pm\n" +
                "INNER JOIN T_RF_BRANCH br ON pm.PRMD_BRANCH_ID = br.BRNH_ID\n" +
                "LEFT JOIN T_RF_COMMON_REFERENCE prescribedMode ON pm.PRMD_MODE = prescribedMode.CMRF_CODE\n" +
                "LEFT JOIN T_RF_COMMON_REFERENCE mealTime ON pm.PRMD_MEAL_TIME = mealTime.CMRF_CODE\n" +
                "WHERE pm.PRMD_ID = :prescriptionMedicineId AND pm.PRMD_STATUS = :status AND pm.PRMD_BRANCH_ID IN (:branchIdList)";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("prescriptionMedicineId", prescriptionMedicineId);

        List<Map<String,Object>> result = extractResultSet(query);

        if (result.size() == 0)
            return null;

        for (Map<String,Object> prescriptionMedicine : result) {

            prescriptionMedicineDTO = new PrescriptionMedicineDTO();

            createDTO(prescriptionMedicineDTO, prescriptionMedicine);
            prescriptionMedicineDTO.setMedicine(medicineService.getMedicineById(prescriptionMedicineDTO.getMedicineId()));
            prescriptionMedicineDTO.setSymptom(symptomService.getSymptomById(prescriptionMedicineDTO.getSymptomId()));
        }

        return prescriptionMedicineDTO;
    }

    @Override
    public List<PrescriptionMedicineDTO> getPrescriptionMedicineListByPrescriptionId(Long prescriptionId) {

        List<PrescriptionMedicineDTO> prescriptionMedicineDTOList = new ArrayList<>();

        if(prescriptionId == null)
            throw new NoRequiredInfoException("Prescription Id is Required");

        final String queryString = "SELECT pm.PRMD_ID, pm.PRMD_PRESCRIPTION_ID, pm.PRMD_SYMPTOM_ID, pm.PRMD_MEDICINE_ID, pm.PRMD_REMARKS,\n" +
                "pm.PRMD_STATUS, pm.CREATED_DATE, pm.CREATED_USER_CODE, pm.LAST_MOD_USER_CODE, br.BRNH_NAME AS BRANCH_NAME, pm.PRMD_BRANCH_ID,\n" +
                "pm.PRMD_MODE, pm.PRMD_PRESCRIBE_DURATION_DAYS, pm.PRMD_QUANTITY, pm.PRMD_MEAL_TIME,\n" +
                "prescribedMode.CMRF_DESCRIPTION AS PRESCRIBE_MODE, mealTime.CMRF_DESCRIPTION AS PRESCRIBE_MEAL_TIME\n" +
                "FROM T_TR_PRESCRIPTION_MEDICINE pm\n" +
                "INNER JOIN T_RF_BRANCH br ON pm.PRMD_BRANCH_ID = br.BRNH_ID\n" +
                "LEFT JOIN T_RF_COMMON_REFERENCE prescribedMode ON pm.PRMD_MODE = prescribedMode.CMRF_CODE\n" +
                "LEFT JOIN T_RF_COMMON_REFERENCE mealTime ON pm.PRMD_MEAL_TIME = mealTime.CMRF_CODE\n" +
                "WHERE pm.PRMD_PRESCRIPTION_ID = :prescriptionId AND pm.PRMD_STATUS = :status AND pm.PRMD_BRANCH_ID IN (:branchIdList)";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("prescriptionId", prescriptionId);

        List<Map<String,Object>> result = extractResultSet(query);

        if (result.size() == 0)
            return null;

        for (Map<String,Object> prescriptionMedicine : result) {

            PrescriptionMedicineDTO prescriptionMedicineDTO = new PrescriptionMedicineDTO();

            createDTO(prescriptionMedicineDTO, prescriptionMedicine);
            prescriptionMedicineDTO.setMedicine(medicineService.getMedicineById(prescriptionMedicineDTO.getMedicineId()));
            prescriptionMedicineDTO.setSymptom(symptomService.getSymptomById(prescriptionMedicineDTO.getSymptomId()));

            prescriptionMedicineDTOList.add(prescriptionMedicineDTO);
        }

        return prescriptionMedicineDTOList;
    }

    @Transactional
    @Override
    public Boolean removePrescriptionMedicineByPrescription(Long prescriptionId) {

        final Query query = entityManager.createNativeQuery("UPDATE T_TR_PRESCRIPTION_MEDICINE SET PRMD_STATUS = :statusInActive\n" +
                        "WHERE PRMD_PRESCRIPTION_ID = :prescriptionId AND PRMD_STATUS = :statusActive AND PRMD_BRANCH_ID IN (:branchIdList)")
                .setParameter("statusInActive", STATUS_INACTIVE.getShortValue())
                .setParameter("prescriptionId", prescriptionId)
                .setParameter("statusActive", STATUS_ACTIVE.getShortValue())
                .setParameter("branchIdList", captureBranchIds());

        return query.executeUpdate() == 0 ? false : true;
    }

    private void validatePrescriptionMedicineId(Long prescriptionMedicineId) {
        if(prescriptionMedicineId == null)
            throw new NoRequiredInfoException("Prescription Medicine Id is Required");
    }

    private void createDTO(PrescriptionMedicineDTO prescriptionMedicineDTO, Map<String,Object> prescriptionMedicine) {

        prescriptionMedicineDTO.setPrescriptionMedicineId(extractLongValue(String.valueOf(prescriptionMedicine.get("PRMD_ID"))));
        prescriptionMedicineDTO.setPrescriptionId(extractLongValue(String.valueOf(prescriptionMedicine.get("PRMD_PRESCRIPTION_ID"))));
        prescriptionMedicineDTO.setSymptomId(extractLongValue(String.valueOf(prescriptionMedicine.get("PRMD_SYMPTOM_ID"))));
        prescriptionMedicineDTO.setMedicineId(extractLongValue(String.valueOf(prescriptionMedicine.get("PRMD_MEDICINE_ID"))));
        prescriptionMedicineDTO.setRemarks(extractValue(String.valueOf(prescriptionMedicine.get("PRMD_REMARKS"))));
        prescriptionMedicineDTO.setStatus(extractShortValue(String.valueOf(prescriptionMedicine.get("PRMD_STATUS"))));
        prescriptionMedicineDTO.setCreatedDate(extractDateTime(String.valueOf(prescriptionMedicine.get("CREATED_DATE"))));
        prescriptionMedicineDTO.setCreatedUserCode(extractValue(String.valueOf(prescriptionMedicine.get("CREATED_USER_CODE"))));
        prescriptionMedicineDTO.setLastUpdatedDate(extractDateTime(String.valueOf(prescriptionMedicine.get("LAST_MOD_DATE"))));
        prescriptionMedicineDTO.setLastUpdatedUserCode(extractValue(String.valueOf(prescriptionMedicine.get("LAST_MOD_USER_CODE"))));
        prescriptionMedicineDTO.setBranchId(extractLongValue(String.valueOf(prescriptionMedicine.get("PRMD_BRANCH_ID"))));
        prescriptionMedicineDTO.setBranchName(extractValue(String.valueOf(prescriptionMedicine.get("BRANCH_NAME"))));
        prescriptionMedicineDTO.setPrescribeMode(extractValue(String.valueOf(prescriptionMedicine.get("PRMD_MODE"))));
        prescriptionMedicineDTO.setPrescribeModeName(extractValue(String.valueOf(prescriptionMedicine.get("PRESCRIBE_MODE"))));
        prescriptionMedicineDTO.setPrescribeMealTime(extractValue(String.valueOf(prescriptionMedicine.get("PRMD_MEAL_TIME"))));
        prescriptionMedicineDTO.setPrescribeMealTimeName(extractValue(String.valueOf(prescriptionMedicine.get("PRESCRIBE_MEAL_TIME"))));
        prescriptionMedicineDTO.setPrescribeDurationDays(extractIntegerValue(String.valueOf(prescriptionMedicine.get("PRMD_PRESCRIBE_DURATION_DAYS"))));
        prescriptionMedicineDTO.setQuantity(extractIntegerValue(String.valueOf(prescriptionMedicine.get("PRMD_QUANTITY"))));
    }
}

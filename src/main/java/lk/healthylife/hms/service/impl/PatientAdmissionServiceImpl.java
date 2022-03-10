package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.config.AuditorAwareImpl;
import lk.healthylife.hms.config.EntityValidator;
import lk.healthylife.hms.dto.PaginatedEntity;
import lk.healthylife.hms.dto.PatientAdmissionDTO;
import lk.healthylife.hms.dto.PatientConditionDTO;
import lk.healthylife.hms.exception.NoRequiredInfoException;
import lk.healthylife.hms.exception.OperationException;
import lk.healthylife.hms.service.PatientAdmissionService;
import lk.healthylife.hms.service.PatientConditionService;
import lk.healthylife.hms.service.TreatmentAdviceService;
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

import static lk.healthylife.hms.util.constant.CommonReferenceCodes.PATIENT_CONDITION_ADMITTING;
import static lk.healthylife.hms.util.constant.Constants.STATUS_ACTIVE;
import static lk.healthylife.hms.util.constant.Constants.STATUS_INACTIVE;

@Slf4j
@Service
public class PatientAdmissionServiceImpl extends EntityValidator implements PatientAdmissionService {

    private final AuditorAwareImpl auditorAware;
    private final DataSource dataSource;

    private final PatientConditionService patientConditionService;
    private final TreatmentAdviceService treatmentAdviceService;

    @PersistenceContext
    private EntityManager entityManager;

    public PatientAdmissionServiceImpl(AuditorAwareImpl auditorAware,
                                       DataSource dataSource,
                                       PatientConditionService patientConditionService,
                                       TreatmentAdviceService treatmentAdviceService) {
        this.auditorAware = auditorAware;
        this.dataSource = dataSource;
        this.patientConditionService = patientConditionService;
        this.treatmentAdviceService = treatmentAdviceService;
    }

    @Transactional
    @Override
    public PatientAdmissionDTO admitPatient(PatientAdmissionDTO patientAdmissionDTO) {

        BigInteger insertedRowId = null;

        validateEntity(patientAdmissionDTO);

        try (Connection connection = dataSource.getConnection()) {

            CallableStatement statement = connection
                    .prepareCall("{CALL INSERT INTO T_TR_PATIENT_ADMISSION (PTAD_PATIENT_CODE, PTAD_ROOM_ID, PTAD_ADMITTED_DATE,\n" +
                            "PTAD_REMARKS, PTAD_STATUS, CREATED_DATE, CREATED_USER_CODE, PTAD_BRANCH_ID)\n" +
                            "VALUES(?, ?, ?, ?, ?, ?, ?, ?) RETURNING PTAD_ID INTO ?}");

            statement.setString(1, patientAdmissionDTO.getPatientCode());
            statement.setLong(2, patientAdmissionDTO.getRoomId());
            statement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(4, patientAdmissionDTO.getRemarks());
            statement.setShort(5, STATUS_ACTIVE.getShortValue());
            statement.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(7, auditorAware.getCurrentAuditor().get());
            statement.setLong(8, captureBranchIds().get(0));

            statement.registerOutParameter( 9, Types.BIGINT);

            int updateCount = statement.executeUpdate();

            if (updateCount > 0)
                insertedRowId = BigInteger.valueOf(statement.getLong(9));

        } catch (Exception e) {
            log.error("Error while persisting Admission : " + e.getMessage());
            throw new OperationException("Error while persisting Admission");
        }

        for(PatientConditionDTO condition : patientAdmissionDTO.getConditions()) {
            condition.setAdmissionId(insertedRowId.longValue());
            condition.setConditionWhen(PATIENT_CONDITION_ADMITTING.getValue());
            patientConditionService.addPatientCondition(condition);
        }

        return getAdmissionById(insertedRowId.longValue());
    }

    @Transactional
    @Override
    public Boolean approveAdmission(Long admissionId) {
        validateAdmissionId(admissionId);

        final Query query = entityManager
                .createNativeQuery("UPDATE T_TR_PATIENT_ADMISSION SET PTAD_ADMISSION_APPROVED_DOCTOR = :approvedDoctor,\n" +
                        "LAST_MOD_DATE = :lastModDate, LAST_MOD_USER_CODE = :lastModUser \n" +
                        "WHERE PTAD_ID = :admissionId AND PTAD_STATUS = :status AND PTAD_BRANCH_ID IN (:branchIdList)")
                .setParameter("approvedDoctor", auditorAware.getCurrentAuditor().get())
                .setParameter("lastModDate", LocalDateTime.now())
                .setParameter("lastModUser", auditorAware.getCurrentAuditor().get())
                .setParameter("status", STATUS_ACTIVE.getShortValue())
                .setParameter("admissionId", admissionId)
                .setParameter("branchIdList", captureBranchIds());

        return query.executeUpdate() > 0;
    }

    @Transactional
    @Override
    public Boolean dischargePatient(Long admissionId) {

        validateAdmissionId(admissionId);

        final Query query = entityManager
                .createNativeQuery("UPDATE T_TR_PATIENT_ADMISSION SET PTAD_DISCHARGED_DATE = :dischargeDate, PTAD_DISCHARGE_APPROVED_DOCTOR = :approvedDoctor,\n" +
                        "LAST_MOD_DATE = :lastModDate, LAST_MOD_USER_CODE = :lastModUser \n" +
                        "WHERE PTAD_ID = :admissionId AND PTAD_STATUS = :status AND PTAD_BRANCH_ID IN (:branchIdList)")
                .setParameter("dischargeDate", LocalDateTime.now())
                .setParameter("approvedDoctor", auditorAware.getCurrentAuditor().get())
                .setParameter("lastModDate", LocalDateTime.now())
                .setParameter("lastModUser", auditorAware.getCurrentAuditor().get())
                .setParameter("status", STATUS_ACTIVE.getShortValue())
                .setParameter("admissionId", admissionId)
                .setParameter("branchIdList", captureBranchIds());

        query.executeUpdate();

        return query.executeUpdate() > 0;
    }

    @Override
    public PatientAdmissionDTO getAdmissionById(Long admissionId) {

        PatientAdmissionDTO patientAdmissionDTO = null;

        validateAdmissionId(admissionId);

        final String queryString = "SELECT pa.PTAD_ID, pa.PTAD_PATIENT_CODE, pa.PTAD_ROOM_ID, pa.PTAD_ADMISSION_APPROVED_DOCTOR,\n" +
                "pa.PTAD_ADMITTED_DATE, pa.PTAD_DISCHARGED_DATE, pa.PTAD_DISCHARGE_APPROVED_DOCTOR,\n" +
                "pa.PTAD_REMARKS, pa.PTAD_STATUS, pa.CREATED_DATE, pa.CREATED_USER_CODE,\n" +
                "pa.LAST_MOD_DATE, pa.LAST_MOD_USER_CODE, pa.PTAD_BRANCH_ID, br.BRNH_NAME AS BRANCH_NAME\n" +
                "FROM T_TR_PATIENT_ADMISSION pa\n" +
                "INNER JOIN T_RF_BRANCH br ON pa.PTAD_BRANCH_ID = br.BRNH_ID\n" +
                "WHERE pa.PTAD_ID = :admissionId AND pa.PTAD_STATUS = :status AND pa.PTAD_BRANCH_ID IN (:branchIdList)";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("admissionId", admissionId);

        List<Map<String,Object>> result = extractResultSet(query);

        if (result.size() == 0)
            return null;

        for (Map<String,Object> admission : result) {
            patientAdmissionDTO = new PatientAdmissionDTO();
            createDTO(patientAdmissionDTO, admission);

            patientAdmissionDTO.setConditions(patientConditionService.getPatientConditionListByAdmission(admissionId));
            patientAdmissionDTO.setTreatmentAdvices(treatmentAdviceService.getAdvicesByAdmissionId(admissionId));
        }

        return patientAdmissionDTO;
    }

    @Override
    public List<PatientAdmissionDTO> getAdmissionListForDropDown() {

        List<PatientAdmissionDTO> patientAdmissionDTOList = new ArrayList<>();

        final String queryString = "SELECT pa.PTAD_ID, pa.PTAD_ADMITTED_DATE, patient.PRTY_NAME AS PATIENT_NAME\n" +
                "FROM T_TR_PATIENT_ADMISSION pa\n" +
                "INNER JOIN T_MS_PARTY patient ON pa.PTAD_PATIENT_CODE = patient.PRTY_CODE\n" +
                "WHERE pa.PTAD_STATUS = :status AND pa.PTAD_BRANCH_ID IN (:branchIdList)";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());

        List<Map<String,Object>> result = extractResultSet(query);

        for (Map<String,Object> admission : result) {
            PatientAdmissionDTO patientAdmissionDTO = new PatientAdmissionDTO();
            createDTO(patientAdmissionDTO, admission);

            patientAdmissionDTOList.add(patientAdmissionDTO);
        }

        return patientAdmissionDTOList;
    }

    @Override
    public PaginatedEntity admissionPaginatedSearch(String patientName, String roomNo, Integer page, Integer size) {

        PaginatedEntity paginatedAdmissionList = null;
        List<PatientAdmissionDTO> admissionList = null;

        page = validatePaginateIndexes(page, size);

        roomNo = roomNo.isEmpty() ? null : roomNo;

        final String countQueryString = "SELECT COUNT(pa.PTAD_ID)\n" +
                "FROM T_TR_PATIENT_ADMISSION pa\n" +
                "INNER JOIN T_MS_PARTY patient ON pa.PTAD_PATIENT_CODE = patient.PRTY_CODE\n" +
                "INNER JOIN T_MS_ROOM room ON pa.PTAD_ROOM_ID = room.ROOM_ID\n" +
                "WHERE pa.PTAD_STATUS = :status AND pa.PTAD_BRANCH_ID IN (:branchIdList)\n" +
                "AND (upper(patient.PRTY_NAME) LIKE ('%'||upper(:patientName)||'%'))\n" +
                "AND (:roomNo IS NULL OR (:roomNo IS NOT NULL) AND room.ROOM_NO = :roomNo)\n";

        Query query = entityManager.createNativeQuery(countQueryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("patientName", patientName);
        query.setParameter("roomNo", roomNo);
        query.setParameter("branchIdList", captureBranchIds());

        final int selectedRecordCount = ((Number) query.getSingleResult()).intValue();

        query = null;

        final String queryString = "SELECT pa.PTAD_ID, pa.PTAD_PATIENT_CODE, pa.PTAD_ROOM_ID, pa.PTAD_ADMISSION_APPROVED_DOCTOR,\n" +
                "pa.PTAD_ADMITTED_DATE, pa.PTAD_DISCHARGED_DATE, pa.PTAD_DISCHARGE_APPROVED_DOCTOR,\n" +
                "pa.PTAD_REMARKS, pa.PTAD_STATUS, pa.CREATED_DATE, pa.CREATED_USER_CODE,\n" +
                "pa.LAST_MOD_DATE, pa.LAST_MOD_USER_CODE, pa.PTAD_BRANCH_ID, br.BRNH_NAME AS BRANCH_NAME,\n" +
                "patient.PRTY_NAME AS PATIENT_NAME, room.ROOM_NO \n" +
                "FROM T_TR_PATIENT_ADMISSION pa\n" +
                "INNER JOIN T_RF_BRANCH br ON pa.PTAD_BRANCH_ID = br.BRNH_ID\n" +
                "INNER JOIN T_MS_PARTY patient ON pa.PTAD_PATIENT_CODE = patient.PRTY_CODE\n" +
                "INNER JOIN T_MS_ROOM room ON pa.PTAD_ROOM_ID = room.ROOM_ID\n" +
                "WHERE pa.PTAD_STATUS = :status AND pa.PTAD_BRANCH_ID IN (:branchIdList)\n" +
                "AND (upper(patient.PRTY_NAME) LIKE ('%'||upper(:patientName)||'%'))\n" +
                "AND (:roomNo IS NULL OR (:roomNo IS NOT NULL) AND room.ROOM_NO = :roomNo)\n" +
                "ORDER BY pa.CREATED_DATE OFFSET :page ROWS FETCH NEXT :size ROWS ONLY";

        query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("patientName", patientName);
        query.setParameter("roomNo", roomNo);
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("size", size);
        query.setParameter("page", size * page);

        List<Map<String,Object>> result = extractResultSet(query);

        paginatedAdmissionList = new PaginatedEntity();
        admissionList = new ArrayList<>();

        for (Map<String,Object> admission : result) {

            PatientAdmissionDTO patientAdmissionDTO = new PatientAdmissionDTO();

            createDTO(patientAdmissionDTO, admission);

            admissionList.add(patientAdmissionDTO);
        }

        paginatedAdmissionList
                .setTotalNoOfPages(getTotalNoOfPages(selectedRecordCount, size));
        paginatedAdmissionList.setTotalNoOfRecords(Long.valueOf(selectedRecordCount));
        paginatedAdmissionList.setEntities(admissionList);

        return paginatedAdmissionList;
    }

    @Transactional
    @Override
    public Boolean removeAdmission(Long admissionId) {

        validateAdmissionId(admissionId);

        final Query query = entityManager.createNativeQuery("UPDATE T_TR_PATIENT_ADMISSION SET PTAD_STATUS = :statusInActive \n" +
                        "WHERE PTAD_ID = :admissionId AND PTAD_STATUS = :statusActive AND PTAD_BRANCH_ID IN (:branchIdList)")
                .setParameter("statusInActive", STATUS_INACTIVE.getShortValue())
                .setParameter("admissionId", admissionId)
                .setParameter("statusActive", STATUS_ACTIVE.getShortValue())
                .setParameter("branchIdList", captureBranchIds());

        return query.executeUpdate() == 0 ? false : true;
    }

    private void validateAdmissionId(Long admissionId) {
        if(admissionId == null)
            throw new NoRequiredInfoException("Admission Id is Required");
    }

    private void createDTO(PatientAdmissionDTO patientAdmissionDTO, Map<String,Object> admission) {

        patientAdmissionDTO.setPatientAdmissionId(extractLongValue(String.valueOf(admission.get("PTAD_ID"))));
        patientAdmissionDTO.setPatientCode(extractValue(String.valueOf(admission.get("PTAD_PATIENT_CODE"))));
        patientAdmissionDTO.setPatientName(extractValue(String.valueOf(admission.get("PATIENT_NAME"))));
        patientAdmissionDTO.setRoomId(extractLongValue(String.valueOf(admission.get("PTAD_ROOM_ID"))));
        patientAdmissionDTO.setRoomNo(extractValue(String.valueOf(admission.get("ROOM_NO"))));
        patientAdmissionDTO.setAdmissionApprovedDoctor(extractValue(String.valueOf(admission.get("PTAD_ADMISSION_APPROVED_DOCTOR"))));
        patientAdmissionDTO.setAdmittedDate(extractDateTime(String.valueOf(admission.get("PTAD_ADMITTED_DATE"))));
        patientAdmissionDTO.setDischargeApprovedDoctor(extractValue(String.valueOf(admission.get("PTAD_DISCHARGE_APPROVED_DOCTOR"))));
        patientAdmissionDTO.setRemarks(extractValue(String.valueOf(admission.get("PTAD_REMARKS"))));
        patientAdmissionDTO.setDischargedDate(extractDateTime(String.valueOf(admission.get("PTAD_DISCHARGED_DATE"))));
        patientAdmissionDTO.setStatus(extractShortValue(String.valueOf(admission.get("PTAD_STATUS"))));
        patientAdmissionDTO.setCreatedDate(extractDateTime(String.valueOf(admission.get("CREATED_DATE"))));
        patientAdmissionDTO.setCreatedUserCode(extractValue(String.valueOf(admission.get("CREATED_USER_CODE"))));
        patientAdmissionDTO.setLastUpdatedDate(extractDateTime(String.valueOf(admission.get("LAST_MOD_DATE"))));
        patientAdmissionDTO.setLastUpdatedUserCode(extractValue(String.valueOf(admission.get("LAST_MOD_USER_CODE"))));
        patientAdmissionDTO.setBranchId(extractLongValue(String.valueOf(admission.get("PTAD_BRANCH_ID"))));
        patientAdmissionDTO.setBranchName(extractValue(String.valueOf(admission.get("BRANCH_NAME"))));
    }
}

package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.config.AuditorAwareImpl;
import lk.healthylife.hms.config.EntityValidator;
import lk.healthylife.hms.dto.DoctorSurgeryDTO;
import lk.healthylife.hms.dto.PaginatedEntity;
import lk.healthylife.hms.dto.PatientSurgeryDTO;
import lk.healthylife.hms.exception.NoRequiredInfoException;
import lk.healthylife.hms.exception.OperationException;
import lk.healthylife.hms.service.PartyService;
import lk.healthylife.hms.service.PatientSurgeryService;
import lk.healthylife.hms.service.RoomService;
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
import java.util.List;
import java.util.Map;

import static lk.healthylife.hms.util.constant.Constants.STATUS_ACTIVE;
import static lk.healthylife.hms.util.constant.Constants.STATUS_INACTIVE;

@Slf4j
@Service
public class PatientSurgeryServiceImpl extends EntityValidator implements PatientSurgeryService {

    private final SurgeryService surgeryService;
    private final PartyService partyService;
    private final RoomService roomService;

    private final AuditorAwareImpl auditorAware;
    private final DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    public PatientSurgeryServiceImpl(SurgeryService surgeryService,
                                     PartyService partyService,
                                     RoomService roomService,
                                     AuditorAwareImpl auditorAware,
                                     DataSource dataSource) {
        this.surgeryService = surgeryService;
        this.partyService = partyService;
        this.roomService = roomService;
        this.auditorAware = auditorAware;
        this.dataSource = dataSource;
    }

    @Transactional
    @Override
    public PatientSurgeryDTO addPatientToSurgery(PatientSurgeryDTO patientSurgeryDTO) {

        BigInteger insertedRowId = null;

        validateEntity(patientSurgeryDTO);

        try (Connection connection = dataSource.getConnection()) {

            CallableStatement statement = connection
                    .prepareCall("{CALL INSERT INTO T_TR_PATIENT_SURGERY(PTSG_SURGERY_ID, PTSG_PATIENT_CODE, PTSG_ADMISSION_ID, \n" +
                            "PTSG_ROOM_ID, PTSG_STARTED_DATE_TIME, CREATED_DATE, CREATED_USER_CODE, PTSG_BRANCH_ID, PTSG_STATUS)\n" +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING PTSG_ID INTO ?}");

            statement.setLong(1, patientSurgeryDTO.getSurgeryId());
            statement.setString(2, patientSurgeryDTO.getPatientCode());
            statement.setLong(3, patientSurgeryDTO.getAdmissionId());
            statement.setLong(4, patientSurgeryDTO.getOperationRoomId());
            statement.setTimestamp(5, Timestamp.valueOf(patientSurgeryDTO.getStartedDateTime()));
            statement.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(7, auditorAware.getCurrentAuditor().get());
            statement.setLong(8, captureBranchIds().get(0));
            statement.setShort(9, STATUS_ACTIVE.getShortValue());

            statement.registerOutParameter( 10, Types.BIGINT);

            int updateCount = statement.executeUpdate();

            if (updateCount > 0)
                insertedRowId = BigInteger.valueOf(statement.getLong(10));

        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error while persisting Patient Surgery : " + e.getMessage());
            throw new OperationException("Error while persisting Patient Surgery");
        }

        for(DoctorSurgeryDTO  doctors : patientSurgeryDTO.getDoctorsInSurgery()) {
            doctors.setPatientSurgeryId(insertedRowId.longValue());
            assignDoctorToSurgery(doctors);
        }

        return getPatientSurgeryById(insertedRowId.longValue());
    }

    @Transactional
    @Override
    public PatientSurgeryDTO finishPatientSurgery(Long patientSurgeryId) {

        validatePatientSurgeryId(patientSurgeryId);

        final Query query = entityManager.createNativeQuery("UPDATE T_TR_PATIENT_SURGERY SET PTSG_ENDED_DATE_TIME = :dateTime\n" +
                        "WHERE PTSG_ID = :patientSurgeryId AND PTSG_STATUS = :statusActive AND PTSG_BRANCH_ID IN (:branchIdList)")
                .setParameter("patientSurgeryId", patientSurgeryId)
                .setParameter("statusActive", STATUS_ACTIVE.getShortValue())
                .setParameter("branchIdList", captureBranchIds())
                .setParameter("dateTime", Timestamp.valueOf(LocalDateTime.now()));

        query.executeUpdate();

        return getPatientSurgeryById(patientSurgeryId);
    }

    @Override
    public PatientSurgeryDTO getPatientSurgeryById(Long patientSurgeryId) {

        PatientSurgeryDTO patientSurgeryDTO = null;

        validatePatientSurgeryId(patientSurgeryId);

        final String queryString = "SELECT ps.PTSG_ID, ps.PTSG_SURGERY_ID, ps.PTSG_PATIENT_CODE, ps.PTSG_ADMISSION_ID, ps.PTSG_ROOM_ID, \n" +
                "ps.PTSG_STARTED_DATE_TIME, ps.PTSG_ENDED_DATE_TIME, ps.CREATED_DATE, ps.CREATED_USER_CODE, \n" +
                "ps.PTSG_BRANCH_ID, ps.PTSG_STATUS, ps.LAST_MOD_DATE, ps.LAST_MOD_USER_CODE, br.BRNH_NAME AS BRANCH_NAME\n" +
                "FROM T_TR_PATIENT_SURGERY ps\n" +
                "INNER JOIN T_RF_BRANCH br ON ps.PTSG_BRANCH_ID = br.BRNH_ID\n" +
                "WHERE ps.PTSG_ID = :patientSurgeryId AND ps.PTSG_STATUS = :status AND ps.PTSG_BRANCH_ID IN (:branchIdList)";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("patientSurgeryId", patientSurgeryId);

        List<Map<String,Object>> result = extractResultSet(query);

        if (result.size() == 0)
            return null;

        for (Map<String,Object> patientSurgery : result) {

            patientSurgeryDTO = new PatientSurgeryDTO();

            createDTO(patientSurgeryDTO, patientSurgery);

            patientSurgeryDTO.setSurgery(surgeryService.getSurgeryById(patientSurgeryDTO.getSurgeryId()));
            patientSurgeryDTO.setPatient(partyService.getPartyByPartyCode(patientSurgeryDTO.getPatientCode()));
            patientSurgeryDTO.setOperationRoom(roomService.getRoomByIdOrNo(patientSurgeryDTO.getOperationRoomId(), null));
            patientSurgeryDTO.setDoctorsInSurgery(getDoctorsByPatientSurgeryId(patientSurgeryId));
        }

        return patientSurgeryDTO;
    }

    @Override
    public PaginatedEntity patientSurgeryPaginatedSearch(String patientName, String doctorCode, String surgeryName,
                                                         Integer page, Integer size) {

        PaginatedEntity paginatedPatientSurgeryList = null;
        List<PatientSurgeryDTO> patientSurgeryList = null;

        page = validatePaginateIndexes(page, size);

        doctorCode = doctorCode.isEmpty() ? null : doctorCode;

        final String countQueryString = "SELECT COUNT(ps.PTSG_ID)" +
                "FROM T_TR_PATIENT_SURGERY ps\n" +
                "INNER JOIN T_MS_SURGERY s ON ps.PTSG_SURGERY_ID = s.SRGY_ID\n" +
                "INNER JOIN T_MS_PARTY p ON ps.PTSG_PATIENT_CODE = p.PRTY_CODE\n" +
                "WHERE ps.PTSG_STATUS = :status AND ps.PTSG_BRANCH_ID IN (:branchIdList)\n" +
                "AND (upper(p.PRTY_NAME) LIKE ('%'||upper(:patientName)||'%'))\n" +
                "AND (upper(s.SRGY_NAME) LIKE ('%'||upper(:surgeryName)||'%'))\n";

        Query query = entityManager.createNativeQuery(countQueryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("patientName", patientName);
        query.setParameter("surgeryName", surgeryName);
        query.setParameter("branchIdList", captureBranchIds());

        final int selectedRecordCount = ((Number) query.getSingleResult()).intValue();

        query = null;

        final String queryString = "SELECT ps.PTSG_ID, ps.PTSG_SURGERY_ID, ps.PTSG_PATIENT_CODE, ps.PTSG_ADMISSION_ID, ps.PTSG_ROOM_ID,\n" +
                "ps.PTSG_STARTED_DATE_TIME, ps.PTSG_ENDED_DATE_TIME, ps.CREATED_DATE, ps.CREATED_USER_CODE,\n" +
                "ps.PTSG_BRANCH_ID, ps.PTSG_STATUS, ps.LAST_MOD_DATE, ps.LAST_MOD_USER_CODE, br.BRNH_NAME AS BRANCH_NAME,\n" +
                "s.SRGY_NAME AS SURGERY_NAME, p.PRTY_NAME AS PATIENT_NAME\n" +
                "FROM T_TR_PATIENT_SURGERY ps\n" +
                "INNER JOIN T_RF_BRANCH br ON ps.PTSG_BRANCH_ID = br.BRNH_ID\n" +
                "INNER JOIN T_MS_SURGERY s ON ps.PTSG_SURGERY_ID = s.SRGY_ID\n" +
                "INNER JOIN T_MS_PARTY p ON ps.PTSG_PATIENT_CODE = p.PRTY_CODE\n" +
                "WHERE ps.PTSG_STATUS = :status AND ps.PTSG_BRANCH_ID IN (:branchIdList)\n" +
                "AND (upper(p.PRTY_NAME) LIKE ('%'||upper(:patientName)||'%'))\n" +
                "AND (upper(s.SRGY_NAME) LIKE ('%'||upper(:surgeryName)||'%'))\n" +
                "ORDER BY ps.CREATED_DATE OFFSET :page ROWS FETCH NEXT :size ROWS ONLY";

        query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("patientName", patientName);
        query.setParameter("surgeryName", surgeryName);
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("size", size);
        query.setParameter("page", size * page);

        List<Map<String,Object>> result = extractResultSet(query);

        paginatedPatientSurgeryList = new PaginatedEntity();
        patientSurgeryList = new ArrayList<>();

        for (Map<String,Object> patientSurgery : result) {

            PatientSurgeryDTO patientSurgeryDTO = new PatientSurgeryDTO();

            createDTO(patientSurgeryDTO, patientSurgery);

            patientSurgeryList.add(patientSurgeryDTO);
        }

        paginatedPatientSurgeryList
                .setTotalNoOfPages(getTotalNoOfPages(selectedRecordCount, size));
        paginatedPatientSurgeryList.setTotalNoOfRecords(Long.valueOf(selectedRecordCount));
        paginatedPatientSurgeryList.setEntities(patientSurgeryList);

        return paginatedPatientSurgeryList;
    }

    @Transactional
    @Override
    public Boolean removePatientSurgery(Long patientSurgeryId) {

        validatePatientSurgeryId(patientSurgeryId);

        final Query query = entityManager.createNativeQuery("UPDATE T_TR_PATIENT_SURGERY SET PTSG_STATUS = :statusInActive \n" +
                        "WHERE PTSG_ID = :patientSurgeryId AND PTSG_STATUS = :statusActive AND PTSG_BRANCH_ID IN (:branchIdList)")
                .setParameter("statusInActive", STATUS_INACTIVE.getShortValue())
                .setParameter("patientSurgeryId", patientSurgeryId)
                .setParameter("statusActive", STATUS_ACTIVE.getShortValue())
                .setParameter("branchIdList", captureBranchIds());

        if(query.executeUpdate() > 0 && removeDoctorSurgery(patientSurgeryId))
            return true;

        return false;
    }

    @Override
    public DoctorSurgeryDTO assignDoctorToSurgery(DoctorSurgeryDTO doctorSurgeryDTO) {

        BigInteger insertedRowId = null;

        validateEntity(doctorSurgeryDTO);

        try (Connection connection = dataSource.getConnection()) {

            CallableStatement statement = connection
                    .prepareCall("{CALL INSERT INTO T_TR_DOCTOR_SURGERY(DCSG_PATIENT_SURGERY_ID, DCSGP_DOCTOR_CODE, DCSG_REMARKS,\n" +
                            "DCSG_STATUS, CREATED_DATE, CREATED_USER_CODE, DCSG_BRANCH_ID)\n" +
                            "VALUES(?, ?, ?, ?, ?, ?, ?) RETURNING DCSG_ID INTO ?}");

            statement.setLong(1, doctorSurgeryDTO.getPatientSurgeryId());
            statement.setString(2, doctorSurgeryDTO.getDoctorCode());
            statement.setString(3, doctorSurgeryDTO.getRemarks());
            statement.setShort(4, STATUS_ACTIVE.getShortValue());
            statement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(6, auditorAware.getCurrentAuditor().get());
            statement.setLong(7, captureBranchIds().get(0));

            statement.registerOutParameter( 8, Types.BIGINT);

            int updateCount = statement.executeUpdate();

            if (updateCount > 0)
                insertedRowId = BigInteger.valueOf(statement.getLong(8));

        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error while persisting Doctor Surgery : " + e.getMessage());
            throw new OperationException("Error while persisting Doctor Surgery");
        }

        return getDoctorSurgeryById(insertedRowId.longValue());
    }

    @Override
    public DoctorSurgeryDTO getDoctorSurgeryById(Long doctorSurgeryId) {

        DoctorSurgeryDTO doctorSurgeryDTO = null;

        validateDoctorSurgeryId(doctorSurgeryId);

        final String queryString = "SELECT DCSG_ID, DCSG_PATIENT_SURGERY_ID, DCSGP_DOCTOR_CODE, DCSG_REMARKS,\n" +
                "DCSG_STATUS, CREATED_DATE, CREATED_USER_CODE, LAST_MOD_DATE, LAST_MOD_USER_CODE, DCSG_BRANCH_ID\n" +
                "FROM T_TR_DOCTOR_SURGERY\n" +
                "WHERE DCSG_ID = :doctorSurgeryId AND DCSG_STATUS = :status AND DCSG_BRANCH_ID IN (:branchIdList)";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("doctorSurgeryId", doctorSurgeryId);

        List<Map<String,Object>> result = extractResultSet(query);

        if (result.size() == 0)
            return null;

        for (Map<String,Object> doctorSurgery : result) {

            doctorSurgeryDTO = new DoctorSurgeryDTO();

            createDTO(doctorSurgeryDTO, doctorSurgery);
        }

        return doctorSurgeryDTO;
    }

    @Override
    public List<DoctorSurgeryDTO> getDoctorsByPatientSurgeryId(Long patientSurgeryId) {

        List<DoctorSurgeryDTO> doctorSurgeryDTOList = new ArrayList<>();

        validatePatientSurgeryId(patientSurgeryId);

        final String queryString = "SELECT DCSG_ID, DCSG_PATIENT_SURGERY_ID, DCSGP_DOCTOR_CODE, DCSG_REMARKS,\n" +
                "DCSG_STATUS, CREATED_DATE, CREATED_USER_CODE, LAST_MOD_DATE, LAST_MOD_USER_CODE, DCSG_BRANCH_ID\n" +
                "FROM T_TR_DOCTOR_SURGERY\n" +
                "WHERE DCSG_PATIENT_SURGERY_ID = :patientSurgeryId AND DCSG_STATUS = :status AND DCSG_BRANCH_ID IN (:branchIdList)";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("patientSurgeryId", patientSurgeryId);

        List<Map<String,Object>> result = extractResultSet(query);

        if (result.size() == 0)
            return null;

        for (Map<String,Object> doctorSurgery : result) {

            DoctorSurgeryDTO doctorSurgeryDTO = new DoctorSurgeryDTO();

            createDTO(doctorSurgeryDTO, doctorSurgery);

            doctorSurgeryDTOList.add(doctorSurgeryDTO);
        }

        return doctorSurgeryDTOList;
    }

    @Override
    public Boolean removeDoctorSurgery(Long patientSurgeryId) {

        validatePatientSurgeryId(patientSurgeryId);

        final Query query = entityManager.createNativeQuery("UPDATE T_TR_PATIENT_SURGERY SET PTSG_STATUS = :statusInActive \n" +
                        "WHERE PTSG_ID = :patientSurgeryId AND PTSG_STATUS = :statusActive AND PTSG_BRANCH_ID IN (:branchIdList)")
                .setParameter("statusInActive", STATUS_INACTIVE.getShortValue())
                .setParameter("patientSurgeryId", patientSurgeryId)
                .setParameter("statusActive", STATUS_ACTIVE.getShortValue())
                .setParameter("branchIdList", captureBranchIds());

        return query.executeUpdate() == 0 ? false : true;
    }

    private void validatePatientSurgeryId(Long patientSurgeryId) {
        if(patientSurgeryId == null)
            throw new NoRequiredInfoException("Patient Surgery Id is Required");
    }

    private void validateDoctorSurgeryId(Long doctorSurgeryId) {
        if(doctorSurgeryId == null)
            throw new NoRequiredInfoException("Doctor Surgery Id is Required");
    }

    private void createDTO(PatientSurgeryDTO patientSurgeryDTO, Map<String,Object> patientSurgery) {

        patientSurgeryDTO.setPatientSurgeryId(extractLongValue(String.valueOf(patientSurgery.get("PTSG_ID"))));
        patientSurgeryDTO.setPatientCode(extractValue(String.valueOf(patientSurgery.get("PTSG_PATIENT_CODE"))));
        patientSurgeryDTO.setPatientName(extractValue(String.valueOf(patientSurgery.get("PATIENT_NAME"))));
        patientSurgeryDTO.setSurgeryId(extractLongValue(String.valueOf(patientSurgery.get("PTSG_SURGERY_ID"))));
        patientSurgeryDTO.setSurgeryName(extractValue(String.valueOf(patientSurgery.get("SURGERY_NAME"))));
        patientSurgeryDTO.setAdmissionId(extractLongValue(String.valueOf(patientSurgery.get("PTSG_ADMISSION_ID"))));
        patientSurgeryDTO.setOperationRoomId(extractLongValue(String.valueOf(patientSurgery.get("PTSG_ROOM_ID"))));
        patientSurgeryDTO.setStartedDateTime(extractDateTime(String.valueOf(patientSurgery.get("PTSG_STARTED_DATE_TIME"))));
        patientSurgeryDTO.setEndedDateTime(extractDateTime(String.valueOf(patientSurgery.get("PTSG_ENDED_DATE_TIME"))));
        patientSurgeryDTO.setStatus(extractShortValue(String.valueOf(patientSurgery.get("PTSG_STATUS"))));
        patientSurgeryDTO.setCreatedDate(extractDateTime(String.valueOf(patientSurgery.get("CREATED_DATE"))));
        patientSurgeryDTO.setCreatedUserCode(extractValue(String.valueOf(patientSurgery.get("CREATED_USER_CODE"))));
        patientSurgeryDTO.setLastUpdatedDate(extractDateTime(String.valueOf(patientSurgery.get("LAST_MOD_DATE"))));
        patientSurgeryDTO.setLastUpdatedUserCode(extractValue(String.valueOf(patientSurgery.get("LAST_MOD_USER_CODE"))));
        patientSurgeryDTO.setBranchId(extractLongValue(String.valueOf(patientSurgery.get("PTSG_BRANCH_ID"))));
        patientSurgeryDTO.setBranchName(extractValue(String.valueOf(patientSurgery.get("BRANCH_NAME"))));
    }

    private void createDTO(DoctorSurgeryDTO doctorSurgeryDTO, Map<String,Object> doctorSurgery) {

        doctorSurgeryDTO.setDoctorSurgeryId(extractLongValue(String.valueOf(doctorSurgery.get("DCSG_ID"))));
        doctorSurgeryDTO.setPatientSurgeryId(extractLongValue(String.valueOf(doctorSurgery.get("DCSG_PATIENT_SURGERY_ID"))));
        doctorSurgeryDTO.setDoctorCode(extractValue(String.valueOf(doctorSurgery.get("DCSGP_DOCTOR_CODE"))));
        doctorSurgeryDTO.setRemarks(extractValue(String.valueOf(doctorSurgery.get("DCSG_REMARKS"))));
        doctorSurgeryDTO.setStatus(extractShortValue(String.valueOf(doctorSurgery.get("DCSG_STATUS"))));
        doctorSurgeryDTO.setCreatedDate(extractDateTime(String.valueOf(doctorSurgery.get("CREATED_DATE"))));
        doctorSurgeryDTO.setCreatedUserCode(extractValue(String.valueOf(doctorSurgery.get("CREATED_USER_CODE"))));
        doctorSurgeryDTO.setLastUpdatedDate(extractDateTime(String.valueOf(doctorSurgery.get("LAST_MOD_DATE"))));
        doctorSurgeryDTO.setLastUpdatedUserCode(extractValue(String.valueOf(doctorSurgery.get("LAST_MOD_USER_CODE"))));
        doctorSurgeryDTO.setBranchId(extractLongValue(String.valueOf(doctorSurgery.get("DCSG_BRANCH_ID"))));
    }
}

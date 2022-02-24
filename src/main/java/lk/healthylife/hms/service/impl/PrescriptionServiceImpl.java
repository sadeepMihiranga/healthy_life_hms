package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.config.AuditorAwareImpl;
import lk.healthylife.hms.config.EntityValidator;
import lk.healthylife.hms.dto.FacilityDTO;
import lk.healthylife.hms.dto.PaginatedEntity;
import lk.healthylife.hms.dto.PrescriptionDTO;
import lk.healthylife.hms.dto.PrescriptionMedicineDTO;
import lk.healthylife.hms.exception.NoRequiredInfoException;
import lk.healthylife.hms.exception.OperationException;
import lk.healthylife.hms.service.PrescriptionMedicineService;
import lk.healthylife.hms.service.PrescriptionService;
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
public class PrescriptionServiceImpl extends EntityValidator implements PrescriptionService {

    private final AuditorAwareImpl auditorAware;
    private final DataSource dataSource;

    private final PrescriptionMedicineService prescriptionMedicineService;

    @PersistenceContext
    private EntityManager entityManager;

    public PrescriptionServiceImpl(AuditorAwareImpl auditorAware,
                                   DataSource dataSource,
                                   PrescriptionMedicineService prescriptionMedicineService) {
        this.auditorAware = auditorAware;
        this.dataSource = dataSource;
        this.prescriptionMedicineService = prescriptionMedicineService;
    }

    @Transactional
    @Override
    public PrescriptionDTO createPrescription(PrescriptionDTO prescriptionDTO) {

        BigInteger insertedRowId = null;

        validateEntity(prescriptionDTO);

        try (Connection connection = dataSource.getConnection()) {

            CallableStatement statement = connection
                    .prepareCall("{CALL INSERT INTO T_TR_PRESCRIPTION (PREC_PATIENT_CODE, PREC_DOCTOR_CODE, PREC_REMARKS,\n" +
                            "PREC_STATUS, CREATED_DATE, CREATED_USER_CODE, PREC_BRANCH_ID)\n" +
                            "VALUES (?, ?, ?, ?, ?, ?, ?); RETURNING PREC_ID INTO ?}");

            statement.setString(1, prescriptionDTO.getPatientCode());
            statement.setString(2, prescriptionDTO.getDoctorCode());
            statement.setString(3, prescriptionDTO.getRemarks());
            statement.setShort(4, STATUS_ACTIVE.getShortValue());
            statement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(6, auditorAware.getCurrentAuditor().get());
            statement.setLong(7, captureBranchIds().get(0));

            statement.registerOutParameter( 8, Types.BIGINT);

            int updateCount = statement.executeUpdate();

            if (updateCount > 0)
                insertedRowId = BigInteger.valueOf(statement.getLong(8));

        } catch (Exception e) {
            log.error("Error while persisting Prescription : " + e.getMessage());
            throw new OperationException("Error while persisting Prescription");
        }

        for(PrescriptionMedicineDTO prescriptionMedicineDTO : prescriptionDTO.getPrescriptionMedicines()) {
            prescriptionMedicineDTO.setPrescriptionId(insertedRowId.longValue());
            prescriptionMedicineService.createPrescriptionMedicine(prescriptionMedicineDTO);
        }

        return getPrescriptionById(insertedRowId.longValue());
    }

    @Override
    public PrescriptionDTO getPrescriptionById(Long prescriptionId) {

        PrescriptionDTO prescriptionDTO = null;

        validatePrescriptionId(prescriptionId);

        final String queryString = "SELECT p.PREC_ID, p.PREC_PATIENT_CODE, p.PREC_DOCTOR_CODE, p.PREC_REMARKS, p.PREC_STATUS, \n" +
                "p.CREATED_DATE, p.LAST_MOD_DATE, p.CREATED_USER_CODE, p.LAST_MOD_USER_CODE, br.BRNH_NAME AS BRANCH_NAME,\n" +
                "patient.PRTY_NAME AS PATIENT_NAME, doc.PRTY_NAME AS DOCTOR_NAME, patient.PRTY_NIC AS PATIENT_NIC\n" +
                "FROM T_TR_PRESCRIPTION p\n" +
                "INNER JOIN T_RF_BRANCH br ON p.PREC_BRANCH_ID = br.BRNH_ID\n" +
                "INNER JOIN T_MS_PARTY patient ON p.PREC_PATIENT_CODE = patient.PRTY_CODE\n" +
                "INNER JOIN T_MS_PARTY doc ON p.PREC_DOCTOR_CODE = doc.PRTY_CODE\n" +
                "WHERE p.PREC_ID = :prescriptionId AND p.PREC_STATUS = :status AND p.PREC_BRANCH_ID IN (:branchIdList)";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("prescriptionId", prescriptionId);

        List<Map<String,Object>> result = extractResultSet(query);

        if (result.size() == 0)
            return null;

        for (Map<String,Object> prescription : result) {

            prescriptionDTO = new PrescriptionDTO();

            createDTO(prescriptionDTO, prescription);

            prescriptionDTO.setPrescriptionMedicines(prescriptionMedicineService.getPrescriptionMedicineListByPrescriptionId(prescriptionId));
        }

        return prescriptionDTO;
    }

    @Override
    public PaginatedEntity prescriptionPaginatedSearch(String doctorCode, String patientName, String patientNic,
                                                       Integer page, Integer size) {

        PaginatedEntity paginatedPrescriptionList = null;
        List<PrescriptionDTO> prescriptionList = null;

        validatePaginateIndexes(page, size);
        page = page == 1 ? 0 : page;

        final String countQueryString = "SELECT COUNT(p.PREC_ID)\n" +
                "FROM T_TR_PRESCRIPTION p\n" +
                "INNER JOIN T_MS_PARTY patient ON p.PREC_PATIENT_CODE = patient.PRTY_CODE\n" +
                "WHERE p.PREC_STATUS = :status AND p.PREC_BRANCH_ID IN (:branchIdList)\n" +
                "AND (upper(patient.PRTY_NAME) LIKE ('%'||upper(:patientName)||'%'))\n" +
                "AND (:patientNic IS NULL OR (:patientNic IS NOT NULL) AND patient.PRTY_NIC = :patientNic)\n" +
                "AND (upper(p.PREC_DOCTOR_CODE) LIKE ('%'||upper(:doctorCode)||'%'))";

        Query query = entityManager.createNativeQuery(countQueryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("doctorCode", doctorCode);
        query.setParameter("patientName", patientName);
        query.setParameter("patientNic", patientNic);
        query.setParameter("branchIdList", captureBranchIds());

        final int selectedRecordCount = ((Number) query.getSingleResult()).intValue();

        query = null;

        final String queryString = "SELECT p.PREC_ID, p.PREC_PATIENT_CODE, p.PREC_DOCTOR_CODE, p.PREC_REMARKS, p.PREC_STATUS,\n" +
                "p.CREATED_DATE, p.LAST_MOD_DATE, p.CREATED_USER_CODE, p.LAST_MOD_USER_CODE, br.BRNH_NAME AS BRANCH_NAME,\n" +
                "patient.PRTY_NAME AS PATIENT_NAME, doc.PRTY_NAME AS DOCTOR_NAME, patient.PRTY_NIC AS PATIENT_NIC\n" +
                "FROM T_TR_PRESCRIPTION p\n" +
                "INNER JOIN T_RF_BRANCH br ON p.PREC_BRANCH_ID = br.BRNH_ID\n" +
                "INNER JOIN T_MS_PARTY patient ON p.PREC_PATIENT_CODE = patient.PRTY_CODE\n" +
                "INNER JOIN T_MS_PARTY doc ON p.PREC_DOCTOR_CODE = doc.PRTY_CODE\n" +
                "WHERE p.PREC_STATUS = :status AND p.PREC_BRANCH_ID IN (:branchIdList)\n" +
                "AND (upper(patient.PRTY_NAME) LIKE ('%'||upper(:patientName)||'%'))\n" +
                "AND (:patientNic IS NULL OR (:patientNic IS NOT NULL) AND patient.PRTY_NIC = :patientNic)\n" +
                "AND (upper(p.PREC_DOCTOR_CODE) LIKE ('%'||upper(:doctorCode)||'%'))\n" +
                "ORDER BY p.CREATED_DATE OFFSET :page ROWS FETCH NEXT :size ROWS ONLY";

        query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("doctorCode", doctorCode);
        query.setParameter("patientName", patientName);
        query.setParameter("patientNic", patientNic);
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("size", size);
        query.setParameter("page", size * page);

        List<Map<String,Object>> result = extractResultSet(query);

        paginatedPrescriptionList = new PaginatedEntity();
        prescriptionList = new ArrayList<>();

        for (Map<String,Object> prescription : result) {

            PrescriptionDTO prescriptionDTO = new PrescriptionDTO();

            createDTO(prescriptionDTO, prescription);

            prescriptionList.add(prescriptionDTO);
        }

        paginatedPrescriptionList
                .setTotalNoOfPages(selectedRecordCount == 0 ? 0 : selectedRecordCount < size ? 1 : selectedRecordCount / size);
        paginatedPrescriptionList.setTotalNoOfRecords(Long.valueOf(selectedRecordCount));
        paginatedPrescriptionList.setEntities(prescriptionList);

        return paginatedPrescriptionList;
    }

    @Override
    public Boolean removePrescription(Long prescriptionId) {

        validatePrescriptionId(prescriptionId);

        final Query query = entityManager.createNativeQuery("UPDATE T_TR_PRESCRIPTION SET PREC_STATUS = :statusInActive\n" +
                        "WHERE PREC_ID = :prescriptionId AND PREC_STATUS = :statusActive AND PREC_BRANCH_ID IN (:branchIdList)")
                .setParameter("statusInActive", STATUS_INACTIVE.getShortValue())
                .setParameter("prescriptionId", prescriptionId)
                .setParameter("statusActive", STATUS_ACTIVE.getShortValue())
                .setParameter("branchIdList", captureBranchIds());

        return query.executeUpdate() == 0 ? false : true;
    }

    private void validatePrescriptionId(Long prescriptionId) {
        if(prescriptionId == null)
            throw new NoRequiredInfoException("Prescription Id is Required");
    }

    private void createDTO(PrescriptionDTO prescriptionDTO, Map<String,Object> prescription) {

        prescriptionDTO.setPrescriptionId(extractLongValue(String.valueOf(prescription.get("PREC_ID"))));
        prescriptionDTO.setPatientCode(extractValue(String.valueOf(prescription.get("PREC_PATIENT_CODE"))));
        prescriptionDTO.setPatientName(extractValue(String.valueOf(prescription.get("PATIENT_NAME"))));
        prescriptionDTO.setPatientNic(extractValue(String.valueOf(prescription.get("PATIENT_NIC"))));
        prescriptionDTO.setDoctorCode(extractValue(String.valueOf(prescription.get("PREC_DOCTOR_CODE"))));
        prescriptionDTO.setDoctorName(extractValue(String.valueOf(prescription.get("DOCTOR_NAME"))));
        prescriptionDTO.setRemarks(extractValue(String.valueOf(prescription.get("PREC_REMARKS"))));
        prescriptionDTO.setStatus(extractShortValue(String.valueOf(prescription.get("PREC_STATUS"))));
        prescriptionDTO.setCreatedDate(extractDateTime(String.valueOf(prescription.get("CREATED_DATE"))));
        prescriptionDTO.setCreatedUserCode(extractValue(String.valueOf(prescription.get("CREATED_USER_CODE"))));
        prescriptionDTO.setLastUpdatedDate(extractDateTime(String.valueOf(prescription.get("LAST_MOD_DATE"))));
        prescriptionDTO.setLastUpdatedUserCode(extractValue(String.valueOf(prescription.get("LAST_MOD_USER_CODE"))));
        prescriptionDTO.setBranchId(extractLongValue(String.valueOf(prescription.get("PREC_BRANCH_ID"))));
        prescriptionDTO.setBranchName(extractValue(String.valueOf(prescription.get("BRANCH_NAME"))));
    }
}

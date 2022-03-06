package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.config.AuditorAwareImpl;
import lk.healthylife.hms.config.EntityValidator;
import lk.healthylife.hms.dto.PaginatedEntity;
import lk.healthylife.hms.dto.PaymentDTO;
import lk.healthylife.hms.exception.NoRequiredInfoException;
import lk.healthylife.hms.exception.OperationException;
import lk.healthylife.hms.service.PaymentService;
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
public class PaymentServiceImpl extends EntityValidator implements PaymentService {

    private final AuditorAwareImpl auditorAware;
    private final DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    public PaymentServiceImpl(AuditorAwareImpl auditorAware,
                              DataSource dataSource) {
        this.auditorAware = auditorAware;
        this.dataSource = dataSource;
    }

    @Override
    public PaymentDTO createPayment(PaymentDTO paymentDTO) {

        BigInteger insertedRowId = null;

        validateEntity(paymentDTO);

        try (Connection connection = dataSource.getConnection()) {

            CallableStatement statement = connection
                    .prepareCall("{CALL INSERT INTO T_MS_PAYMENT(PAYT_PATIENT_CODE, PAYT_TYPE, PAYT_AMOUNT, PAYT_STATUS, CREATED_DATE,\n" +
                            "CREATED_USER_CODE, PAYT_BRANCH_ID, PAYT_DESCRIPTION, PAYT_ADMISSION_ID,\n" +
                            "PAYT_PRESCRIPTION_ID, PAYT_PATIENT_MEDICAL_TEST_ID)\n" +
                            "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING PAYT_ID INTO ?}");

            statement.setString(1, paymentDTO.getPatientCode());
            statement.setString(2, paymentDTO.getType());
            statement.setBigDecimal(3, paymentDTO.getAmount());
            statement.setShort(4, STATUS_ACTIVE.getShortValue());
            statement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(6, auditorAware.getCurrentAuditor().get());
            statement.setLong(7, captureBranchIds().get(0));
            statement.setString(8, paymentDTO.getDescription());
            if(paymentDTO.getAdmissionId() != null)
                statement.setLong(9, paymentDTO.getAdmissionId());
            else
                statement.setNull(9, Types.BIGINT);

            if(paymentDTO.getPrescriptionId() != null)
                statement.setLong(10, paymentDTO.getPrescriptionId());
            else
                statement.setNull(10, Types.BIGINT);

            if(paymentDTO.getPatientMedicalTestId() != null)
                statement.setLong(11, paymentDTO.getPatientMedicalTestId());
            else
                statement.setNull(11, Types.BIGINT);

            statement.registerOutParameter( 12, Types.BIGINT);

            int updateCount = statement.executeUpdate();

            if (updateCount > 0)
                insertedRowId = BigInteger.valueOf(statement.getLong(12));

        } catch (Exception e) {
            log.error("Error while persisting Payment : " + e.getMessage());
            throw new OperationException("Error while persisting Payment");
        }

        return getPaymentById(insertedRowId.longValue());
    }

    @Override
    public PaymentDTO updatePayment(PaymentDTO paymentDTO) {
        return null;
    }

    @Transactional
    @Override
    public Boolean removePayment(Long paymentId, PaymentDTO paymentDTO) {
        validatePaymentId(paymentId);

        final Query query = entityManager
                .createNativeQuery("UPDATE T_MS_PAYMENT SET PAYT_STATUS = :statusInActive, PAYT_CANCEL_REASON = :canceledReason\n" +
                        "WHERE PAYT_ID = :surgeryId AND PAYT_STATUS = :statusActive")
                .setParameter("statusInActive", STATUS_INACTIVE.getShortValue())
                .setParameter("surgeryId", paymentId)
                .setParameter("canceledReason", paymentDTO.getCanceledReason())
                .setParameter("statusActive", STATUS_ACTIVE.getShortValue());

        return query.executeUpdate() == 0 ? false : true;
    }

    @Override
    public PaymentDTO getPaymentById(Long paymentId) {
        PaymentDTO paymentDTO = null;

        validatePaymentId(paymentId);

        final String queryString = "SElECT PAYT_ID, PAYT_PATIENT_CODE, PAYT_TYPE, PAYT_AMOUNT, PAYT_STATUS, CREATED_DATE, CREATED_USER_CODE,\n" +
                "PAYT_BRANCH_ID, PAYT_DESCRIPTION, PAYT_ADMISSION_ID, PAYT_PRESCRIPTION_ID, PAYT_PATIENT_MEDICAL_TEST_ID, PAYT_CANCEL_REASON\n" +
                "LAST_MOD_DATE, LAST_MOD_USER_CODE, type.CMRF_DESCRIPTION AS PAYMENT_TYPE_NAME\n" +
                "FROM T_MS_PAYMENT p\n" +
                "INNER JOIN T_RF_COMMON_REFERENCE type ON p.PAYT_TYPE = type.CMRF_CODE\n" +
                "WHERE p.PAYT_ID = :paymentId AND p.PAYT_STATUS = :status AND p.PAYT_BRANCH_ID IN (:branchIdList)";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("paymentId", paymentId);

        List<Map<String,Object>> result = extractResultSet(query);

        if (result.size() == 0)
            return null;

        for (Map<String,Object> payment : result) {

            paymentDTO = new PaymentDTO();
            createDTO(paymentDTO, payment);
        }

        return paymentDTO;
    }

    @Override
    public PaginatedEntity patientSurgeryPaginatedSearch(String patientCode, Integer page, Integer size) {

        PaginatedEntity paginatedPaymentList = null;
        List<PaymentDTO> paymentList = null;

        page = validatePaginateIndexes(page, size);

        final String countQueryString = "SElECT COUNT(PAYT_ID)\n" +
                "FROM T_MS_PAYMENT\n" +
                "WHERE PAYT_STATUS = :status AND PAYT_BRANCH_ID IN (:branchIdList)\n" +
                "AND (:patientCode IS NULL OR (:patientCode IS NOT NULL) AND PAYT_PATIENT_CODE = :patientCode)\n";

        Query query = entityManager.createNativeQuery(countQueryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("patientCode", patientCode);
        query.setParameter("branchIdList", captureBranchIds());

        final int selectedRecordCount = ((Number) query.getSingleResult()).intValue();

        query = null;

        final String queryString = "SElECT p.PAYT_ID, p.PAYT_PATIENT_CODE, p.PAYT_TYPE, p.PAYT_AMOUNT, p.PAYT_STATUS, p.CREATED_DATE,\n" +
                "p.CREATED_USER_CODE, p.PAYT_BRANCH_ID, p.PAYT_DESCRIPTION, p.PAYT_ADMISSION_ID, p.PAYT_PRESCRIPTION_ID, p.PAYT_CANCEL_REASON,\n" +
                "p.PAYT_PATIENT_MEDICAL_TEST_ID, p.LAST_MOD_DATE, p.LAST_MOD_USER_CODE, type.CMRF_DESCRIPTION AS PAYMENT_TYPE_NAME, patient.PRTY_NAME\n" +
                "FROM T_MS_PAYMENT p\n" +
                "INNER JOIN T_RF_COMMON_REFERENCE type ON p.PAYT_TYPE = type.CMRF_CODE\n" +
                "INNER JOIN T_MS_PARTY patient ON p.PAYT_PATIENT_CODE = patient.PRTY_CODE\n" +
                "WHERE p.PAYT_STATUS = :status AND p.PAYT_BRANCH_ID IN (:branchIdList)\n" +
                "AND (:patientCode IS NULL OR (:patientCode IS NOT NULL) AND p.PAYT_PATIENT_CODE = :patientCode)\n" +
                "ORDER BY p.CREATED_DATE OFFSET :page ROWS FETCH NEXT :size ROWS ONLY";

        query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("patientCode", patientCode);
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("size", size);
        query.setParameter("page", size * page);

        List<Map<String,Object>> result = extractResultSet(query);

        paginatedPaymentList = new PaginatedEntity();
        paymentList = new ArrayList<>();

        for (Map<String,Object> payment : result) {

            PaymentDTO paymentDTO = new PaymentDTO();
            createDTO(paymentDTO, payment);
            paymentList.add(paymentDTO);
        }

        paginatedPaymentList
                .setTotalNoOfPages(getTotalNoOfPages(selectedRecordCount, size));
        paginatedPaymentList.setTotalNoOfRecords(Long.valueOf(selectedRecordCount));
        paginatedPaymentList.setEntities(paymentList);

        return paginatedPaymentList;
    }

    private void validatePaymentId(Long paymentId) {
        if(paymentId == null)
            throw new NoRequiredInfoException("Payment Id is Required");
    }

    private void createDTO(PaymentDTO paymentDTO, Map<String,Object> payment) {

        paymentDTO.setPaymentId(extractLongValue(String.valueOf(payment.get("PAYT_ID"))));
        paymentDTO.setAdmissionId(extractLongValue(String.valueOf(payment.get("PAYT_ADMISSION_ID"))));
        paymentDTO.setPrescriptionId(extractLongValue(String.valueOf(payment.get("PAYT_PRESCRIPTION_ID"))));
        paymentDTO.setPatientMedicalTestId(extractLongValue(String.valueOf(payment.get("PAYT_PATIENT_MEDICAL_TEST_ID"))));
        paymentDTO.setPatientCode(extractValue(String.valueOf(payment.get("PAYT_PATIENT_CODE"))));
        paymentDTO.setType(extractValue(String.valueOf(payment.get("PAYT_TYPE"))));
        paymentDTO.setTypeName(extractValue(String.valueOf(payment.get("PAYMENT_TYPE_NAME"))));
        paymentDTO.setAmount(extractDecimalValue(String.valueOf(payment.get("PAYT_AMOUNT"))));
        paymentDTO.setDescription(extractValue(String.valueOf(payment.get("PAYT_DESCRIPTION"))));
        paymentDTO.setCanceledReason(extractValue(String.valueOf(payment.get("PAYT_CANCEL_REASON"))));
        paymentDTO.setStatus(extractShortValue(String.valueOf(payment.get("PAYT_STATUS"))));
        paymentDTO.setCreatedDate(extractDateTime(String.valueOf(payment.get("CREATED_DATE"))));
        paymentDTO.setCreatedUserCode(extractValue(String.valueOf(payment.get("CREATED_USER_CODE"))));
        paymentDTO.setLastUpdatedDate(extractDateTime(String.valueOf(payment.get("LAST_MOD_DATE"))));
        paymentDTO.setLastUpdatedUserCode(extractValue(String.valueOf(payment.get("LAST_MOD_USER_CODE"))));
        paymentDTO.setBranchId(extractLongValue(String.valueOf(payment.get("PAYT_BRANCH_ID"))));
        paymentDTO.setBranchName(extractValue(String.valueOf(payment.get("BRANCH_NAME"))));
    }
}

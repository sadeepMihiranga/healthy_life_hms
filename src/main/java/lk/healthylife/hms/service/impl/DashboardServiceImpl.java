package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.config.EntityValidator;
import lk.healthylife.hms.dto.SummeryCountsDTO;
import lk.healthylife.hms.exception.OperationException;
import lk.healthylife.hms.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.Map;

import static lk.healthylife.hms.util.constant.Constants.STATUS_ACTIVE;

@Slf4j
@Service
public class DashboardServiceImpl extends EntityValidator implements DashboardService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public SummeryCountsDTO getSummeryCounts(SummeryCountsDTO summeryCountsDTOTRequest) {

        SummeryCountsDTO summeryCountsDTO = new SummeryCountsDTO();

        summeryCountsDTO.setPatientAdmissionCount(Map.of("label", "Admission Count", "count", executeQuery("Admission").toString()));
        summeryCountsDTO.setPrescriptionCount(Map.of("label", "Prescription Count", "count", executeQuery("Prescription").toString()));
        summeryCountsDTO.setSurgeryCount(Map.of("label", "Surgery Count", "count", executeQuery("Surgery").toString()));
        summeryCountsDTO.setMedicalTestCount(Map.of("label", "Medical Test Count", "count", executeQuery("MedicalTest").toString()));

        return summeryCountsDTO;
    }

    private Integer executeQuery(String queryType) {
        Query query = entityManager.createNativeQuery(getCountQuery(queryType));

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("branchIdList", captureBranchIds());

        return  ((Number) query.getSingleResult()).intValue();
    }

    private String getCountQuery(String type) {
        switch (type) {
            case "Admission" : return "SELECT COUNT(*) FROM T_TR_PATIENT_ADMISSION WHERE PTAD_STATUS = :status AND PTAD_BRANCH_ID IN (:branchIdList)";
            case "Prescription" : return "SELECT COUNT(*) FROM T_TR_PRESCRIPTION WHERE PREC_STATUS = :status AND PREC_BRANCH_ID IN (:branchIdList)";
            case "Surgery" : return "SELECT COUNT(*) FROM T_TR_PATIENT_SURGERY WHERE PTSG_STATUS = :status AND PTSG_BRANCH_ID IN (:branchIdList)";
            case "MedicalTest" : return "SELECT COUNT(*) FROM T_MS_MEDICAL_TEST WHERE MDTS_STATUS = :status AND MDTS_BRANCH_ID IN (:branchIdList)";
            default: throw new OperationException("Error while getting count query");
        }
    }
}

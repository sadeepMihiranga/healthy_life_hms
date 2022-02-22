package lk.healthylife.hms.config.repository;

import lk.healthylife.hms.service.NumberGeneratorService;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.PersistenceContext;
import javax.persistence.StoredProcedureQuery;

@Service
public class NumberGeneratorRepository implements NumberGeneratorService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Call the stored procedure F_CM_GEN_SYS_REF_NUMBER and generate numbers.
     */
    public String generateNumber(String refNumType, String increase, String subTypeRef1, String subTypeRef2, String subTypeRef3,
                                 String subTypeRef4, String year, String month) {
        StoredProcedureQuery storedProcedureQuery = entityManager.createStoredProcedureQuery("F_GEN_SYS_REF_NUMBER");

        storedProcedureQuery.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
        storedProcedureQuery.setParameter(1, refNumType);

        storedProcedureQuery.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
        storedProcedureQuery.setParameter(2, increase);

        storedProcedureQuery.registerStoredProcedureParameter(3, String.class, ParameterMode.IN);
        storedProcedureQuery.setParameter(3, subTypeRef1);

        storedProcedureQuery.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);
        storedProcedureQuery.setParameter(4, subTypeRef2);

        storedProcedureQuery.registerStoredProcedureParameter(5, String.class, ParameterMode.IN);
        storedProcedureQuery.setParameter(5, subTypeRef3);

        storedProcedureQuery.registerStoredProcedureParameter(6, String.class, ParameterMode.IN);
        storedProcedureQuery.setParameter(6, subTypeRef4);

        storedProcedureQuery.registerStoredProcedureParameter(7, String.class, ParameterMode.IN);
        storedProcedureQuery.setParameter(7, year);

        storedProcedureQuery.registerStoredProcedureParameter(8, String.class, ParameterMode.IN);
        storedProcedureQuery.setParameter(8, month);

        storedProcedureQuery.registerStoredProcedureParameter(9, String.class, ParameterMode.OUT);

        return (String) storedProcedureQuery.getOutputParameterValue(9);
    }
}

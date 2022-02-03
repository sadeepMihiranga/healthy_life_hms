package lk.healthylife.hms.repository;

import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.PersistenceContext;
import javax.persistence.StoredProcedureQuery;

@Repository
public class SequenceGeneratorRepository {

	@PersistenceContext
	private EntityManager entityManager;
	
	/**
	 * Call the stored procedure F_CM_GEN_TAB_SEQUENCE and generated table sequences.
	 * @param tableName - database table name
	 * @return - Generated table sequence number
	 */
	public String generateSequenceNo(String tableName) {
		StoredProcedureQuery storedProcedureQuery = entityManager.createStoredProcedureQuery("\"LAKDERANA_BASE\".\"F_CM_GEN_TAB_SEQUENCE\"");
		
		// Set the parameters of the stored procedure.
		String firstParam = "firstParam";
		storedProcedureQuery.registerStoredProcedureParameter(firstParam, String.class, ParameterMode.IN);
		storedProcedureQuery.setParameter(firstParam, tableName);
		
		// Call the stored procedure. 
		String generatedSequenceNumber = (String) storedProcedureQuery.getSingleResult();
		return generatedSequenceNumber;
	}
}

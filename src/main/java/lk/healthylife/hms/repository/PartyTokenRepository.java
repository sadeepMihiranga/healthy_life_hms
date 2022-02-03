package lk.healthylife.hms.repository;

import lk.healthylife.hms.entity.TMsPartyToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartyTokenRepository extends JpaRepository<TMsPartyToken, Long>{

	/**
	 * List all party tokens - named query
	 */
	List<TMsPartyToken> findAll();
	
	/**
	 * List all Party tokens in T_CM_MS_PARTY_TOKEN table for a given party code
	 */
	@Query ("SELECT t FROM TMsPartyToken t " +
			"WHERE t.party.prtyCode = :toknPartyCode")
	List<TMsPartyToken> findAllByPartyCode(@Param("toknPartyCode") String toknPartyCode);
	
	/**
	 * List all Party tokens in T_CM_MS_PARTY_TOKEN table for a given party code and request type
	 */
	@Query ("SELECT t FROM TMsPartyToken t " +
			"WHERE t.party.prtyCode = :toknPartyCode " +
			"AND t.toknRequestType = :toknRequestType")
	List<TMsPartyToken> findAllByPartyCodeRequestType(@Param("toknPartyCode") String toknPartyCode,
															 @Param("toknRequestType") String toknRequestType);
	
	/**
	 * List all Party token information in T_CM_MS_PARTY_TOKEN table for a given party code and token 
	 */
	@Query ("SELECT t FROM TMsPartyToken t " +
			"WHERE t.toknStatus = 'A' " +
			"AND t.party.prtyCode = :toknPartyCode " +
			"AND t.toknToken = :toknToken")
	TMsPartyToken findAllByPartyCodeToken(@Param("toknPartyCode") String toknPartyCode,
												@Param("toknToken") String toknToken);
	
	/**
	 * List all Party token information in T_CM_MS_PARTY_TOKEN table for a given party code and pin 
	 */
	@Query ("SELECT t FROM TMsPartyToken t " +
			"WHERE t.toknStatus = 'A' " +
			"AND t.party.prtyCode = :toknPartyCode " +
			"AND t.toknPinNo = :toknPinNo")
	TMsPartyToken findAllByPartyCodePIN(@Param("toknPartyCode") String toknPartyCode,
											   @Param("toknPinNo") String toknPinNo);
}

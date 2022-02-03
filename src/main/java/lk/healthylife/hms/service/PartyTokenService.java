package lk.healthylife.hms.service;

import lk.healthylife.hms.entity.TMsPartyToken;

import java.util.List;

public interface PartyTokenService {

	List<TMsPartyToken> findAll();
	
	List<TMsPartyToken> findAllByPartyCode(String tokenPartyCode);
	
	void insert(TMsPartyToken tCmMsPartyToken);

	void update(TMsPartyToken tCmMsPartyToken);
	
	List<TMsPartyToken>findAllByPartyCodeRequestType(String tokenPartyCode, String tokenRequestType);
	
	TMsPartyToken findAllByPartyCodeToken(String tokenPartyCode, String tokenString);
	
	TMsPartyToken findAllByPartyCodePIN(String tokenPartyCode, String pinNo);
}

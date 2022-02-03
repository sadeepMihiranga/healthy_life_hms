package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.entity.TMsPartyToken;
import lk.healthylife.hms.repository.PartyTokenRepository;
import lk.healthylife.hms.repository.SequenceGeneratorRepository;
import lk.healthylife.hms.service.PartyTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PartyTokenServiceImpl implements PartyTokenService {
	

	private final PartyTokenRepository partyTokenRepository;
	private final SequenceGeneratorRepository sequenceGeneratorRepository;

	public PartyTokenServiceImpl(PartyTokenRepository partyTokenRepository,
								 SequenceGeneratorRepository sequenceGeneratorRepository) {
		this.partyTokenRepository = partyTokenRepository;
		this.sequenceGeneratorRepository = sequenceGeneratorRepository;
	}

	/**
	 * List all party tokens - named query
	 */
	public List<TMsPartyToken> findAll() {
		return partyTokenRepository.findAll();
	}
	
	/**
	 * Insert party tokens to T_MS_PARTY_TOKEN table
	 */

	public void insert(TMsPartyToken tCmMsPartyToken) {
		String tokenSeqNo = sequenceGeneratorRepository.generateSequenceNo("T_MS_PARTY_TOKEN");
		tCmMsPartyToken.setToknSeqNo(tokenSeqNo);
		partyTokenRepository.save(tCmMsPartyToken);
	}

	public void update(TMsPartyToken tCmMsPartyToken) {
		partyTokenRepository.save(tCmMsPartyToken);
	}

	/**
	 * List all Party tokens in T_MS_PARTY_TOKEN table for a given party code
	 */
	public List<TMsPartyToken> findAllByPartyCode(String tokenPartyCode) {
		return partyTokenRepository.findAllByPartyCode(tokenPartyCode);
	}

	/**
	 * List all Party tokens in T_MS_PARTY_TOKEN table for a given party code and request type
	 */
	public List<TMsPartyToken> findAllByPartyCodeRequestType(String tokenPartyCode, String tokenRequestType) {
		return partyTokenRepository.findAllByPartyCodeRequestType(tokenPartyCode, tokenRequestType);
	}
	
	/**
	 * List all Party token information in T_MS_PARTY_TOKEN table for a given party code and token
	 */
	public TMsPartyToken findAllByPartyCodeToken(String tokenPartyCode, String tokenString) {
		return partyTokenRepository.findAllByPartyCodeToken(tokenPartyCode, tokenString);
	}

	/**
	 * List all Party token information in T_MS_PARTY_TOKEN table for a given party code and PIN
	 */
	public TMsPartyToken findAllByPartyCodePIN(String tokenPartyCode, String pinNo) {
		return partyTokenRepository.findAllByPartyCodePIN(tokenPartyCode, pinNo);
	}
}

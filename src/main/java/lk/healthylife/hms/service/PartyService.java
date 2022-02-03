package lk.healthylife.hms.service;

import lk.healthylife.hms.dto.PaginatedEntity;
import lk.healthylife.hms.dto.PartyDTO;

import javax.transaction.Transactional;
import java.util.List;

public interface PartyService {

    @Transactional
    PartyDTO createParty(PartyDTO partyDTO);

    PartyDTO getPartyByPartyCode(String partyCode);

    @Transactional
    PartyDTO updateParty(String partyCode, PartyDTO partyDTO);

    @Transactional
    Boolean removeParty(String partyCode);

    PaginatedEntity partyPaginatedSearch(String name, String partyType, Integer page, Integer size);

    List<PartyDTO> getPartyListByType(String partyType);
}

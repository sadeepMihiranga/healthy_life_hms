package lk.healthylife.hms.service;

import lk.healthylife.hms.dto.PartyContactDTO;

import java.util.List;

public interface PartyContactService {

    PartyContactDTO insertPartyContact(PartyContactDTO partyContactDTO, Boolean isPartyValidated);

    PartyContactDTO updatePartyContactById(PartyContactDTO partyContactDTO);

    List<PartyContactDTO> getContactsByPartyCode(String partyCode, Boolean isPartyValidated);

    PartyContactDTO getContactsByPartyCodeAndType(String partyCode, String contactType);

    Boolean removePartyContactByPartyCode(String partyCode);
}

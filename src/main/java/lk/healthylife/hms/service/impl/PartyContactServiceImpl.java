package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.config.EntityValidator;
import lk.healthylife.hms.dto.PartyContactDTO;
import lk.healthylife.hms.entity.TMsParty;
import lk.healthylife.hms.entity.TMsPartyContact;
import lk.healthylife.hms.exception.*;
import lk.healthylife.hms.mapper.PartyContactMapper;
import lk.healthylife.hms.config.repository.PartyContactRepository;
import lk.healthylife.hms.config.repository.PartyRepository;
import lk.healthylife.hms.service.CommonReferenceService;
import lk.healthylife.hms.service.PartyContactService;
import lk.healthylife.hms.util.constant.CommonReferenceCodes;
import lk.healthylife.hms.util.constant.Constants;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.hibernate.query.internal.NativeQueryImpl;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static lk.healthylife.hms.util.constant.CommonReferenceCodes.PARTY_CONTACT_EMAIL;
import static lk.healthylife.hms.util.constant.CommonReferenceCodes.PARTY_CONTACT_MOBILE;
import static lk.healthylife.hms.util.constant.CommonReferenceTypeCodes.*;
import static lk.healthylife.hms.util.constant.Constants.*;

@Slf4j
@Service
public class PartyContactServiceImpl extends EntityValidator implements PartyContactService {

    private final PartyContactRepository partyContactRepository;
    private final PartyRepository partyRepository;

    private final CommonReferenceService commonReferenceService;

    @PersistenceContext
    private EntityManager entityManager;

    public PartyContactServiceImpl(PartyContactRepository partyContactRepository,
                                   PartyRepository partyRepository,
                                   CommonReferenceService commonReferenceService) {
        this.partyContactRepository = partyContactRepository;
        this.partyRepository = partyRepository;
        this.commonReferenceService = commonReferenceService;
    }

    @Override
    public PartyContactDTO insertPartyContact(PartyContactDTO partyContactDTO, Boolean isPartyValidated) {

        validateEntity(partyContactDTO);

        if(!isPartyValidated)
            validatePartyCode(partyContactDTO.getPartyCode());

        commonReferenceService
                .getByCmrfCodeAndCmrtCode(PARTY_CONTACT_TYPES.getValue(), partyContactDTO.getContactType());

        final TMsPartyContact alreadyPartyContact = partyContactRepository
                .findAllByParty_PrtyCodeAndPtcnContactTypeAndPtcnStatus(partyContactDTO.getPartyCode(),
                        partyContactDTO.getContactType(), STATUS_ACTIVE.getShortValue());

        if(alreadyPartyContact != null)
            throw new DuplicateRecordException("There is a active Contact Number for the given Type");

        if(partyContactDTO.getContactType().equals(PARTY_CONTACT_MOBILE.getValue()))
            if(partyContactDTO.getContactNumber().length() > 15 || partyContactDTO.getContactNumber().length() < 9)
                throw new InvalidDataException("Invalid contact number");

        if(partyContactDTO.getContactType().equals(PARTY_CONTACT_EMAIL.getValue()))
            if(!partyContactDTO.getContactNumber().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
                throw new InvalidDataException("Invalid email address");

        final Query query = entityManager.createNativeQuery("INSERT INTO \"T_MS_PARTY_CONTACT\" " +
                        "(\"PTCN_CONTACT_TYPE\", \"PTCN_CONTACT_NUMBER\", \"PTCN_STATUS\", \"PTCN_PRTY_CODE\")\n" +
                        "VALUES (?, ?, ?, ?)")
                .setParameter(1, partyContactDTO.getContactType())
                .setParameter(2, partyContactDTO.getContactNumber())
                .setParameter(3, STATUS_ACTIVE.getShortValue())
                .setParameter(4, partyContactDTO.getPartyCode());

        query.executeUpdate();

        return null;
    }

    @Override
    public PartyContactDTO updatePartyContactById(PartyContactDTO partyContactDTO) {

        if(partyContactDTO.getContactId() == null)
            throw new NoRequiredInfoException("Party Contact Id is required");

        final Query query = entityManager.createNativeQuery("UPDATE T_MS_PARTY_CONTACT SET PTCN_CONTACT_TYPE = ?, PTCN_CONTACT_NUMBER = ?\n" +
                        "WHERE PTCN_ID = ? AND PTCN_STATUS = ?")
                .setParameter(1, partyContactDTO.getContactType())
                .setParameter(2, partyContactDTO.getContactNumber())
                .setParameter(3, partyContactDTO.getContactId())
                .setParameter(4, STATUS_ACTIVE.getShortValue());

        final int updatedRows = query.executeUpdate();

        if(updatedRows == 0)
            throw new DataNotFoundException("Contact not found for the Id " + partyContactDTO.getContactId());

        return PartyContactMapper.INSTANCE.entityToDTO(
                partyContactRepository.findByPtcnIdAndPtcnStatus(partyContactDTO.getContactId(), STATUS_ACTIVE.getShortValue()));
    }

    @Override
    public List<PartyContactDTO> getContactsByPartyCode(String partyCode, Boolean isPartyValidated) {

        if(!isPartyValidated)
           validatePartyCode(partyCode);

        String queryString = "SELECT PTCN_ID, PTCN_CONTACT_TYPE, PTCN_CONTACT_NUMBER, PTCN_STATUS, PTCN_PRTY_CODE\n" +
                "FROM T_MS_PARTY_CONTACT\n" +
                "WHERE PTCN_PRTY_CODE = :partyCode AND PTCN_STATUS = :status";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("partyCode", partyCode);

        NativeQueryImpl nativeQuery = (NativeQueryImpl) query;
        nativeQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
        List<Map<String,Object>> result = nativeQuery.getResultList();

        if(result.size() == 0)
            return Collections.emptyList();

        List<PartyContactDTO> contactDTOList = new ArrayList<>();

        for (Map<String,Object> party : result) {

            PartyContactDTO partyContactDTO = new PartyContactDTO();

            partyContactDTO.setContactId(extractLongValue(String.valueOf(party.get("PTCN_ID"))));
            partyContactDTO.setPartyCode(extractValue(String.valueOf(party.get("PTCN_PRTY_CODE"))));
            partyContactDTO.setContactType(extractValue(String.valueOf(party.get("PTCN_CONTACT_TYPE"))));
            partyContactDTO.setContactNumber(extractValue(String.valueOf(party.get("PTCN_CONTACT_NUMBER"))));
            partyContactDTO.setStatus(extractShortValue(String.valueOf(party.get("PTCN_STATUS"))));

            contactDTOList.add(partyContactDTO);
        }

        return contactDTOList;
    }

    @Override
    public PartyContactDTO getContactsByPartyCodeAndType(String partyCode, String contactType) {

        if(Strings.isNullOrEmpty(contactType))
            throw new NoRequiredInfoException("Contact Type Code is required");

        validatePartyCode(partyCode);

        // TODO : this should be getting active type of contact for the given type
        final TMsPartyContact tMsPartyContact = partyContactRepository
                .findAllByParty_PrtyCodeAndPtcnContactTypeAndPtcnStatus(partyCode, contactType, STATUS_ACTIVE.getShortValue());

        if(tMsPartyContact == null)
            throw new DataNotFoundException("Contact Not found");

        return PartyContactMapper.INSTANCE.entityToDTO(tMsPartyContact);
    }

    @Override
    public Boolean removePartyContactByPartyCode(String partyCode) {

        validatePartyCode(partyCode);

        final Query query = entityManager.createNativeQuery("UPDATE T_MS_PARTY_CONTACT SET PTCN_STATUS = ? " +
                        "WHERE PTCN_PRTY_CODE = ? AND PTCN_STATUS = ?")
                .setParameter(1, STATUS_INACTIVE.getShortValue())
                .setParameter(2, partyCode)
                .setParameter(3, STATUS_ACTIVE.getShortValue());

        return query.executeUpdate() == 0 ? false : true;
    }

    private TMsParty validatePartyCode(String partyCode) {

        if(Strings.isNullOrEmpty(partyCode))
            throw new NoRequiredInfoException("Party Code is required");

        final TMsParty tMsParty = partyRepository
                .findByPrtyCodeAndPrtyStatus(partyCode, STATUS_ACTIVE.getShortValue());

        if(tMsParty == null)
            throw new DataNotFoundException("Party not found for the Code : " + partyCode);

        return tMsParty;
    }
}

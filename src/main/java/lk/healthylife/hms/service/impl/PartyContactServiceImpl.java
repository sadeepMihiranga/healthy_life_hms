package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.config.EntityValidator;
import lk.healthylife.hms.dto.PartyContactDTO;
import lk.healthylife.hms.entity.TMsParty;
import lk.healthylife.hms.entity.TMsPartyContact;
import lk.healthylife.hms.exception.*;
import lk.healthylife.hms.mapper.PartyContactMapper;
import lk.healthylife.hms.repository.PartyContactRepository;
import lk.healthylife.hms.repository.PartyRepository;
import lk.healthylife.hms.service.CommonReferenceService;
import lk.healthylife.hms.service.PartyContactService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.hibernate.query.internal.NativeQueryImpl;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

        final TMsPartyContact tMsPartyContact = partyContactRepository
                .findByPtcnIdAndPtcnStatus(partyContactDTO.getContactId(), STATUS_ACTIVE.getShortValue());

        if(tMsPartyContact == null)
            throw new DataNotFoundException("Contact not found for the Id " + partyContactDTO.getContactId());

        tMsPartyContact.setPtcnContactNumber(partyContactDTO.getContactNumber());

        return PartyContactMapper.INSTANCE.entityToDTO(persistEntity(tMsPartyContact));
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

        final List<TMsPartyContact> tMsPartyContactList = partyContactRepository
                .findAllByParty_PrtyCodeAndPtcnStatus(partyCode, STATUS_ACTIVE.getShortValue());

        if(tMsPartyContactList.isEmpty())
            throw new DataNotFoundException("Party Contacts not found for the Party Code : " + partyCode);

        tMsPartyContactList.forEach(tMsPartyContact -> {
            tMsPartyContact.setPtcnStatus(STATUS_INACTIVE.getShortValue());
            persistEntity(tMsPartyContact);
        });

        return true;
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

    private TMsPartyContact persistEntity(TMsPartyContact tMsPartyContact) {
        try {
            validateEntity(tMsPartyContact);
            return partyContactRepository.save(tMsPartyContact);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new TransactionConflictException("Transaction Updated by Another User.");
        } catch (Exception e) {
            log.error("Error while persisting : " + e.getMessage());
            throw new OperationException(e.getMessage());
        }
    }
}

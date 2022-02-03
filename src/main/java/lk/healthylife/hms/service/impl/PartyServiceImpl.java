package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.config.EntityValidator;
import lk.healthylife.hms.dto.CommonReferenceDTO;
import lk.healthylife.hms.dto.PaginatedEntity;
import lk.healthylife.hms.dto.PartyContactDTO;
import lk.healthylife.hms.dto.PartyDTO;
import lk.healthylife.hms.entity.TMsDepartment;
import lk.healthylife.hms.entity.TMsParty;
import lk.healthylife.hms.entity.TRfBranch;
import lk.healthylife.hms.exception.DataNotFoundException;
import lk.healthylife.hms.exception.InvalidDataException;
import lk.healthylife.hms.exception.OperationException;
import lk.healthylife.hms.exception.TransactionConflictException;
import lk.healthylife.hms.mapper.PartyMapper;
import lk.healthylife.hms.repository.BranchRepository;
import lk.healthylife.hms.repository.DepartmentRepository;
import lk.healthylife.hms.repository.NumberGeneratorRepository;
import lk.healthylife.hms.repository.PartyRepository;
import lk.healthylife.hms.service.CommonReferenceService;
import lk.healthylife.hms.service.PartyContactService;
import lk.healthylife.hms.service.PartyService;
import lk.healthylife.hms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static lk.healthylife.hms.util.constant.CommonReferenceTypeCodes.*;
import static lk.healthylife.hms.util.constant.Constants.*;

@Slf4j
@Service
public class PartyServiceImpl extends EntityValidator implements PartyService {

    private final CommonReferenceService commonReferenceService;
    private final PartyContactService partyContactService;
    private final UserService userService;

    private final PartyRepository partyRepository;
    private final DepartmentRepository departmentRepository;
    private final BranchRepository branchRepository;
    private final NumberGeneratorRepository numberGeneratorRepository;

    public PartyServiceImpl(PartyRepository partyRepository,
                            CommonReferenceService commonReferenceService,
                            PartyContactService partyContactService,
                            UserService userService,
                            DepartmentRepository departmentRepository,
                            BranchRepository branchRepository,
                            NumberGeneratorRepository numberGeneratorRepository) {
        this.partyRepository = partyRepository;
        this.commonReferenceService = commonReferenceService;
        this.partyContactService = partyContactService;
        this.userService = userService;
        this.departmentRepository = departmentRepository;
        this.branchRepository = branchRepository;
        this.numberGeneratorRepository = numberGeneratorRepository;
    }

    @Transactional
    @Override
    public PartyDTO createParty(PartyDTO partyDTO) {

        String partyNumber = null;
        validateEntity(partyDTO);

        partyDTO.setName(partyDTO.getFirstName() + " " + partyDTO.getLastName());
        partyDTO.setBranchId(captureBranchIds().get(0));
        partyDTO.setPassport(Strings.isNullOrEmpty(partyDTO.getPassport()) ? null : partyDTO.getPassport());

        final TMsParty tMsParty = PartyMapper.INSTANCE.dtoToEntity(partyDTO);

        populateAndValidatePartyReferenceDetailsOnPersist(tMsParty, partyDTO);

        try {
            partyNumber = numberGeneratorRepository.generateNumber("CU", "Y", "#", "#",
                    "#", "#", "#", "#");
        } catch (Exception e) {
            log.error("Error while creating a Party Code : " + e.getMessage());
            throw new OperationException("Error while creating a Party Code");
        }

        tMsParty.setPrtyStatus(STATUS_ACTIVE.getShortValue());
        tMsParty.setPrtyCode(partyNumber);

        final TMsParty createdParty = persistEntity(tMsParty);

        if(partyDTO.getContactList() != null) {
            partyDTO.getContactList().forEach(partyContactDTO -> {
                commonReferenceService
                        .getByCmrfCodeAndCmrtCode(PARTY_CONTACT_TYPES.getValue(), partyContactDTO.getContactType());

                partyContactDTO.setPartyCode(createdParty.getPrtyCode());

                partyContactService.insertPartyContact(partyContactDTO, true);
            });
        }

        return PartyMapper.INSTANCE.entityToDTO(createdParty);
    }

    @Transactional
    @Override
    public PartyDTO getPartyByPartyCode(String partyCode) {

        final TMsParty tMsParty = validateByPartyCode(partyCode);

        PartyDTO partyDTO = PartyMapper.INSTANCE.entityToDTO(tMsParty);

        setReferenceData(tMsParty, partyDTO);

        final List<PartyContactDTO> contactDTOList = partyContactService.getContactsByPartyCode(partyDTO.getPartyCode(), true);
        partyDTO.setContactList(contactDTOList);

        return partyDTO;
    }

    @Transactional
    @Override
    public PartyDTO updateParty(String partyCode, PartyDTO partyDTO) {

        validateEntity(partyDTO);
        partyDTO.setBranchId(captureBranchIds().get(0));

        TMsParty tMsParty = validateByPartyCode(partyCode);

        populateAndValidatePartyReferenceDetailsOnPersist(tMsParty, partyDTO);

        partyDTO.setName(partyDTO.getFirstName() + " " + partyDTO.getLastName());
        tMsParty.setPrtyAddress1(partyDTO.getAddress1());
        tMsParty.setPrtyAddress2(partyDTO.getAddress2());
        tMsParty.setPrtyAddress3(partyDTO.getAddress3());
        tMsParty.setPrtyDob(partyDTO.getDob());
        tMsParty.setPrtyFirstName(partyDTO.getFirstName());
        tMsParty.setPrtyLastName(partyDTO.getLastName());
        tMsParty.setPrtyName(partyDTO.getName());
        tMsParty.setPrtyNic(partyDTO.getNic());
        tMsParty.setPrtyPassport(partyDTO.getPassport());
        tMsParty.setPrtyManagedBy(partyDTO.getManagedBy());
        tMsParty.setPrtyGender(partyDTO.getGender());

        tMsParty.setPrtyStatus(STATUS_ACTIVE.getShortValue());

        partyDTO.getContactList().forEach(partyContactDTO -> {
            partyContactDTO.setPartyCode(partyCode);

            if(partyContactDTO.getContactId() != null)
                partyContactService.updatePartyContactById(partyContactDTO);
            else
                partyContactService.insertPartyContact(partyContactDTO, true);
        });

        return PartyMapper.INSTANCE.entityToDTO(persistEntity(tMsParty));
    }

    private void populateAndValidatePartyReferenceDetailsOnPersist(TMsParty tMsParty, PartyDTO partyDTO) {

        commonReferenceService
                .getByCmrfCodeAndCmrtCode(PARTY_TYPES.getValue(), partyDTO.getType());

        tMsParty.setDepartment(null);
        if(!Strings.isNullOrEmpty(partyDTO.getDepartmentCode())) {
            final TMsDepartment tMsDepartment = departmentRepository
                    .findByDpmtCodeAndDpmtStatus(partyDTO.getDepartmentCode(), STATUS_ACTIVE.getShortValue());

            tMsParty.setDepartment(tMsDepartment);
        }

        tMsParty.setBranch(null);
        if(partyDTO.getBranchId() != null) {
            final TRfBranch tRfBranch = branchRepository
                    .findByBrnhIdAndBrnhStatus(partyDTO.getBranchId(), STATUS_ACTIVE.getShortValue());

            tMsParty.setBranch(tRfBranch);
        }

        commonReferenceService
                .getByCmrfCodeAndCmrtCode(GENDER_TYPES.getValue(), partyDTO.getGender());
    }

    @Transactional
    @Override
    public Boolean removeParty(String partyCode) {

        TMsParty tMsParty = validateByPartyCode(partyCode);

        partyContactService.removePartyContactByPartyCode(partyCode);
        userService.removeUserByPartyCode(partyCode);

        tMsParty.setPrtyStatus(STATUS_INACTIVE.getShortValue());
        persistEntity(tMsParty);

        return true;
    }

    @Override
    public PaginatedEntity partyPaginatedSearch(String name, String partyType, Integer page, Integer size) {

        PaginatedEntity paginatedPartyList = null;
        List<PartyDTO> customerList = null;

        validatePaginateIndexes(page, size);

        partyType = partyType.isEmpty() ? null : partyType;

        Page<TMsParty> tMsPartyPage = partyRepository
                .getActiveParties(name, STATUS_ACTIVE.getShortValue(), partyType, captureBranchIds(),
                        PageRequest.of(page - 1, size));

        if (tMsPartyPage.getSize() == 0)
            return null;

        paginatedPartyList = new PaginatedEntity();
        customerList = new ArrayList<>();

        for (TMsParty tMsParty : tMsPartyPage) {

            PartyDTO partyDTO = PartyMapper.INSTANCE.entityToDTO(tMsParty);

            setReferenceData(tMsParty, partyDTO);

            customerList.add(partyDTO);
        }

        paginatedPartyList.setTotalNoOfPages(tMsPartyPage.getTotalPages());
        paginatedPartyList.setTotalNoOfRecords(tMsPartyPage.getTotalElements());
        paginatedPartyList.setEntities(customerList);

        return paginatedPartyList;
    }

    @Override
    public List<PartyDTO> getPartyListByType(String partyType) {

        final List<TMsParty> tMsPartyList = partyRepository
                .findAllByBranch_BrnhIdInAndPrtyStatusAndPrtyType(captureBranchIds(), STATUS_ACTIVE.getShortValue(), partyType);

        if(tMsPartyList.isEmpty())
            return Collections.emptyList();

        List<PartyDTO> partyDTOList = new ArrayList<>();

        tMsPartyList.forEach(tMsParty -> {

            PartyDTO partyDTO = new PartyDTO();

            partyDTO.setPartyCode(tMsParty.getPrtyCode());
            partyDTO.setName(tMsParty.getPrtyName());

            partyDTOList.add(partyDTO);
        });

        return partyDTOList;
    }

    private void setReferenceData(TMsParty tMsParty, PartyDTO partyDTO) {

        if(tMsParty.getDepartment() != null)
            partyDTO.setDepartmentName(tMsParty.getDepartment().getDpmtName());

        if(!Strings.isNullOrEmpty(partyDTO.getGender())) {
            final CommonReferenceDTO commonReferenceDTO = commonReferenceService
                    .getByCmrfCodeAndCmrtCode(GENDER_TYPES.getValue(), partyDTO.getGender());

            partyDTO.setGenderName(commonReferenceDTO.getDescription());
        }

        partyDTO.setContactList(partyContactService.getContactsByPartyCode(partyDTO.getPartyCode(), true));

        if(!Strings.isNullOrEmpty(tMsParty.getPrtyManagedBy()))
            partyDTO.setManagedByName(partyRepository
                    .findByPrtyCodeAndPrtyStatus(tMsParty.getPrtyManagedBy(), STATUS_ACTIVE.getShortValue()).getPrtyName());
    }

    TMsParty validateByPartyCode(String partyCode) {

        if (Strings.isNullOrEmpty(partyCode))
            throw new InvalidDataException("Party Code is required");

        final TMsParty tMsParty = partyRepository.findByPrtyCodeAndPrtyStatus(partyCode, STATUS_ACTIVE.getShortValue());

        if(tMsParty == null)
            throw new DataNotFoundException("Party not found for the Code : " + partyCode);

        return tMsParty;
    }

    private TMsParty persistEntity(TMsParty tMsParty) {
        try {
            validateEntity(tMsParty);
            return partyRepository.save(tMsParty);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new TransactionConflictException("Transaction Updated by Another User.");
        } catch (Exception e) {
            log.error("Error while persisting : " + e.getMessage());
            throw new OperationException(e.getMessage());
        }
    }
}

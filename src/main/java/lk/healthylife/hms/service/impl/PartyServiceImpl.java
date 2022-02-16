package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.config.AuditorAwareImpl;
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

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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

    private final AuditorAwareImpl auditorAware;

    @PersistenceContext
    private EntityManager entityManager;

    public PartyServiceImpl(PartyRepository partyRepository,
                            CommonReferenceService commonReferenceService,
                            PartyContactService partyContactService,
                            UserService userService,
                            DepartmentRepository departmentRepository,
                            BranchRepository branchRepository,
                            NumberGeneratorRepository numberGeneratorRepository,
                            AuditorAwareImpl auditorAware) {
        this.partyRepository = partyRepository;
        this.commonReferenceService = commonReferenceService;
        this.partyContactService = partyContactService;
        this.userService = userService;
        this.departmentRepository = departmentRepository;
        this.branchRepository = branchRepository;
        this.numberGeneratorRepository = numberGeneratorRepository;
        this.auditorAware = auditorAware;
    }

    @Transactional
    @Override
    public PartyDTO createParty(PartyDTO partyDTO) {

        String partyCode = null;
        validateEntity(partyDTO);

        partyDTO.setName(partyDTO.getFirstName() + " " + partyDTO.getLastName());

        final TMsParty tMsParty = PartyMapper.INSTANCE.dtoToEntity(partyDTO);

        populateAndValidatePartyReferenceDetailsOnPersist(tMsParty, partyDTO);

        try {
            partyCode = numberGeneratorRepository.generateNumber("CU", "Y", "#", "#",
                    "#", "#", "#", "#");
        } catch (Exception e) {
            log.error("Error while creating a Party Code : " + e.getMessage());
            throw new OperationException("Error while creating a Party Code");
        }

        if(Strings.isNullOrEmpty(partyCode))
            throw new OperationException("Party Code not generated");

        try {
            final Query query = entityManager.createNativeQuery("INSERT INTO \"HEALTHYLIFE_BASE\".\"T_MS_PARTY\" " +
                    "(\"PRTY_FIRST_NAME\", \"PRTY_LAST_NAME\", \"PRTY_DOB\", \"PRTY_ADDRESS_1\", \"PRTY_ADDRESS_2\", \"PRTY_ADDRESS_3\", \"PRTY_STATUS\",\n" +
                    "\"PRTY_GENDER\", \"PRTY_TYPE\", \"PRTY_BRANCH_ID\", \"PRTY_DEPARTMENT_CODE\", \"PRTY_NIC\", \"PRTY_MANAGED_BY\", \"PRTY_PASSPORT\", " +
                    "\"PRTY_NAME\", \"CREATED_DATE\", \"CREATED_USER_CODE\", \"PRTY_CODE\", \"PRTY_INITIALS\", " +
                    "\"PRTY_BLOOD_GROUP\", \"PRTY_SPECIALIZATION_CODE\")\n" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", TMsParty.class)
                    .setParameter(1, partyDTO.getFirstName())
                    .setParameter(2, Strings.isNullOrEmpty(partyDTO.getLastName()) ? null : partyDTO.getLastName())
                    .setParameter(3, partyDTO.getDob())
                    .setParameter(4, Strings.isNullOrEmpty(partyDTO.getAddress1()) ? null : partyDTO.getAddress1())
                    .setParameter(5, Strings.isNullOrEmpty(partyDTO.getAddress2()) ? null : partyDTO.getAddress2())
                    .setParameter(6, Strings.isNullOrEmpty(partyDTO.getAddress3()) ? null : partyDTO.getAddress3())
                    .setParameter(7, STATUS_ACTIVE.getShortValue())
                    .setParameter(8, partyDTO.getGender())
                    .setParameter(9, partyDTO.getType())
                    .setParameter(10, captureBranchIds().get(0))
                    .setParameter(11, Strings.isNullOrEmpty(partyDTO.getDepartmentCode()) ? null : partyDTO.getDepartmentCode())
                    .setParameter(12, Strings.isNullOrEmpty(partyDTO.getNic()) ? null : partyDTO.getNic())
                    .setParameter(13, partyDTO.getManagedBy())
                    .setParameter(14, Strings.isNullOrEmpty(partyDTO.getPassport()) ? null : partyDTO.getPassport())
                    .setParameter(15, partyDTO.getName())
                    .setParameter(16, LocalDateTime.now())
                    .setParameter(17, auditorAware.getCurrentAuditor().get())
                    .setParameter(18, partyCode)
                    .setParameter(19, Strings.isNullOrEmpty(partyDTO.getInitials()) ? null : partyDTO.getInitials())
                    .setParameter(20, partyDTO.getBloodGroup())
                    .setParameter(21, Strings.isNullOrEmpty(partyDTO.getSpecialization()) ? null : partyDTO.getSpecialization());

            query.executeUpdate();
        } catch (Exception e) {
            log.error("Error while persisting : " + getExceptionMessageChain(e.getCause()));
            throw new OperationException(getExceptionMessageChain(e.getCause()));
        }

        final TMsParty createdParty = partyRepository.findByPrtyCodeAndPrtyStatus(partyCode, STATUS_ACTIVE.getShortValue());

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

        if(partyDTO.getType() != null)
            commonReferenceService
                    .getByCmrfCodeAndCmrtCode(PARTY_TYPES.getValue(), partyDTO.getType());

        if(partyDTO.getGender() != null)
            commonReferenceService
                    .getByCmrfCodeAndCmrtCode(GENDER_TYPES.getValue(), partyDTO.getGender());

        if(partyDTO.getBloodGroup() != null)
            commonReferenceService
                    .getByCmrfCodeAndCmrtCode(BLOODGROUP_TYPES.getValue(), partyDTO.getBloodGroup());

        if(partyDTO.getSpecialization() != null)
            commonReferenceService
                    .getByCmrfCodeAndCmrtCode(DOC_SPECIALIZATION_TYPES.getValue(), partyDTO.getSpecialization());

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

        /*Page<TMsParty> tMsPartyPage = partyRepository
                .getActiveParties(name, STATUS_ACTIVE.getShortValue(), partyType, captureBranchIds(),
                        PageRequest.of(page - 1, size));*/

        String countQueryString = "SELECT COUNT(*) FROM \"HEALTHYLIFE_BASE\".\"T_MS_PARTY\"\n" +
                "WHERE \"PRTY_STATUS\"=:status\n" +
                "  AND (:partyType IS NULL OR (:partyType IS NOT NULL) AND \"PRTY_TYPE\"=:partyType)\n" +
                "  AND (upper(\"PRTY_NAME\") LIKE ('%'||upper(:name)||'%'))\n" +
                "  AND (\"PRTY_BRANCH_ID\" IN (:branchIdList))";

        Query query = entityManager.createNativeQuery(countQueryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("partyType", partyType);
        query.setParameter("name", name);
        query.setParameter("branchIdList", captureBranchIds());

        int count = ((Number) query.getSingleResult()).intValue();

        String queryString = "SELECT p.\"PRTY_CODE\", p.\"CREATED_DATE\", p.\"CREATED_USER_CODE\",\n" +
                "       p.\"LAST_MOD_DATE\", p.\"LAST_MOD_USER_CODE\", p.\"PRTY_BRANCH_ID\",\n" +
                "       p.\"PRTY_DEPARTMENT_CODE\", p.\"PRTY_ADDRESS_1\", p.\"PRTY_ADDRESS_2\",\n" +
                "       p.\"PRTY_ADDRESS_3\", p.\"PRTY_BLOOD_GROUP\", p.\"PRTY_DOB\",\n" +
                "       p.\"PRTY_FIRST_NAME\", p.\"PRTY_GENDER\", p.\"PRTY_LAST_NAME\",\n" +
                "       p.\"PRTY_MANAGED_BY\", p.\"PRTY_NAME\", p.\"PRTY_NIC\",\n" +
                "       p.\"PRTY_PASSPORT\", p.\"PRTY_SPECIALIZATION_CODE\", p.\"PRTY_STATUS\",\n" +
                "       p.\"PRTY_TYPE\", string_agg(pc.\"PTCN_CONTACT_TYPE\", ',') AS \"CONTACT_TYPES\", string_agg(pc.\"PTCN_CONTACT_NUMBER\", ',') AS \"CONTACT_NOS\",\n" +
                "       split_part(string_agg(gender.\"CMRF_DESCRIPTION\", ','), ',', 1) AS \"PRTY_GENDER_VALUE\",\n" +
                "       split_part(string_agg(type.\"CMRF_DESCRIPTION\", ','), ',', 1) AS \"PRTY_TYPE_VALUE\",\n" +
                "       split_part(string_agg(br.\"BRNH_NAME\", ','), ',', 1) AS \"PRTY_BRANCH_NAME\",\n" +
                "       split_part(string_agg(dspec.\"CMRF_DESCRIPTION\", ','), ',', 1) AS \"PRTY_SPECIALIZATION\"\n" +
                "FROM \"HEALTHYLIFE_BASE\".\"T_MS_PARTY\" p\n" +
                "LEFT JOIN \"HEALTHYLIFE_BASE\".\"T_MS_PARTY_CONTACT\" pc ON p.\"PRTY_CODE\" = pc.\"PTCN_PRTY_CODE\"\n" +
                "LEFT JOIN \"HEALTHYLIFE_BASE\".\"T_RF_COMMON_REFERENCE\" gender ON p.\"PRTY_GENDER\" = gender.\"CMRF_CODE\"\n" +
                "LEFT JOIN \"HEALTHYLIFE_BASE\".\"T_RF_COMMON_REFERENCE\" type ON p.\"PRTY_TYPE\" = type.\"CMRF_CODE\"\n" +
                "LEFT JOIN \"HEALTHYLIFE_BASE\".\"T_RF_BRANCH\" br ON p.\"PRTY_BRANCH_ID\" = br.\"BRNH_ID\"\n" +
                "LEFT JOIN \"HEALTHYLIFE_BASE\".\"T_RF_COMMON_REFERENCE\" dspec ON p.\"PRTY_SPECIALIZATION_CODE\" = dspec.\"CMRF_CODE\"\n" +
                "WHERE p.\"PRTY_STATUS\"=1\n" +
                "  AND p.\"PRTY_TYPE\"='DOCTR'\n" +
                "  AND (? IS NULL OR (? IS NOT NULL) AND p.\"PRTY_TYPE\"=?)\n" +
                "  AND (upper(p.\"PRTY_NAME\") LIKE ('%'||upper(?)||'%'))\n" +
                "  AND (p.\"PRTY_BRANCH_ID\" IN (?))\n" +
                "GROUP BY p.\"PRTY_CODE\", p.\"LAST_MOD_DATE\"\n" +
                "ORDER BY p.\"LAST_MOD_DATE\" OFFSET 0 LIMIT 10;";
        query = entityManager.createNativeQuery(queryString);
        //query.setParameter(1, id);

        List<Object[]> resultList = query.getResultList();

        /*if (tMsPartyPage.getSize() == 0)
            return null;

        paginatedPartyList = new PaginatedEntity();
        customerList = new ArrayList<>();

        for (TMsParty tMsParty : tMsPartyPage) {

            PartyDTO partyDTO = PartyMapper.INSTANCE.entityToDTO(tMsParty);

            setReferenceData(tMsParty, partyDTO);

            customerList.add(partyDTO);
        }*/

        /*paginatedPartyList.setTotalNoOfPages(tMsPartyPage.getTotalPages());
        paginatedPartyList.setTotalNoOfRecords(tMsPartyPage.getTotalElements());*/
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

package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.config.AuditorAwareImpl;
import lk.healthylife.hms.config.EntityValidator;
import lk.healthylife.hms.config.repository.NumberGeneratorRepository;
import lk.healthylife.hms.config.repository.PartyRepository;
import lk.healthylife.hms.dto.PaginatedEntity;
import lk.healthylife.hms.dto.PartyDTO;
import lk.healthylife.hms.dto.UserDTO;
import lk.healthylife.hms.entity.TMsParty;
import lk.healthylife.hms.exception.DataNotFoundException;
import lk.healthylife.hms.exception.InvalidDataException;
import lk.healthylife.hms.exception.OperationException;
import lk.healthylife.hms.mapper.PartyMapper;
import lk.healthylife.hms.service.CommonReferenceService;
import lk.healthylife.hms.service.PartyContactService;
import lk.healthylife.hms.service.PartyService;
import lk.healthylife.hms.service.UserService;
import lk.healthylife.hms.util.DateConversion;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static lk.healthylife.hms.util.constant.CommonReferenceTypeCodes.*;
import static lk.healthylife.hms.util.constant.Constants.STATUS_ACTIVE;
import static lk.healthylife.hms.util.constant.Constants.STATUS_INACTIVE;

@Slf4j
@Service
public class PartyServiceImpl extends EntityValidator implements PartyService {

    private final CommonReferenceService commonReferenceService;
    private final PartyContactService partyContactService;
    private final UserService userService;

    private final PartyRepository partyRepository;
    private final NumberGeneratorRepository numberGeneratorRepository;

    private final AuditorAwareImpl auditorAware;

    @PersistenceContext
    private EntityManager entityManager;

    public PartyServiceImpl(PartyRepository partyRepository,
                            CommonReferenceService commonReferenceService,
                            PartyContactService partyContactService,
                            UserService userService,
                            NumberGeneratorRepository numberGeneratorRepository,
                            AuditorAwareImpl auditorAware) {
        this.partyRepository = partyRepository;
        this.commonReferenceService = commonReferenceService;
        this.partyContactService = partyContactService;
        this.userService = userService;
        this.numberGeneratorRepository = numberGeneratorRepository;
        this.auditorAware = auditorAware;
    }

    @Transactional
    @Override
    public PartyDTO createParty(PartyDTO partyDTO) {

        String partyCode = null;

        validateEntity(partyDTO);
        validatePartyReferenceDetailsOnPersist(partyDTO);

        if(!Strings.isNullOrEmpty(partyDTO.getNic())) {
            if(!partyDTO.getNic().matches("^([0-9]{9}[x|X|v|V]|[0-9]{12})$")) {
                throw new InvalidDataException("NIC number is invalid");
            }
        }

        if(!Strings.isNullOrEmpty(partyDTO.getPassport())) {
            if(!partyDTO.getPassport().matches("^(?!^0+$)[a-zA-Z0-9]{5,20}$")) {
                throw new InvalidDataException("Passport number is invalid");
            }
        }

        try{
            if(!DateConversion.isValidDate(DateConversion.convertLocalDateToString(partyDTO.getDob()), DateConversion.STANDARD_DATE_FORMAT)) {
                throw new InvalidDataException("Date of birth cannot be a future date");
            }
        }catch (ParseException e) {
            e.printStackTrace();
            throw new OperationException("Error while saving patient");
        }

        try {
            partyCode = numberGeneratorRepository.generateNumber("CU", "Y", "#", "#",
                    "#", "#", "#", "#");
        } catch (Exception e) {
            log.error("Error while creating a Party Code : " + e.getMessage());
            throw new OperationException("Error while creating a Party Code");
        }

        if(Strings.isNullOrEmpty(partyCode))
            throw new OperationException("Party Code not generated");

        partyDTO.setName(partyDTO.getFirstName() + " " + partyDTO.getLastName());

        try {
            final Query query = entityManager.createNativeQuery("INSERT INTO \"T_MS_PARTY\" " +
                    "(\"PRTY_FIRST_NAME\", \"PRTY_LAST_NAME\", \"PRTY_DOB\", \"PRTY_ADDRESS_1\", \"PRTY_ADDRESS_2\", \"PRTY_ADDRESS_3\", \"PRTY_STATUS\",\n" +
                    "\"PRTY_GENDER\", \"PRTY_TYPE\", \"PRTY_BRANCH_ID\", \"PRTY_DEPARTMENT_CODE\", \"PRTY_NIC\", \"PRTY_MANAGED_BY\", \"PRTY_PASSPORT\", " +
                    "\"PRTY_NAME\", \"CREATED_DATE\", \"CREATED_USER_CODE\", \"PRTY_CODE\", \"PRTY_INITIALS\", " +
                    "\"PRTY_BLOOD_GROUP\", \"PRTY_SPECIALIZATION_CODE\")\n" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", TMsParty.class)
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

        if(partyDTO.getUser() != null) {
            UserDTO userDTO = partyDTO.getUser();
            userDTO.setPartyCode(createdParty.getPrtyCode());
            userService.createUser(userDTO);
        }

        return PartyMapper.INSTANCE.entityToDTO(createdParty);
    }
    
    @Transactional
    @Override
    public PartyDTO getPartyByPartyCode(String partyCode) {

        validateByPartyCode(partyCode);

        PartyDTO partyDTO = new PartyDTO();

        final String queryString = "SELECT p.\"PRTY_CODE\", p.\"CREATED_DATE\", p.\"CREATED_USER_CODE\",\n" +
                "       p.\"LAST_MOD_DATE\", p.\"LAST_MOD_USER_CODE\", p.\"PRTY_BRANCH_ID\",\n" +
                "       p.\"PRTY_DEPARTMENT_CODE\", p.\"PRTY_ADDRESS_1\", p.\"PRTY_ADDRESS_2\",\n" +
                "       p.\"PRTY_ADDRESS_3\", p.\"PRTY_BLOOD_GROUP\", p.\"PRTY_DOB\",\n" +
                "       p.\"PRTY_FIRST_NAME\", p.\"PRTY_GENDER\", p.\"PRTY_LAST_NAME\",\n" +
                "       p.\"PRTY_MANAGED_BY\", p.\"PRTY_NAME\", p.\"PRTY_NIC\", p.\"PRTY_PASSPORT\",\n" +
                "       p.\"PRTY_SPECIALIZATION_CODE\", p.\"PRTY_STATUS\", p.\"PRTY_TYPE\", p.\"PRTY_INITIALS\",\n"+
                "       listagg(pc.\"PTCN_CONTACT_TYPE\", ',') AS \"CONTACT_TYPES\",\n " +
                "       listagg(pc.\"PTCN_CONTACT_NUMBER\", ',') AS \"CONTACT_NOS\",\n" +
                "       listagg(gender.\"CMRF_DESCRIPTION\", ',') AS \"PRTY_GENDER_VALUE\",\n" +
                "       listagg(type.\"CMRF_DESCRIPTION\", ',') AS \"PRTY_TYPE_VALUE\",\n" +
                "       listagg(br.\"BRNH_NAME\", ',') AS \"PRTY_BRANCH_NAME\",\n" +
                "       listagg(dspec.\"CMRF_DESCRIPTION\", ',') AS \"PRTY_SPECIALIZATION\"\n" +
                "FROM \"T_MS_PARTY\" p\n" +
                "LEFT JOIN \"T_MS_PARTY_CONTACT\" pc ON p.\"PRTY_CODE\" = pc.\"PTCN_PRTY_CODE\"\n" +
                "LEFT JOIN \"T_RF_COMMON_REFERENCE\" gender ON p.\"PRTY_GENDER\" = gender.\"CMRF_CODE\"\n" +
                "LEFT JOIN \"T_RF_COMMON_REFERENCE\" type ON p.\"PRTY_TYPE\" = type.\"CMRF_CODE\"\n" +
                "LEFT JOIN \"T_RF_BRANCH\" br ON p.\"PRTY_BRANCH_ID\" = br.\"BRNH_ID\"\n" +
                "LEFT JOIN \"T_RF_COMMON_REFERENCE\" dspec ON p.\"PRTY_SPECIALIZATION_CODE\" = dspec.\"CMRF_CODE\"\n" +
                "WHERE p.\"PRTY_STATUS\" = :status\n" +
                "AND p.\"PRTY_CODE\" = :partyCode\n" +
                "GROUP BY p.\"PRTY_CODE\", p.\"LAST_MOD_DATE\", p.\"CREATED_DATE\", p.\"CREATED_USER_CODE\", \n" +
                "         p.\"LAST_MOD_USER_CODE\", p.\"PRTY_BRANCH_ID\", p.\"PRTY_DEPARTMENT_CODE\", \n" +
                "         p.\"PRTY_ADDRESS_1\", p.\"PRTY_ADDRESS_2\", p.\"PRTY_ADDRESS_3\", p.\"PRTY_BLOOD_GROUP\", \n" +
                "         p.\"PRTY_DOB\", p.\"PRTY_FIRST_NAME\", p.\"PRTY_GENDER\", p.\"PRTY_LAST_NAME\", \n" +
                "         p.\"PRTY_MANAGED_BY\", p.\"PRTY_NAME\", p.\"PRTY_NIC\", p.\"PRTY_PASSPORT\", \n" +
                "         p.\"PRTY_SPECIALIZATION_CODE\", p.\"PRTY_STATUS\", p.\"PRTY_TYPE\", p.\"PRTY_INITIALS\"";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("partyCode", partyCode);

        List<Map<String,Object>> result = extractResultSet(query);

        if (result.size() == 0)
            throw new InvalidDataException("No Party found for the Code : " + partyCode);

        for (Map<String,Object> party : result) {

            createDTO(partyDTO, party);

            partyDTO.setContactList(partyContactService.getContactsByPartyCode(partyDTO.getPartyCode(), true));
        }

        return partyDTO;
    }

    @Transactional
    @Override
    public PartyDTO updateParty(String partyCode, PartyDTO partyDTO) {

        validateEntity(partyDTO);
        partyDTO.setBranchId(captureBranchIds().get(0));

        validateByPartyCode(partyCode);

        validatePartyReferenceDetailsOnPersist(partyDTO);

        final Query query = entityManager.createNativeQuery("UPDATE T_MS_PARTY SET PRTY_NAME = :fullName, PRTY_INITIALS = :initials, \n" +
                        "PRTY_FIRST_NAME = :firstName, PRTY_LAST_NAME = :lastName, PRTY_ADDRESS_1 = :address1, PRTY_ADDRESS_2 = :address2, \n" +
                        "PRTY_ADDRESS_3 = :address3, PRTY_DOB = :dob, PRTY_NIC = :nic, PRTY_PASSPORT = :passport, PRTY_MANAGED_BY = :managedBy, \n" +
                        "PRTY_GENDER = :gender, PRTY_SPECIALIZATION_CODE = :specialization, PRTY_BLOOD_GROUP = :bloodGroup, " +
                        "LAST_MOD_DATE = :lastModDate, LAST_MOD_USER_CODE = :lastModUser \n" +
                        "WHERE PRTY_CODE = :partyCode AND PRTY_BRANCH_ID IN (:branchIdList)")
                .setParameter("fullName", partyDTO.getName())
                .setParameter("initials", partyDTO.getInitials())
                .setParameter("firstName", partyDTO.getFirstName())
                .setParameter("lastName", partyDTO.getLastName())
                .setParameter("address1", partyDTO.getAddress1())
                .setParameter("address2", partyDTO.getAddress2())
                .setParameter("address3", partyDTO.getAddress3())
                .setParameter("dob", partyDTO.getDob())
                .setParameter("nic", partyDTO.getNic())
                .setParameter("passport", partyDTO.getPassport())
                .setParameter("managedBy", partyDTO.getManagedBy())
                .setParameter("gender", partyDTO.getGender())
                .setParameter("specialization", partyDTO.getSpecialization())
                .setParameter("bloodGroup", partyDTO.getBloodGroup())
                .setParameter("lastModDate", LocalDateTime.now())
                .setParameter("lastModUser", auditorAware.getCurrentAuditor().get())
                .setParameter("partyCode", partyCode)
                .setParameter("branchIdList", captureBranchIds());

        query.executeUpdate();

        partyDTO.getContactList().forEach(partyContactDTO -> {
            partyContactDTO.setPartyCode(partyCode);

            if(partyContactDTO.getContactId() != null)
                partyContactService.updatePartyContactById(partyContactDTO);
            else
                partyContactService.insertPartyContact(partyContactDTO, true);
        });

        return PartyMapper.INSTANCE.entityToDTO(partyRepository.findByPrtyCodeAndPrtyStatus(partyCode, STATUS_ACTIVE.getShortValue()));
    }

    @Transactional
    @Override
    public Boolean removeParty(String partyCode) {

        validateByPartyCode(partyCode);

        partyContactService.removePartyContactByPartyCode(partyCode);
        userService.removeUserByPartyCode(partyCode, true);

        final Query query = entityManager.createNativeQuery("UPDATE T_MS_PARTY SET PRTY_STATUS = ? WHERE PRTY_CODE = ? AND PRTY_STATUS = ?")
                .setParameter(1, STATUS_INACTIVE.getShortValue())
                .setParameter(2, partyCode)
                .setParameter(3, STATUS_ACTIVE.getShortValue());

        return query.executeUpdate() == 0 ? false : true;
    }

    @Override
    public PaginatedEntity partyPaginatedSearch(String name, String partyType, Integer page, Integer size) {

        PaginatedEntity paginatedPartyList = null;
        List<PartyDTO> partyList = null;

        page = validatePaginateIndexes(page, size);

        partyType = partyType.isEmpty() ? null : partyType;

        String countQueryString = "SELECT COUNT(*) FROM \"T_MS_PARTY\"\n" +
                "WHERE \"PRTY_STATUS\"=:status\n" +
                "  AND (:partyType IS NULL OR (:partyType IS NOT NULL) AND \"PRTY_TYPE\"=:partyType)\n" +
                "  AND (upper(\"PRTY_NAME\") LIKE ('%'||upper(:name)||'%'))\n" +
                "  AND (\"PRTY_BRANCH_ID\" IN (:branchIdList))";

        Query query = entityManager.createNativeQuery(countQueryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("partyType", partyType);
        query.setParameter("name", name);
        query.setParameter("branchIdList", captureBranchIds());

        final int selectedRecordCount = ((Number) query.getSingleResult()).intValue();

        query = null;

        final String queryString = "SELECT p.\"PRTY_CODE\", p.\"CREATED_DATE\", p.\"CREATED_USER_CODE\",\n" +
                "       p.\"LAST_MOD_DATE\", p.\"LAST_MOD_USER_CODE\", p.\"PRTY_BRANCH_ID\",\n" +
                "       p.\"PRTY_DEPARTMENT_CODE\", p.\"PRTY_ADDRESS_1\", p.\"PRTY_ADDRESS_2\",\n" +
                "       p.\"PRTY_ADDRESS_3\", p.\"PRTY_BLOOD_GROUP\", p.\"PRTY_DOB\",\n" +
                "       p.\"PRTY_FIRST_NAME\", p.\"PRTY_GENDER\", p.\"PRTY_LAST_NAME\",\n" +
                "       p.\"PRTY_MANAGED_BY\", p.\"PRTY_NAME\", p.\"PRTY_NIC\", p.\"PRTY_PASSPORT\",\n" +
                "       p.\"PRTY_SPECIALIZATION_CODE\", p.\"PRTY_STATUS\", p.\"PRTY_TYPE\", p.\"PRTY_INITIALS\",\n"+
                "       listagg(pc.\"PTCN_CONTACT_TYPE\", ',') AS \"CONTACT_TYPES\",\n " +
                "       listagg(pc.\"PTCN_CONTACT_NUMBER\", ',') AS \"CONTACT_NOS\",\n" +
                "       listagg(gender.\"CMRF_DESCRIPTION\", ',') AS \"PRTY_GENDER_VALUE\",\n" +
                "       listagg(type.\"CMRF_DESCRIPTION\", ',') AS \"PRTY_TYPE_VALUE\",\n" +
                "       listagg(br.\"BRNH_NAME\", ',') AS \"PRTY_BRANCH_NAME\",\n" +
                "       listagg(dspec.\"CMRF_DESCRIPTION\", ',') AS \"PRTY_SPECIALIZATION\"\n" +
                "FROM \"T_MS_PARTY\" p\n" +
                "LEFT JOIN \"T_MS_PARTY_CONTACT\" pc ON p.\"PRTY_CODE\" = pc.\"PTCN_PRTY_CODE\"\n" +
                "LEFT JOIN \"T_RF_COMMON_REFERENCE\" gender ON p.\"PRTY_GENDER\" = gender.\"CMRF_CODE\"\n" +
                "LEFT JOIN \"T_RF_COMMON_REFERENCE\" type ON p.\"PRTY_TYPE\" = type.\"CMRF_CODE\"\n" +
                "LEFT JOIN \"T_RF_BRANCH\" br ON p.\"PRTY_BRANCH_ID\" = br.\"BRNH_ID\"\n" +
                "LEFT JOIN \"T_RF_COMMON_REFERENCE\" dspec ON p.\"PRTY_SPECIALIZATION_CODE\" = dspec.\"CMRF_CODE\"\n" +
                "WHERE p.\"PRTY_STATUS\"=:status\n" +
                "  AND (:partyType IS NULL OR (:partyType IS NOT NULL) AND p.\"PRTY_TYPE\"=:partyType)\n" +
                "  AND (upper(p.\"PRTY_NAME\") LIKE ('%'||upper(:name)||'%'))\n" +
                "  AND (p.\"PRTY_BRANCH_ID\" IN (:branchIdList))\n" +
                "GROUP BY p.\"PRTY_CODE\", p.\"LAST_MOD_DATE\", p.\"CREATED_DATE\", p.\"CREATED_USER_CODE\", \n" +
                "         p.\"LAST_MOD_USER_CODE\", p.\"PRTY_BRANCH_ID\", p.\"PRTY_DEPARTMENT_CODE\", \n" +
                "         p.\"PRTY_ADDRESS_1\", p.\"PRTY_ADDRESS_2\", p.\"PRTY_ADDRESS_3\", p.\"PRTY_BLOOD_GROUP\", \n" +
                "         p.\"PRTY_DOB\", p.\"PRTY_FIRST_NAME\", p.\"PRTY_GENDER\", p.\"PRTY_LAST_NAME\", \n" +
                "         p.\"PRTY_MANAGED_BY\", p.\"PRTY_NAME\", p.\"PRTY_NIC\", p.\"PRTY_PASSPORT\", \n" +
                "         p.\"PRTY_SPECIALIZATION_CODE\", p.\"PRTY_STATUS\", p.\"PRTY_TYPE\", p.\"PRTY_INITIALS\" \n" +
                "ORDER BY p.\"CREATED_DATE\" OFFSET :page ROWS FETCH NEXT :size ROWS ONLY";

        query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("partyType", partyType);
        query.setParameter("name", name);
        query.setParameter("branchIdList", captureBranchIds());
        query.setParameter("size", size);
        query.setParameter("page", size * page);

        List<Map<String,Object>> result = extractResultSet(query);

        if (result.size() == 0)
            return null;

        paginatedPartyList = new PaginatedEntity();
        partyList = new ArrayList<>();

        for (Map<String,Object> party : result) {

            PartyDTO partyDTO = new PartyDTO();

            createDTO(partyDTO, party);

            partyDTO.setContactList(partyContactService.getContactsByPartyCode(partyDTO.getPartyCode(), true));

            partyList.add(partyDTO);
        }

        paginatedPartyList.setTotalNoOfPages(getTotalNoOfPages(selectedRecordCount, size));
        paginatedPartyList.setTotalNoOfRecords(Long.valueOf(selectedRecordCount));
        paginatedPartyList.setEntities(partyList);

        return paginatedPartyList;
    }

    @Override
    public List<PartyDTO> getPartyListByType(String partyType) {

        partyType = partyType.isEmpty() ? null : partyType;

        final String queryString = "SELECT p.\"PRTY_CODE\", p.\"PRTY_NAME\" \n" +
                "FROM \"T_MS_PARTY\" p\n" +
                "WHERE p.\"PRTY_STATUS\"=:status\n" +
                "  AND (:partyType IS NULL OR (:partyType IS NOT NULL) AND p.\"PRTY_TYPE\"=:partyType)\n" +
                "  AND (p.\"PRTY_BRANCH_ID\" IN (:branchIdList))\n" +
                "ORDER BY p.\"LAST_MOD_DATE\"";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("partyType", partyType);
        query.setParameter("branchIdList", captureBranchIds());

        List<Map<String,Object>> result = extractResultSet(query);

        if(result.size() == 0)
            return Collections.emptyList();

        List<PartyDTO> partyDTOList = new ArrayList<>();

        for (Map<String,Object> party : result) {

            PartyDTO partyDTO = new PartyDTO();

            partyDTO.setPartyCode(extractValue(String.valueOf(party.get("PRTY_CODE"))));
            partyDTO.setName(extractValue(String.valueOf(party.get("PRTY_NAME"))));

            partyDTOList.add(partyDTO);
        }

        return partyDTOList;
    }

    private void validatePartyReferenceDetailsOnPersist(PartyDTO partyDTO) {

        if(partyDTO.getType() != null)
            commonReferenceService
                    .getByCmrfCodeAndCmrtCode(PARTY_TYPES.getValue(), partyDTO.getType());

        if(partyDTO.getGender() != null)
            commonReferenceService
                    .getByCmrfCodeAndCmrtCode(GENDER_TYPES.getValue(), partyDTO.getGender());

        if(partyDTO.getBloodGroup() != null)
            commonReferenceService
                    .getByCmrfCodeAndCmrtCode(BLOOD_GROUP_TYPES.getValue(), partyDTO.getBloodGroup());

        if(partyDTO.getSpecialization() != null)
            commonReferenceService
                    .getByCmrfCodeAndCmrtCode(DOC_SPECIALIZATION_TYPES.getValue(), partyDTO.getSpecialization());
    }

    private void createDTO(PartyDTO partyDTO, Map<String,Object> party) {

        partyDTO.setPartyCode(extractValue(String.valueOf(party.get("PRTY_CODE"))));
        partyDTO.setName(extractValue(String.valueOf(party.get("PRTY_NAME"))));
        partyDTO.setBranchId(extractLongValue(String.valueOf(party.get("PRTY_BRANCH_ID"))));
        partyDTO.setBranchName(extractValue(String.valueOf(party.get("PRTY_BRANCH_NAME"))));
        partyDTO.setAddress1(extractValue(String.valueOf(party.get("PRTY_ADDRESS_1"))));
        partyDTO.setAddress2(extractValue(String.valueOf(party.get("PRTY_ADDRESS_2"))));
        partyDTO.setAddress3(extractValue(String.valueOf(party.get("PRTY_ADDRESS_3"))));
        partyDTO.setCreatedDate(extractDateTime(String.valueOf(party.get("CREATED_DATE"))));
        partyDTO.setCreatedUserCode(extractValue(String.valueOf(party.get("CREATED_USER_CODE"))));
        partyDTO.setDob(extractDate(String.valueOf(party.get("PRTY_DOB"))));
        partyDTO.setInitials(extractValue(String.valueOf(party.get("PRTY_INITIALS"))));
        partyDTO.setFirstName(extractValue(String.valueOf(party.get("PRTY_FIRST_NAME"))));
        partyDTO.setLastName(extractValue(String.valueOf(party.get("PRTY_LAST_NAME"))));
        partyDTO.setLastUpdatedDate(extractDateTime(String.valueOf(party.get("LAST_MOD_DATE"))));
        partyDTO.setLastUpdatedUserCode(extractValue(String.valueOf(party.get("LAST_MOD_USER_CODE"))));
        partyDTO.setGender(extractValue(String.valueOf(party.get("PRTY_GENDER"))));
        partyDTO.setGenderName(extractValue(String.valueOf(party.get("PRTY_GENDER_VALUE"))));
        partyDTO.setBloodGroup(extractValue(String.valueOf(party.get("PRTY_BLOOD_GROUP"))));
        partyDTO.setNic(extractValue(String.valueOf(party.get("PRTY_NIC"))));
        partyDTO.setPassport(extractValue(String.valueOf(party.get("PRTY_PASSPORT"))));
        partyDTO.setManagedBy(extractValue(String.valueOf(party.get("PRTY_MANAGED_BY"))));
        partyDTO.setManagedByName(extractValue(String.valueOf(party.get("PRTY_MANAGED_BY"))));
        partyDTO.setType(extractValue(String.valueOf(party.get("PRTY_TYPE"))));
        partyDTO.setSpecialization(extractValue(String.valueOf(party.get("PRTY_SPECIALIZATION_CODE"))));
        partyDTO.setSpecializationName(extractValue(String.valueOf(party.get("PRTY_SPECIALIZATION"))));
        partyDTO.setStatus(extractShortValue(String.valueOf(party.get("PRTY_STATUS"))));
    }

    private void validateByPartyCode(String partyCode) {

        if (Strings.isNullOrEmpty(partyCode))
            throw new InvalidDataException("Party Code is required");

        /*final String queryString = "SELECT p.\"PRTY_CODE\" \n" +
                "FROM \"T_MS_PARTY\" p\n" +
                "WHERE p.\"PRTY_STATUS\"=:status\n" +
                "  AND (p.\"PRTY_BRANCH_ID\" IN (:branchIdList))\n";*/

        final String queryString = "SELECT p.\"PRTY_CODE\" \n" +
                "FROM \"T_MS_PARTY\" p\n" +
                "WHERE p.\"PRTY_STATUS\"=:status";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        //query.setParameter("branchIdList", captureBranchIds());

        List<Map<String,Object>> result = extractResultSet(query);

        if(result.size() == 0)
            throw new DataNotFoundException("Party not found for the Code : " + partyCode);
    }
}

package lk.healthylife.hms.service.impl;

import com.google.common.base.Strings;
import lk.healthylife.hms.dto.DropDownDTO;
import lk.healthylife.hms.exception.InvalidDataException;
import lk.healthylife.hms.exception.NoRequiredInfoException;
import lk.healthylife.hms.config.repository.FunctionRepository;
import lk.healthylife.hms.config.repository.RoleRepository;
import lk.healthylife.hms.service.*;
import lk.healthylife.hms.util.DateConversion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DropDownServiceImpl implements DropDownService {

    private static final String BRANCHES = "BRANCH";
    private static final String DEPARTMENTS = "DEPART";
    private static final String PERMISSIONS = "PERMIS";
    private static final String ROLES = "ROLES";
    private static final String PATIENT = "PATNT";
    private static final String DOCTOR = "DOCTR";
    private static final String ROOM_TYPES = "ROMTP";
    private static final String ROOMS = "ROOMS";
    private static final String MEASUREMENTS_UNITS = "UOFMS";
    private static final String ITEM_TYPES = "ITMTP";
    private static final String PAYMENT_TYPES = "PAYTP";
    private static final String MEDICINES = "MEDCN";
    private static final String MEDICINE_TYPES = "MEDTP";
    private static final String FACILITIES = "FCLTY";
    private static final String SYMPTOMS = "SYMTP";
    private static final String MEDICINE_BRANDS = "MDBRD";
    private static final String SURGERIES = "SUGRIS";
    private static final String PARTY_TYPES = "PTYPE";
    private static final String DOCTOR_SPECIALIZATION = "DCSPC";
    private static final String MEDICAL_TEST_TYPES = "MDTTP";
    private static final String MEDICAL_TESTS = "MDTST";
    private static final String PATIENT_CONDITION_WHEN = "CONDW";
    private static final String SURGERY_TYPES = "SRGTP";
    private static final String ADMISSIONS = "ADMIS";
    private static final String MEAL_TIME = "PMLTM";
    private static final String MODE_OF_MEDICINE = "MDPMD";

    private final BranchService branchService;
    private final DepartmentService departmentService;
    private final PartyService partyService;
    private final CommonReferenceService commonReferenceService;
    private final RoomService roomService;
    private final MedicineService medicineService;
    private final FacilityService facilityService;
    private final SymptomService symptomService;
    private final SurgeryService surgeryService;
    private final MedicalTestService medicalTestService;
    private final PatientAdmissionService patientAdmissionService;

    private final FunctionRepository functionRepository;
    private final RoleRepository roleRepository;

    public DropDownServiceImpl(BranchService branchService,
                               DepartmentService departmentService,
                               PartyService partyService,
                               CommonReferenceService commonReferenceService,
                               RoomService roomService,
                               MedicineService medicineService,
                               FacilityService facilityService,
                               SymptomService symptomService,
                               SurgeryService surgeryService,
                               MedicalTestService medicalTestService,
                               PatientAdmissionService patientAdmissionService,
                               FunctionRepository functionRepository,
                               RoleRepository roleRepository) {
        this.branchService = branchService;
        this.departmentService = departmentService;
        this.partyService = partyService;
        this.commonReferenceService = commonReferenceService;
        this.roomService = roomService;
        this.medicineService = medicineService;
        this.facilityService = facilityService;
        this.symptomService = symptomService;
        this.surgeryService = surgeryService;
        this.medicalTestService = medicalTestService;
        this.patientAdmissionService = patientAdmissionService;
        this.functionRepository = functionRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public Map<String, String> getDropDownCodes() {

        Map<String, String> dropDownCodes = new HashMap<>();

        dropDownCodes.put("BRANCHES", BRANCHES);
        dropDownCodes.put("DEPARTMENTS", DEPARTMENTS);
        dropDownCodes.put("PERMISSIONS", PERMISSIONS);
        dropDownCodes.put("ROLES", ROLES);
        dropDownCodes.put("PATIENT", PATIENT);
        dropDownCodes.put("DOCTOR", DOCTOR);
        dropDownCodes.put("ROOM_TYPES", ROOM_TYPES);
        dropDownCodes.put("MEASUREMENTS_UNITS", MEASUREMENTS_UNITS);
        dropDownCodes.put("ITEM_TYPES", ITEM_TYPES);
        dropDownCodes.put("PAYMENT_TYPES", PAYMENT_TYPES);
        dropDownCodes.put("ROOMS", ROOMS);
        dropDownCodes.put("MEDICINES", MEDICINES);
        dropDownCodes.put("FACILITIES", FACILITIES);
        dropDownCodes.put("SYMPTOMS", SYMPTOMS);
        dropDownCodes.put("SURGERIES", SURGERIES);
        dropDownCodes.put("PARTY_TYPES", PARTY_TYPES);
        dropDownCodes.put("MEDICINE_TYPES", MEDICINE_TYPES);
        dropDownCodes.put("MEDICINE_BRANDS", MEDICINE_BRANDS);
        dropDownCodes.put("DOCTOR_SPECIALIZATION", DOCTOR_SPECIALIZATION);
        dropDownCodes.put("MEDICAL_TESTS", MEDICAL_TESTS);
        dropDownCodes.put("MEDICAL_TEST_TYPES", MEDICAL_TEST_TYPES);
        dropDownCodes.put("PATIENT_CONDITION_WHEN", PATIENT_CONDITION_WHEN);
        dropDownCodes.put("SURGERY_TYPES", SURGERY_TYPES);
        dropDownCodes.put("ADMISSIONS", ADMISSIONS);
        dropDownCodes.put("MEAL_TIME", MEAL_TIME);
        dropDownCodes.put("MODE_OF_MEDICINE", MODE_OF_MEDICINE);

        return dropDownCodes;
    }

    @Override
    public List<DropDownDTO> getDropDownByCode(String code) {

        if(Strings.isNullOrEmpty(code))
            throw new NoRequiredInfoException("Code is required");

        List<DropDownDTO> downDTOList = new ArrayList<>();

        switch (code) {
            case BRANCHES :
                List<DropDownDTO> branchList = downDTOList;
                branchService.getAllBranches().forEach(branchDTO -> {
                    branchList.add(new DropDownDTO(
                            branchDTO.getBranchId().toString(),
                            branchDTO.getMame(),
                            null,
                            branchDTO.getStatus()));
                });
                break;
            case DEPARTMENTS :
                List<DropDownDTO> departmentList = downDTOList;
                departmentService.getAllDepartmentsDropdown().forEach(departmentDTO -> {
                    departmentList.add(new DropDownDTO(
                            departmentDTO.getDepartmentCode(),
                            departmentDTO.getName(),
                            null,
                            departmentDTO.getStatus()));
                });
                break;
            case PERMISSIONS :
                List<DropDownDTO> permissionList = downDTOList;
                functionRepository.findAll().forEach(tMsFunction -> {
                    permissionList.add(new DropDownDTO(
                            tMsFunction.getFuncId(),
                            tMsFunction.getDunsDescription(),
                            null,
                            tMsFunction.getFuncStatus()));
                });
                break;
            case ROLES :
                List<DropDownDTO> roleList = downDTOList;
                roleRepository.findAll().forEach(tMsRole -> {
                    roleList.add(new DropDownDTO(
                            tMsRole.getRoleName(),
                            tMsRole.getRoleName(),
                            tMsRole.getRoleDescription(),
                            tMsRole.getRoleStatus()));
                });
                break;
            case PATIENT :
                List<DropDownDTO> patientList = downDTOList;
                partyService.getPartyListByType(PATIENT).forEach(partyDTO -> {
                    patientList.add(new DropDownDTO(
                            partyDTO.getPartyCode(),
                            partyDTO.getName(),
                            null,
                            null
                    ));
                });
                break;
            case DOCTOR :
                List<DropDownDTO> doctorList = downDTOList;
                partyService.getPartyListByType(DOCTOR).forEach(partyDTO -> {
                    doctorList.add(new DropDownDTO(
                            partyDTO.getPartyCode(),
                            partyDTO.getName(),
                            null,
                            null
                    ));
                });
                break;
            case ROOMS :
                List<DropDownDTO> roomList = downDTOList;
                roomService.getAllRoomsDropdown().forEach(roomDTO -> {
                    roomList.add(new DropDownDTO(
                            String.valueOf(roomDTO.getRoomId()),
                            roomDTO.getRoomNo(),
                            null,
                            null
                    ));
                });
                break;
            case MEDICINES :
                List<DropDownDTO> medicineList = downDTOList;
                medicineService.getAllMedicinesDropdown().forEach(medicineDTO -> {
                    medicineList.add(new DropDownDTO(
                            String.valueOf(medicineDTO.getMedicineId()),
                            medicineDTO.getName(),
                            null,
                            null
                    ));
                });
                break;
            case FACILITIES :
                List<DropDownDTO> facilityList = downDTOList;
                facilityService.getAllFacilitiesDropdown().forEach(facilityDTO -> {
                    facilityList.add(new DropDownDTO(
                            String.valueOf(facilityDTO.getFacilityId()),
                            facilityDTO.getName(),
                            null,
                            null
                    ));
                });
                break;
            case SYMPTOMS :
                List<DropDownDTO> symptomList = downDTOList;
                symptomService.getAllSymptomsDropdown().forEach(symptomDTO -> {
                    symptomList.add(new DropDownDTO(
                            String.valueOf(symptomDTO.getSymptomId()),
                            symptomDTO.getName(),
                            null,
                            null
                    ));
                });
                break;
            case SURGERIES :
                List<DropDownDTO> surgeriesList = downDTOList;
                surgeryService.getAllSurgeriesDropdown().forEach(surgeryDTO -> {
                    surgeriesList.add(new DropDownDTO(
                            String.valueOf(surgeryDTO.getSurgeryId()),
                            surgeryDTO.getName(),
                            null,
                            null
                    ));
                });
                break;
            case MEDICAL_TESTS :
                List<DropDownDTO> medicalTestList = downDTOList;
                medicalTestService.getAllMedicalTestsDropdown().forEach(medicalTestDTO -> {
                    medicalTestList.add(new DropDownDTO(
                            String.valueOf(medicalTestDTO.getMedicalTestId()),
                            medicalTestDTO.getName(),
                            null,
                            null
                    ));
                });
                break;
            case ADMISSIONS :
                List<DropDownDTO> admissionList = downDTOList;
                patientAdmissionService.getAdmissionListForDropDown().forEach(admissionDTO -> {
                    admissionList.add(new DropDownDTO(
                            String.valueOf(admissionDTO.getPatientAdmissionId()),
                            DateConversion.convertLocalDateTimeToString(admissionDTO.getAdmittedDate()),
                            admissionDTO.getPatientName(),
                            null
                    ));
                });
                break;
            case ROOM_TYPES :
                downDTOList = populateFromCommonReference(ROOM_TYPES);
                break;
            case MEASUREMENTS_UNITS :
                downDTOList = populateFromCommonReference(MEASUREMENTS_UNITS);
                break;
            case ITEM_TYPES :
                downDTOList = populateFromCommonReference(ITEM_TYPES);
                break;
            case PAYMENT_TYPES :
                downDTOList = populateFromCommonReference(PAYMENT_TYPES);
                break;
            case PARTY_TYPES :
                downDTOList = populateFromCommonReference(PARTY_TYPES);
                break;
            case MEDICINE_TYPES :
                downDTOList = populateFromCommonReference(MEDICINE_TYPES);
                break;
            case MEDICINE_BRANDS :
                downDTOList = populateFromCommonReference(MEDICINE_BRANDS);
                break;
            case DOCTOR_SPECIALIZATION :
                downDTOList = populateFromCommonReference(DOCTOR_SPECIALIZATION);
                break;
            case PATIENT_CONDITION_WHEN :
                downDTOList = populateFromCommonReference(PATIENT_CONDITION_WHEN);
                break;
            case SURGERY_TYPES :
                downDTOList = populateFromCommonReference(SURGERY_TYPES);
                break;
            case MEDICAL_TEST_TYPES :
                downDTOList = populateFromCommonReference(MEDICAL_TEST_TYPES);
                break;
            case MEAL_TIME :
                downDTOList = populateFromCommonReference(MEAL_TIME);
                break;
            case MODE_OF_MEDICINE :
                downDTOList = populateFromCommonReference(MODE_OF_MEDICINE);
                break;
            default:
                throw new InvalidDataException("Requested Dropdown Code is invalid");
        }

        return downDTOList;
    }

    private List<DropDownDTO> populateFromCommonReference(String code) {

        List<DropDownDTO> downDTOList = new ArrayList<>();

        commonReferenceService.getAllByCmrtCode(code).forEach(commonReferenceDTO -> {
            downDTOList.add(new DropDownDTO(
                    commonReferenceDTO.getCmrfCode(),
                    commonReferenceDTO.getDescription(),
                    null,
                    null
            ));
        });

        return downDTOList;
    }
}

package lk.healthylife.hms.service;

import lk.healthylife.hms.dto.PatientConditionDTO;

import java.util.List;

public interface PatientConditionService {

    PatientConditionDTO addPatientCondition(PatientConditionDTO patientConditionDTO);

    PatientConditionDTO getPatientConditionById(Long patientConditionId);

    List<PatientConditionDTO> getPatientConditionListByAdmission(Long admissionId);

    Boolean removePatientCondition(Long patientConditionId);

    Boolean removePatientConditionsByAdmission(Long admissionId);
}

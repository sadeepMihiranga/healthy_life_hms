package lk.healthylife.hms.service;

import lk.healthylife.hms.dto.PaginatedEntity;
import lk.healthylife.hms.dto.PatientMedicalTestDTO;

public interface PatientMedicalTestService {

    PatientMedicalTestDTO insertPatientMedicalTest(PatientMedicalTestDTO patientMedicalTestDTO);

    PatientMedicalTestDTO getPatientMedicalTestById(Long patientMedicalTestId);

    PaginatedEntity patientMedicalTestPaginatedSearch(String patientName, String testName, Integer page, Integer size);

    PatientMedicalTestDTO updatePatientMedicalTest(Long patientMedicalTestId, PatientMedicalTestDTO patientMedicalTestDTO);

    Boolean removeMedicalTest(Long patientMedicalTestId);

    PatientMedicalTestDTO approveMedicalTest(Long patientMedicalTestId);
}

package lk.healthylife.hms.service;

import lk.healthylife.hms.dto.PaginatedEntity;
import lk.healthylife.hms.dto.PatientAdmissionDTO;

public interface PatientAdmissionService {

    PatientAdmissionDTO admitPatient(PatientAdmissionDTO patientAdmissionDTO);

    PatientAdmissionDTO dischargePatient(Long admissionId);

    PatientAdmissionDTO getAdmissionById(Long admissionId);

    PaginatedEntity admissionPaginatedSearch(String patientName, String roomNo, Integer page, Integer size);

    Boolean removeAdmission(Long admissionId);
}

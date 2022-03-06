package lk.healthylife.hms.service;

import lk.healthylife.hms.dto.DoctorSurgeryDTO;
import lk.healthylife.hms.dto.PaginatedEntity;
import lk.healthylife.hms.dto.PatientSurgeryDTO;

import java.util.List;

public interface PatientSurgeryService {

    PatientSurgeryDTO addPatientToSurgery(PatientSurgeryDTO patientSurgeryDTO);

    PatientSurgeryDTO finishPatientSurgery(Long patientSurgeryId);

    PatientSurgeryDTO getPatientSurgeryById(Long patientSurgeryId);

    PaginatedEntity patientSurgeryPaginatedSearch(String patientName, String doctorCode, String surgeryName, Integer page, Integer size);

    Boolean removePatientSurgery(Long patientSurgeryId);

    List<PatientSurgeryDTO> getPatientSurgeriesByAdmission(Long admissionId);


    DoctorSurgeryDTO assignDoctorToSurgery(DoctorSurgeryDTO doctorSurgeryDTO);

    DoctorSurgeryDTO getDoctorSurgeryById(Long doctorSurgeryId);

    List<DoctorSurgeryDTO> getDoctorsByPatientSurgeryId(Long patientSurgeryId);

    Boolean removeDoctorSurgery(Long patientSurgeryId);
}

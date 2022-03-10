package lk.healthylife.hms.service;

import lk.healthylife.hms.dto.PaginatedEntity;
import lk.healthylife.hms.dto.PrescriptionDTO;

import javax.transaction.Transactional;

public interface PrescriptionService {

    PrescriptionDTO createPrescription(PrescriptionDTO prescriptionDTO);

    PrescriptionDTO getPrescriptionById(Long prescriptionId);

    PaginatedEntity prescriptionPaginatedSearch(String doctorCode, String patientName, String patientNic,
                                                Integer page, Integer size);

    Boolean removePrescription(Long prescriptionId);
}

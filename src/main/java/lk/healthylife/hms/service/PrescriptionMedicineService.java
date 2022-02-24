package lk.healthylife.hms.service;

import lk.healthylife.hms.dto.PrescriptionMedicineDTO;

import java.util.List;

public interface PrescriptionMedicineService {

    PrescriptionMedicineDTO createPrescriptionMedicine(PrescriptionMedicineDTO prescriptionMedicineDTO);

    PrescriptionMedicineDTO getPrescriptionMedicineById(Long prescriptionMedicineId);

    List<PrescriptionMedicineDTO> getPrescriptionMedicineListByPrescriptionId(Long prescriptionId);

    Boolean removePrescriptionMedicineByPrescription(Long prescriptionId);
}

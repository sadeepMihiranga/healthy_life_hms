package lk.healthylife.hms.service;

import lk.healthylife.hms.dto.MedicalTestDTO;
import lk.healthylife.hms.dto.MedicineDTO;
import lk.healthylife.hms.dto.PaginatedEntity;

import java.util.List;

public interface MedicalTestService {

    List<MedicalTestDTO> getAllMedicalTestsDropdown();

    MedicalTestDTO insertMedicalTest(MedicalTestDTO medicalTestDTO);

    MedicalTestDTO getMedicalTestById(Long medicalTestId);

    MedicalTestDTO updateMedicalTest(Long medicalTestId, MedicalTestDTO medicalTestDTO);

    Boolean removeMedicalTest(Long medicalTestId);

    PaginatedEntity medicalTestPaginatedSearch(String name, String type, Integer page, Integer size);
}

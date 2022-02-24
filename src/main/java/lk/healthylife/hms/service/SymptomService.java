package lk.healthylife.hms.service;

import lk.healthylife.hms.dto.SymptomDTO;

import java.util.List;

public interface SymptomService {

    List<SymptomDTO> getAllSymptomsDropdown();

    SymptomDTO getSymptomById(Long symptomId);
}

package lk.healthylife.hms.service;

import lk.healthylife.hms.dto.PaginatedEntity;
import lk.healthylife.hms.dto.SurgeryDTO;

import java.util.List;

public interface SurgeryService {

    List<SurgeryDTO> getAllSurgeriesDropdown();

    SurgeryDTO createSurgery(SurgeryDTO surgeryDTO);

    SurgeryDTO getSurgeryById(Long surgeryId);

    PaginatedEntity surgeryPaginatedSearch(String name, String type, Integer page, Integer size);

    Boolean removeSurgery(Long surgeryId);

    SurgeryDTO updateSurgery(Long surgeryId, SurgeryDTO surgeryDTO);
}

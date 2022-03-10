package lk.healthylife.hms.service;

import lk.healthylife.hms.dto.FacilityDTO;
import lk.healthylife.hms.dto.PaginatedEntity;

import javax.transaction.Transactional;
import java.util.List;

public interface FacilityService {

    List<FacilityDTO> getAllFacilitiesDropdown();

    FacilityDTO createFacility(FacilityDTO facilityDTO);

    FacilityDTO updateFacility(Long facilityId,  FacilityDTO facilityDTO);

    FacilityDTO getFacilityById(Long facilityId);

    PaginatedEntity facilityPaginatedSearch(String facilityName, Integer page, Integer size);

    Boolean removeFacility(Long facilityId);
}

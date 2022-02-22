package lk.healthylife.hms.service;

import lk.healthylife.hms.dto.FacilityDTO;
import lk.healthylife.hms.dto.PaginatedEntity;

import javax.transaction.Transactional;
import java.util.List;

public interface FacilityService {

    List<FacilityDTO> getAllFacilitiesDropdown();

    @Transactional
    FacilityDTO createFacility(FacilityDTO facilityDTO);

    @Transactional
    FacilityDTO updateFacility(Long facilityId,  FacilityDTO facilityDTO);

    FacilityDTO getFacilityById(Long facilityId);

    PaginatedEntity facilityPaginatedSearch(String facilityName, Integer page, Integer size);

    @Transactional
    Boolean removeFacility(Long facilityId);
}

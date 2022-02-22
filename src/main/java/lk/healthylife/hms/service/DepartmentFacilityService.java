package lk.healthylife.hms.service;

import lk.healthylife.hms.dto.DepartmentFacilityDTO;
import lk.healthylife.hms.dto.DepartmentLocationDTO;

import java.util.List;

public interface DepartmentFacilityService {

    DepartmentFacilityDTO addFacilityToDepartment(String departmentCode, DepartmentFacilityDTO departmentFacilityDTO);

    Boolean removeFacilityFromDepartment(String departmentCode, Long departmentFacilityId);

    DepartmentFacilityDTO getDepartmentFacilityById(Long departmentFacilityId);

    List<DepartmentFacilityDTO> getDepartmentFacilitiesByDepartmentCode(String departmentCode);
}

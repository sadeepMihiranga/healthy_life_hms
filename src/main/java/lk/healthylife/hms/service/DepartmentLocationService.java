package lk.healthylife.hms.service;

import lk.healthylife.hms.dto.DepartmentLocationDTO;

import java.util.List;

public interface DepartmentLocationService {

    DepartmentLocationDTO addLocationToDepartment(String departmentCode, DepartmentLocationDTO departmentLocationDTO);

    Boolean removeLocationFromDepartment(String departmentCode, Long departmentLocationId);

    DepartmentLocationDTO getDepartmentLocationById(Long departmentLocationId);

    List<DepartmentLocationDTO> getDepartmentLocationsByDepartmentCode(String departmentCode);
}

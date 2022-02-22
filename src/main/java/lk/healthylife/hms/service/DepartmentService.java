package lk.healthylife.hms.service;

import lk.healthylife.hms.dto.DepartmentDTO;
import lk.healthylife.hms.dto.PaginatedEntity;

import javax.transaction.Transactional;
import java.util.List;

public interface DepartmentService {

    List<DepartmentDTO> getAllDepartmentsDropdown();

    @Transactional
    DepartmentDTO createDepartment(DepartmentDTO departmentDTO);

    @Transactional
    DepartmentDTO updateDepartment(String departmentCode, DepartmentDTO departmentDTO);

    @Transactional
    Boolean removeDepartment(String departmentCode);

    PaginatedEntity departmentPaginatedSearch(String departmentCode, String departmentName, Integer page, Integer size);

    DepartmentDTO getDepartmentByCode(String departmentCode);
}

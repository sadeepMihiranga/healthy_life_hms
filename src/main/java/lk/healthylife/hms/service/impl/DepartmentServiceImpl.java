package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.dto.DepartmentDTO;
import lk.healthylife.hms.entity.TMsDepartment;
import lk.healthylife.hms.mapper.DepartmentMapper;
import lk.healthylife.hms.repository.DepartmentRepository;
import lk.healthylife.hms.service.DepartmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static lk.healthylife.hms.util.constant.Constants.STATUS_ACTIVE;

@Slf4j
@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    public List<DepartmentDTO> getAllDepartments() {

        final List<TMsDepartment> tMsDepartmentList = departmentRepository
                .findAllByDpmtStatus(STATUS_ACTIVE.getShortValue());

        if(tMsDepartmentList.isEmpty() || tMsDepartmentList == null)
            return Collections.emptyList();

        List<DepartmentDTO> departmentDTOList = new ArrayList<>();

        tMsDepartmentList.forEach(tMsDepartment -> {
            departmentDTOList.add(DepartmentMapper.INSTANCE.entityToDTO(tMsDepartment));
        });

        return departmentDTOList;
    }
}

package lk.healthylife.hms.mapper;

import lk.healthylife.hms.dto.DepartmentDTO;
import lk.healthylife.hms.entity.TMsDepartment;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DepartmentMapper {

    DepartmentMapper INSTANCE = Mappers.getMapper(DepartmentMapper.class);

    @Mappings({
            @Mapping(source = "dpmtCode", target = "departmentCode"),
            @Mapping(source = "dpmtName", target = "mame"),
            @Mapping(source = "dpmtDescription", target = "description"),
            @Mapping(source = "dpmtStatus", target = "status")
    })
    DepartmentDTO entityToDTO(TMsDepartment entity);

    @InheritInverseConfiguration
    TMsDepartment dtoToEntity(DepartmentDTO dto);
}

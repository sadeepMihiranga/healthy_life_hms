package lk.healthylife.hms.mapper;

import lk.healthylife.hms.dto.CommonReferenceDTO;
import lk.healthylife.hms.entity.TRfCommonReference;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CommonReferenceMapper {

    CommonReferenceMapper INSTANCE = Mappers.getMapper(CommonReferenceMapper.class);

    @Mappings({
            @Mapping(source = "cmrfCode", target = "cmrfCode"),
            @Mapping(source = "referenceType.cmrtCode", target = "referenceType"),
            @Mapping(source = "cmrfDescription", target = "description"),
            @Mapping(source = "cmrfStringValue", target = "stringValue"),
            @Mapping(source = "cmrfNumberValue", target = "numberValue"),
            @Mapping(source = "cmrfStatus", target = "status")
    })
    CommonReferenceDTO entityToDTO(TRfCommonReference entity);

    @InheritInverseConfiguration
    TRfCommonReference dtoToEntity(CommonReferenceDTO dto);
}

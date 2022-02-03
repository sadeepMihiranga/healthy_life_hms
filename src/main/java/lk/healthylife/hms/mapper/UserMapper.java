package lk.healthylife.hms.mapper;

import lk.healthylife.hms.dto.UserDTO;
import lk.healthylife.hms.entity.TMsUser;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    @Mappings({
            @Mapping(source = "userId", target = "id"),
            @Mapping(source = "party.prtyName", target = "displayName"),
            @Mapping(source = "userUsername", target = "username"),
            @Mapping(source = "userPassword", target = "password"),
            @Mapping(source = "party.prtyCode", target = "partyCode"),
            @Mapping(source = "userStatus", target = "status")
    })
    UserDTO entityToDTO(TMsUser entity);

    @InheritInverseConfiguration
    TMsUser dtoToEntity(UserDTO dto);
}

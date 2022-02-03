package lk.healthylife.hms.mapper;

import lk.healthylife.hms.dto.PartyContactDTO;
import lk.healthylife.hms.entity.TMsPartyContact;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PartyContactMapper {

    PartyContactMapper INSTANCE = Mappers.getMapper(PartyContactMapper.class);

    @Mappings({
            @Mapping(source = "ptcnId", target = "contactId"),
            @Mapping(source = "party.prtyCode", target = "partyCode"),
            @Mapping(source = "ptcnContactType", target = "contactType"),
            @Mapping(source = "ptcnContactNumber", target = "contactNumber"),
            @Mapping(source = "ptcnStatus", target = "status")
    })
    PartyContactDTO entityToDTO(TMsPartyContact entity);

    @InheritInverseConfiguration
    TMsPartyContact dtoToEntity(PartyContactDTO dto);
}

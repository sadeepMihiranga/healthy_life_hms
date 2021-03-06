package lk.healthylife.hms.service;

import lk.healthylife.hms.dto.RoomDTO;
import lk.healthylife.hms.dto.PaginatedEntity;

import javax.transaction.Transactional;
import java.util.List;

public interface RoomService {

    List<RoomDTO> getAllRoomsDropdown();

    RoomDTO createRoom(RoomDTO roomDTO);

    RoomDTO updateRoom(Long roomId, RoomDTO roomDTO);

    Boolean removeRoom(Long roomId);

    PaginatedEntity roomPaginatedSearch(String roomNo, String roomType, Integer page, Integer size);

    RoomDTO getRoomByIdOrNo(Long roomId, String roomNo);
}

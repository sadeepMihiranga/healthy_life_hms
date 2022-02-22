package lk.healthylife.hms.controller;

import lk.healthylife.hms.dto.RoomDTO;
import lk.healthylife.hms.response.SuccessResponse;
import lk.healthylife.hms.response.SuccessResponseHandler;
import lk.healthylife.hms.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@CrossOrigin
@RestController
@RequestMapping("/api/room")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping(path = "/search")
    public ResponseEntity<SuccessResponse> searchRoomsPaginated(@RequestParam(name = "roomNo", required = false) String roomNo,
                                                                @RequestParam(name = "roomType", required = false) String roomType,
                                                                @RequestParam(name = "page", required = true) Integer page,
                                                                @RequestParam(name = "size", required = true) Integer size) {
        return SuccessResponseHandler.generateResponse(roomService.roomPaginatedSearch(roomNo, roomType, page, size));
    }

    @PostMapping
    public ResponseEntity<SuccessResponse> insertRoom(@RequestBody RoomDTO roomDTO) {
        return SuccessResponseHandler.generateResponse(roomService.createRoom(roomDTO));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<SuccessResponse> getRoomById(@PathVariable("roomId") Long roomId) {
        return SuccessResponseHandler.generateResponse(roomService.getRoomByIdOrNo(roomId, null));
    }

    @GetMapping("/byNo/{roomNo}")
    public ResponseEntity<SuccessResponse> getRoomByRoomNo(@PathVariable("roomNo") String roomNo) {
        return SuccessResponseHandler.generateResponse(roomService.getRoomByIdOrNo(null, roomNo));
    }

    @PutMapping("/{roomId}")
    public ResponseEntity<SuccessResponse> updateRoom(@PathVariable("roomId") Long roomId,
                                                       @RequestBody RoomDTO roomDTO) throws IOException {
        return SuccessResponseHandler.generateResponse(roomService.updateRoom(roomId, roomDTO));
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<SuccessResponse> removeRoom(@PathVariable("roomId") Long roomId) {
        return SuccessResponseHandler.generateResponse(roomService.removeRoom(roomId));
    }
}

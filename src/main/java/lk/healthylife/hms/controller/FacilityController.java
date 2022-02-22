package lk.healthylife.hms.controller;

import lk.healthylife.hms.dto.FacilityDTO;
import lk.healthylife.hms.response.SuccessResponse;
import lk.healthylife.hms.response.SuccessResponseHandler;
import lk.healthylife.hms.service.FacilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@CrossOrigin
@RestController
@RequestMapping("/api/facility")
public class FacilityController {

    private final FacilityService facilityService;

    public FacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @PostMapping
    public ResponseEntity<SuccessResponse> insertFacility(@RequestBody FacilityDTO facilityDTO) {
        return SuccessResponseHandler.generateResponse(facilityService.createFacility(facilityDTO));
    }

    @GetMapping("/{facilityId}")
    public ResponseEntity<SuccessResponse> getFacilityById(@PathVariable("facilityId") Long facilityId) {
        return SuccessResponseHandler.generateResponse(facilityService.getFacilityById(facilityId));
    }

    @GetMapping(path = "/search")
    public ResponseEntity<SuccessResponse> searchRoomsPaginated(@RequestParam(name = "name", required = false) String name,
                                                                @RequestParam(name = "page", required = true) Integer page,
                                                                @RequestParam(name = "size", required = true) Integer size) {
        return SuccessResponseHandler.generateResponse(facilityService.facilityPaginatedSearch(name, page, size));
    }

    @PutMapping("/{facilityId}")
    public ResponseEntity<SuccessResponse> updateFacility(@PathVariable("facilityId") Long facilityId,
                                                          @RequestBody FacilityDTO facilityDTO) throws IOException {
        return SuccessResponseHandler.generateResponse(facilityService.updateFacility(facilityId, facilityDTO));
    }

    @DeleteMapping("/{facilityId}")
    public ResponseEntity<SuccessResponse> removeRoom(@PathVariable("facilityId") Long facilityId) {
        return SuccessResponseHandler.generateResponse(facilityService.removeFacility(facilityId));
    }
}

package lk.healthylife.hms.controller;

import lk.healthylife.hms.dto.SurgeryDTO;
import lk.healthylife.hms.response.SuccessResponse;
import lk.healthylife.hms.response.SuccessResponseHandler;
import lk.healthylife.hms.service.SurgeryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@CrossOrigin
@RestController
@RequestMapping("/api/surgery")
public class SurgeryController {

    private final SurgeryService surgeryService;

    public SurgeryController(SurgeryService surgeryService) {
        this.surgeryService = surgeryService;
    }

    @PostMapping
    public ResponseEntity<SuccessResponse> insertSurgery(@RequestBody SurgeryDTO surgeryDTO) {
        return SuccessResponseHandler.generateResponse(surgeryService.createSurgery(surgeryDTO));
    }

    @GetMapping("/{surgeryId}")
    public ResponseEntity<SuccessResponse> getSurgeryById(@PathVariable("surgeryId") Long surgeryId) {
        return SuccessResponseHandler.generateResponse(surgeryService.getSurgeryById(surgeryId));
    }

    @GetMapping(path = "/search")
    public ResponseEntity<SuccessResponse> searchSurgeriesPaginated(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "page", required = true) Integer page,
            @RequestParam(name = "size", required = true) Integer size) {
        return SuccessResponseHandler.generateResponse(surgeryService.surgeryPaginatedSearch(name, type, page, size));
    }

    @PutMapping("/{surgeryId}")
    public ResponseEntity<SuccessResponse> updateSurgery(@PathVariable("surgeryId") Long surgeryId,
                                                         @RequestBody SurgeryDTO surgeryDTO) throws IOException {
        return SuccessResponseHandler.generateResponse(surgeryService.updateSurgery(surgeryId, surgeryDTO));
    }

    @DeleteMapping("/{surgeryId}")
    public ResponseEntity<SuccessResponse> removeSurgery(@PathVariable("surgeryId") Long surgeryId) {
        return SuccessResponseHandler.generateResponse(surgeryService.removeSurgery(surgeryId));
    }
}

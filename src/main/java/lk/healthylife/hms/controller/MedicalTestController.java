package lk.healthylife.hms.controller;

import lk.healthylife.hms.dto.MedicalTestDTO;
import lk.healthylife.hms.response.SuccessResponse;
import lk.healthylife.hms.response.SuccessResponseHandler;
import lk.healthylife.hms.service.MedicalTestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@CrossOrigin
@RestController
@RequestMapping("/api/medicalTest")
public class MedicalTestController {

    private final MedicalTestService medicalTestService;

    public MedicalTestController(MedicalTestService medicalTestService) {
        this.medicalTestService = medicalTestService;
    }

    @PostMapping
    public ResponseEntity<SuccessResponse> insertMedicalTest(@RequestBody MedicalTestDTO medicalTestDTO) {
        return SuccessResponseHandler.generateResponse(medicalTestService.insertMedicalTest(medicalTestDTO));
    }

    @GetMapping("/{medicalTestId}")
    public ResponseEntity<SuccessResponse> getMedicalTestById(@PathVariable("medicalTestId") Long medicalTestId) {
        return SuccessResponseHandler.generateResponse(medicalTestService.getMedicalTestById(medicalTestId));
    }

    @GetMapping(path = "/search")
    public ResponseEntity<SuccessResponse> searchMedicalTestPaginated(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "page", required = true) Integer page,
            @RequestParam(name = "size", required = true) Integer size) {
        return SuccessResponseHandler.generateResponse(medicalTestService.medicalTestPaginatedSearch(name, type, page, size));
    }

    @PutMapping("/{facilityId}")
    public ResponseEntity<SuccessResponse> updateMedicalTest(@PathVariable("medicalTestId") Long medicalTestId,
                                                          @RequestBody MedicalTestDTO medicalTestDTO) throws IOException {
        return SuccessResponseHandler.generateResponse(medicalTestService.updateMedicalTest(medicalTestId, medicalTestDTO));
    }

    @DeleteMapping("/{facilityId}")
    public ResponseEntity<SuccessResponse> removeMedicalTest(@PathVariable("medicalTestId") Long medicalTestId) {
        return SuccessResponseHandler.generateResponse(medicalTestService.removeMedicalTest(medicalTestId));
    }
}

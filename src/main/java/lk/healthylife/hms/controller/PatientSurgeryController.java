package lk.healthylife.hms.controller;

import lk.healthylife.hms.dto.PatientSurgeryDTO;
import lk.healthylife.hms.response.SuccessResponse;
import lk.healthylife.hms.response.SuccessResponseHandler;
import lk.healthylife.hms.service.PatientSurgeryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@CrossOrigin
@RestController
@RequestMapping("/api/patient/surgery")
public class PatientSurgeryController {

    private final PatientSurgeryService patientSurgeryService;

    public PatientSurgeryController(PatientSurgeryService patientSurgeryService) {
        this.patientSurgeryService = patientSurgeryService;
    }

    @PostMapping
    public ResponseEntity<SuccessResponse> insertPatientSurgery(@RequestBody PatientSurgeryDTO patientSurgeryDTO) {
        return SuccessResponseHandler.generateResponse(patientSurgeryService.addPatientToSurgery(patientSurgeryDTO));
    }

    @PutMapping("/{patientSurgeryId}/finish")
    public ResponseEntity<SuccessResponse> finishPatientSurgery(@PathVariable("patientSurgeryId") Long patientSurgeryId) throws IOException {
        return SuccessResponseHandler.generateResponse(patientSurgeryService.finishPatientSurgery(patientSurgeryId));
    }

    @DeleteMapping("/{patientSurgeryId}")
    public ResponseEntity<SuccessResponse> removePatientSurgery(@PathVariable("patientSurgeryId") Long patientSurgeryId) {
        return SuccessResponseHandler.generateResponse(patientSurgeryService.removePatientSurgery(patientSurgeryId));
    }

    @GetMapping(path = "/search")
    public ResponseEntity<SuccessResponse> searchPatientSurgeriesPaginated(
            @RequestParam(name = "patientName", required = false) String patientName,
            @RequestParam(name = "doctorCode", required = false) String doctorCode,
            @RequestParam(name = "surgeryName", required = false) String surgeryName,
            @RequestParam(name = "page", required = true) Integer page,
            @RequestParam(name = "size", required = true) Integer size) {
        return SuccessResponseHandler.generateResponse(patientSurgeryService
                .patientSurgeryPaginatedSearch(patientName, doctorCode, surgeryName, page, size));
    }

    @GetMapping("/{patientSurgeryId}")
    public ResponseEntity<SuccessResponse> getPatientSurgeryById(@PathVariable("patientSurgeryId") Long patientSurgeryId) {
        return SuccessResponseHandler.generateResponse(patientSurgeryService.getPatientSurgeryById(patientSurgeryId));
    }
}

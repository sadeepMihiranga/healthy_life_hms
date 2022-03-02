package lk.healthylife.hms.controller;

import lk.healthylife.hms.dto.PatientMedicalTestDTO;
import lk.healthylife.hms.response.SuccessResponse;
import lk.healthylife.hms.response.SuccessResponseHandler;
import lk.healthylife.hms.service.PatientMedicalTestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@CrossOrigin
@RestController
@RequestMapping("/api/patient/medicalTest")
public class PatientMedicalTestController {

    private final PatientMedicalTestService patientMedicalTestService;

    public PatientMedicalTestController(PatientMedicalTestService patientMedicalTestService) {
        this.patientMedicalTestService = patientMedicalTestService;
    }

    @PostMapping
    public ResponseEntity<SuccessResponse> insertPatientMedicalTest(@RequestBody PatientMedicalTestDTO patientMedicalTestDTO) {
        return SuccessResponseHandler.generateResponse(patientMedicalTestService.insertPatientMedicalTest(patientMedicalTestDTO));
    }

    @GetMapping("/{patientMedicalTestId}")
    public ResponseEntity<SuccessResponse> getMedicalTestById(@PathVariable("patientMedicalTestId") Long patientMedicalTestId) {
        return SuccessResponseHandler.generateResponse(patientMedicalTestService.getPatientMedicalTestById(patientMedicalTestId));
    }

    @GetMapping(path = "/search")
    public ResponseEntity<SuccessResponse> searchMedicalTestPaginated(
            @RequestParam(name = "patientName", required = false) String patientName,
            @RequestParam(name = "testName", required = false) String testName,
            @RequestParam(name = "page", required = true) Integer page,
            @RequestParam(name = "size", required = true) Integer size) {
        return SuccessResponseHandler.generateResponse(patientMedicalTestService
                .patientMedicalTestPaginatedSearch(patientName, testName, page, size));
    }

    @PutMapping("/{patientMedicalTestId}")
    public ResponseEntity<SuccessResponse> updateMedicalTest(@PathVariable("patientMedicalTestId") Long patientMedicalTestId,
                                                             @RequestBody PatientMedicalTestDTO patientMedicalTestDTO) throws IOException {
        return SuccessResponseHandler.generateResponse(patientMedicalTestService.updatePatientMedicalTest(patientMedicalTestId, patientMedicalTestDTO));
    }

    @DeleteMapping("/{patientMedicalTestId}")
    public ResponseEntity<SuccessResponse> removeMedicalTest(@PathVariable("patientMedicalTestId") Long patientMedicalTestId) {
        return SuccessResponseHandler.generateResponse(patientMedicalTestService.removeMedicalTest(patientMedicalTestId));
    }

    @PutMapping("/{patientMedicalTestId}/approve")
    public ResponseEntity<SuccessResponse> approveMedicalTest(@PathVariable("patientMedicalTestId") Long patientMedicalTestId) {
        return SuccessResponseHandler.generateResponse(patientMedicalTestService.approveMedicalTest(patientMedicalTestId));
    }
}

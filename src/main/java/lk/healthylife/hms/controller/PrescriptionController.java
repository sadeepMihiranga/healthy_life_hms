package lk.healthylife.hms.controller;

import lk.healthylife.hms.dto.PrescriptionDTO;
import lk.healthylife.hms.response.SuccessResponse;
import lk.healthylife.hms.response.SuccessResponseHandler;
import lk.healthylife.hms.service.PrescriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/prescription")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @PostMapping
    public ResponseEntity<SuccessResponse> insertPrescription(@RequestBody PrescriptionDTO prescriptionDTO) {
        return SuccessResponseHandler.generateResponse(prescriptionService.createPrescription(prescriptionDTO));
    }

    @GetMapping("/{prescriptionId}")
    public ResponseEntity<SuccessResponse> getPrescriptionById(@PathVariable("prescriptionId") Long prescriptionId) {
        return SuccessResponseHandler.generateResponse(prescriptionService.getPrescriptionById(prescriptionId));
    }

    @GetMapping(path = "/search")
    public ResponseEntity<SuccessResponse> searchPrescriptionsPaginated(
            @RequestParam(name = "doctorCode", required = false) String doctorCode,
            @RequestParam(name = "patientName", required = false) String patientName,
            @RequestParam(name = "patientNic", required = false) String patientNic,
            @RequestParam(name = "page", required = true) Integer page,
            @RequestParam(name = "size", required = true) Integer size) {
        return SuccessResponseHandler.generateResponse(prescriptionService
                .prescriptionPaginatedSearch(doctorCode, patientName, patientNic, page, size));
    }

    @DeleteMapping("/{prescriptionId}")
    public ResponseEntity<SuccessResponse> removePrescription(@PathVariable("prescriptionId") Long prescriptionId) {
        return SuccessResponseHandler.generateResponse(prescriptionService.removePrescription(prescriptionId));
    }
}

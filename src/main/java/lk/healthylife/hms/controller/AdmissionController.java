package lk.healthylife.hms.controller;

import lk.healthylife.hms.dto.PatientAdmissionDTO;
import lk.healthylife.hms.response.SuccessResponse;
import lk.healthylife.hms.response.SuccessResponseHandler;
import lk.healthylife.hms.service.PatientAdmissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/admission")
public class AdmissionController {

    private final PatientAdmissionService patientAdmissionService;

    public AdmissionController(PatientAdmissionService patientAdmissionService) {
        this.patientAdmissionService = patientAdmissionService;
    }

    @PostMapping
    public ResponseEntity<SuccessResponse> admitPatient(@RequestBody PatientAdmissionDTO patientAdmissionDTO) {
        return SuccessResponseHandler.generateResponse(patientAdmissionService.admitPatient(patientAdmissionDTO));
    }

    @PostMapping("/{admissionId}/approve")
    public ResponseEntity<SuccessResponse> approveAdmission(@PathVariable("admissionId") Long admissionId) {
        return SuccessResponseHandler.generateResponse(patientAdmissionService.approveAdmission(admissionId));
    }

    @GetMapping("/{admissionId}")
    public ResponseEntity<SuccessResponse> getAdmissionById(@PathVariable("admissionId") Long admissionId) {
        return SuccessResponseHandler.generateResponse(patientAdmissionService.getAdmissionById(admissionId));
    }

    @GetMapping(path = "/search")
    public ResponseEntity<SuccessResponse> searchAdmissionsPaginated(@RequestParam(name = "patientName", required = false) String patientName,
                                                                @RequestParam(name = "roomNo", required = false) String roomNo,
                                                                @RequestParam(name = "page", required = true) Integer page,
                                                                @RequestParam(name = "size", required = true) Integer size) {
        return SuccessResponseHandler.generateResponse(patientAdmissionService.admissionPaginatedSearch(patientName, roomNo, page, size));
    }

    @DeleteMapping("/{admissionId}")
    public ResponseEntity<SuccessResponse> removeAdmission(@PathVariable("admissionId") Long admissionId) {
        return SuccessResponseHandler.generateResponse(patientAdmissionService.removeAdmission(admissionId));
    }

    @PostMapping("/{admissionId}/discharge")
    public ResponseEntity<SuccessResponse> dischargePatient(@PathVariable("admissionId") Long admissionId) {
        return SuccessResponseHandler.generateResponse(patientAdmissionService.dischargePatient(admissionId));
    }
}

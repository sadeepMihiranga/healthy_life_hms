package lk.healthylife.hms.controller;

import lk.healthylife.hms.dto.PatientConditionDTO;
import lk.healthylife.hms.response.SuccessResponse;
import lk.healthylife.hms.response.SuccessResponseHandler;
import lk.healthylife.hms.service.PatientConditionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/patientCondition")
public class PatientConditionController {

    private final PatientConditionService patientConditionService;

    public PatientConditionController(PatientConditionService patientConditionService) {
        this.patientConditionService = patientConditionService;
    }

    @PostMapping
    public ResponseEntity<SuccessResponse> insertPatientCondition(@RequestBody PatientConditionDTO patientConditionDTO) {
        return SuccessResponseHandler.generateResponse(patientConditionService.addPatientCondition(patientConditionDTO));
    }
}

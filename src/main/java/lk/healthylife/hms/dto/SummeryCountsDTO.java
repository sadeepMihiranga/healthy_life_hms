package lk.healthylife.hms.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class SummeryCountsDTO {

    private LocalDateTime from;
    private LocalDateTime to;
    private Map<String, String> patientAdmissionCount;
    private Map<String, String> prescriptionCount;
    private Map<String, String> surgeryCount;
    private Map<String, String> medicalTestCount;
}

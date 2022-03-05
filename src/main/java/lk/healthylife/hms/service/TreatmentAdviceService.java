package lk.healthylife.hms.service;

import lk.healthylife.hms.dto.TreatmentAdviceDTO;

import java.util.List;

public interface TreatmentAdviceService {

    TreatmentAdviceDTO addTreatmentAdvice(Long admissionId, TreatmentAdviceDTO treatmentAdviceDTO);

    TreatmentAdviceDTO getTreatmentAdviceById(Long adviceId);

    Boolean removeTreatmentAdvicesByAdmission(Long admissionId);

    Boolean removeTreatmentAdvice(Long adviceId);

    List<TreatmentAdviceDTO> getAdvicesByAdmissionId(Long adviceId);
}

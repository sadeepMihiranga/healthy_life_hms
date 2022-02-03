package lk.healthylife.hms.service;

import lk.healthylife.hms.dto.CommonReferenceDTO;

import java.util.List;

public interface CommonReferenceService {

    List<CommonReferenceDTO> getAllByCmrtCode(String cmrtCode);

    CommonReferenceDTO getByCmrfCodeAndCmrtCode(String cmrtCode, String cmrfCode);
}

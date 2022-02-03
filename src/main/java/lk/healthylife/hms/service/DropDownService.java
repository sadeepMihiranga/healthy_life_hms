package lk.healthylife.hms.service;

import lk.healthylife.hms.dto.DropDownDTO;

import java.util.List;
import java.util.Map;

public interface DropDownService {

    List<DropDownDTO> getDropDownByCode(String code);

    Map<String, String> getDropDownCodes();
}

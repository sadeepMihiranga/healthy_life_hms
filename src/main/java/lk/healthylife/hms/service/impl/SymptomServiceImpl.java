package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.config.EntityValidator;
import lk.healthylife.hms.dto.SymptomDTO;
import lk.healthylife.hms.exception.NoRequiredInfoException;
import lk.healthylife.hms.service.SymptomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SymptomServiceImpl extends EntityValidator implements SymptomService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<SymptomDTO> getAllSymptomsDropdown() {

        List<SymptomDTO> symptomDTOList = new ArrayList<>();

        final String queryString = "SELECT SYMP_ID, SYMP_NAME FROM T_MS_SYMPTOM";

        Query query = entityManager.createNativeQuery(queryString);

        List<Map<String,Object>> result = extractResultSet(query);

        if(result.size() == 0)
            return Collections.emptyList();

        for (Map<String,Object> department : result) {

            SymptomDTO symptomDTO = new SymptomDTO();

            symptomDTO.setSymptomId(extractLongValue(String.valueOf(department.get("SYMP_ID"))));
            symptomDTO.setName(extractValue(String.valueOf(department.get("SYMP_NAME"))));

            symptomDTOList.add(symptomDTO);
        }

        return symptomDTOList;
    }

    @Override
    public SymptomDTO getSymptomById(Long symptomId) {

        SymptomDTO symptomDTO = null;

       if(symptomId == null)
           throw new NoRequiredInfoException("Symptom Id is Required");

        final String queryString = "SELECT s.SYMP_ID, s.SYMP_NAME, s.SYMP_DESCRIPTION, s.SYMP_TYPE, s.CREATED_DATE, \n" +
                "s.LAST_MOD_DATE, s.CREATED_USER_CODE, s.LAST_MOD_USER_CODE, type.CMRF_DESCRIPTION AS TYPE_NAME\n" +
                "FROM T_MS_SYMPTOM s\n" +
                "LEFT JOIN T_RF_COMMON_REFERENCE type ON s.SYMP_TYPE = type.CMRF_CODE\n" +
                "WHERE s.SYMP_ID = :symptomId";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("symptomId", symptomId);

        List<Map<String,Object>> result = extractResultSet(query);

        if (result.size() == 0)
            return null;

        for (Map<String,Object> symptom : result) {

            symptomDTO = new SymptomDTO();

            createDTO(symptomDTO, symptom);
        }

        return symptomDTO;
    }

    private void createDTO(SymptomDTO symptomDTO, Map<String,Object> symptom) {

        symptomDTO.setSymptomId(extractLongValue(String.valueOf(symptom.get("SYMP_ID"))));
        symptomDTO.setName(extractValue(String.valueOf(symptom.get("SYMP_NAME"))));
        symptomDTO.setType(extractValue(String.valueOf(symptom.get("SYMP_TYPE"))));
        symptomDTO.setTypeName(extractValue(String.valueOf(symptom.get("TYPE_NAME"))));
        symptomDTO.setDescription(extractValue(String.valueOf(symptom.get("SYMP_DESCRIPTION"))));
        symptomDTO.setCreatedDate(extractDateTime(String.valueOf(symptom.get("CREATED_DATE"))));
        symptomDTO.setCreatedUserCode(extractValue(String.valueOf(symptom.get("CREATED_USER_CODE"))));
        symptomDTO.setLastUpdatedDate(extractDateTime(String.valueOf(symptom.get("LAST_MOD_DATE"))));
        symptomDTO.setLastUpdatedUserCode(extractValue(String.valueOf(symptom.get("LAST_MOD_USER_CODE"))));
    }
}

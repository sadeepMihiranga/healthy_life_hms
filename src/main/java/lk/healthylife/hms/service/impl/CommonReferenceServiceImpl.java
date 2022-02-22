package lk.healthylife.hms.service.impl;

import com.google.common.base.Strings;
import lk.healthylife.hms.config.EntityValidator;
import lk.healthylife.hms.dto.CommonReferenceDTO;
import lk.healthylife.hms.entity.TRfCommonReference;
import lk.healthylife.hms.exception.DataNotFoundException;
import lk.healthylife.hms.exception.NoRequiredInfoException;
import lk.healthylife.hms.mapper.CommonReferenceMapper;
import lk.healthylife.hms.config.repository.CommonReferenceRepository;
import lk.healthylife.hms.service.CommonReferenceService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.internal.NativeQueryImpl;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static lk.healthylife.hms.util.constant.Constants.STATUS_ACTIVE;

@Slf4j
@Service
public class CommonReferenceServiceImpl extends EntityValidator implements CommonReferenceService {

    private final CommonReferenceRepository commonReferenceRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public CommonReferenceServiceImpl(CommonReferenceRepository commonReferenceRepository) {
        this.commonReferenceRepository = commonReferenceRepository;
    }

    @Override
    public List<CommonReferenceDTO> getAllByCmrtCode(String cmrtCode) {

        if(Strings.isNullOrEmpty(cmrtCode))
            throw new NoRequiredInfoException("Cmrt Code is required");

        final List<TRfCommonReference> tRfCommonReferenceList = commonReferenceRepository
                .findAllByReferenceTypeCmrtCodeAndCmrfStatus(cmrtCode, STATUS_ACTIVE.getShortValue());

        if(tRfCommonReferenceList.isEmpty() || tRfCommonReferenceList == null)
            return Collections.emptyList();

        List<CommonReferenceDTO> commonReferenceDTOList = new ArrayList<>();

        tRfCommonReferenceList.forEach(tRfCommonReference -> {
            commonReferenceDTOList.add(CommonReferenceMapper.INSTANCE.entityToDTO(tRfCommonReference));
        });

        return commonReferenceDTOList;
    }

    @Override
    public CommonReferenceDTO getByCmrfCodeAndCmrtCode(String cmrtCode, String cmrfCode) {

        CommonReferenceDTO commonReferenceDTO = null;

        if(Strings.isNullOrEmpty(cmrfCode))
            throw new NoRequiredInfoException("Cmrf Code is required");

        if(Strings.isNullOrEmpty(cmrtCode))
            throw new NoRequiredInfoException("Cmrt Code is required");

        String queryString = "SELECT CMRF_CODE, CMRF_CMRT_CODE, CMRF_DESCRIPTION, CMRF_STATUS, CMRF_STRING_VALUE, CMRF_NUMBER_VALUES\n" +
                "FROM T_RF_COMMON_REFERENCE\n" +
                "WHERE CMRF_CMRT_CODE = :cmrtCode AND CMRF_CODE = :cmrfCode AND CMRF_STATUS = :status";

        Query query = entityManager.createNativeQuery(queryString);

        query.setParameter("status", STATUS_ACTIVE.getShortValue());
        query.setParameter("cmrtCode", cmrtCode);
        query.setParameter("cmrfCode", cmrfCode);

        NativeQueryImpl nativeQuery = (NativeQueryImpl) query;
        nativeQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
        List<Map<String,Object>> result = nativeQuery.getResultList();

        if(result.size() == 0)
            throw new DataNotFoundException("Reference not found for Cmrf Code : " + cmrfCode + " and Cmrt Code : " + cmrtCode);

        for (Map<String,Object> party : result) {

            commonReferenceDTO = new CommonReferenceDTO();

            commonReferenceDTO.setCmrfCode(extractValue(String.valueOf(party.get("CMRF_CODE"))));
            commonReferenceDTO.setReferenceType(extractValue(String.valueOf(party.get("CMRF_CMRT_CODE"))));
            commonReferenceDTO.setDescription(extractValue(String.valueOf(party.get("CMRF_DESCRIPTION"))));
            commonReferenceDTO.setStatus(extractShortValue(String.valueOf(party.get("CMRF_STATUS"))));
            commonReferenceDTO.setNumberValue(extractIntegerValue(String.valueOf(party.get("CMRF_NUMBER_VALUES"))));
            commonReferenceDTO.setStringValue(extractValue(String.valueOf(party.get("CMRF_STRING_VALUE"))));
        }

        return commonReferenceDTO;
    }
}

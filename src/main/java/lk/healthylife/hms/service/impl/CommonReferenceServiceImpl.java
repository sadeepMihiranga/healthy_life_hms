package lk.healthylife.hms.service.impl;

import com.google.common.base.Strings;
import lk.healthylife.hms.dto.CommonReferenceDTO;
import lk.healthylife.hms.entity.TRfCommonReference;
import lk.healthylife.hms.exception.DataNotFoundException;
import lk.healthylife.hms.exception.NoRequiredInfoException;
import lk.healthylife.hms.mapper.CommonReferenceMapper;
import lk.healthylife.hms.repository.CommonReferenceRepository;
import lk.healthylife.hms.service.CommonReferenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static lk.healthylife.hms.util.constant.Constants.STATUS_ACTIVE;

@Slf4j
@Service
public class CommonReferenceServiceImpl implements CommonReferenceService {

    private final CommonReferenceRepository commonReferenceRepository;

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

        if(Strings.isNullOrEmpty(cmrfCode))
            throw new NoRequiredInfoException("Cmrf Code is required");

        if(Strings.isNullOrEmpty(cmrtCode))
            throw new NoRequiredInfoException("Cmrt Code is required");

        final TRfCommonReference tRfCommonReference = commonReferenceRepository
                .findByCmrtCodeAndCmrfCode(cmrtCode, cmrfCode, STATUS_ACTIVE.getShortValue());

        if(tRfCommonReference == null)
            throw new DataNotFoundException("Reference not found for Cmrf Code : " + cmrfCode + " and Cmrt Code : " + cmrtCode);

        return CommonReferenceMapper.INSTANCE.entityToDTO(tRfCommonReference);
    }
}

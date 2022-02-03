package lk.healthylife.hms.service.impl;

import lk.healthylife.hms.dto.BranchDTO;
import lk.healthylife.hms.entity.TRfBranch;
import lk.healthylife.hms.mapper.BranchMapper;
import lk.healthylife.hms.repository.BranchRepository;
import lk.healthylife.hms.service.BranchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static lk.healthylife.hms.util.constant.Constants.STATUS_ACTIVE;

@Slf4j
@Service
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;

    public BranchServiceImpl(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    @Override
    public List<BranchDTO> getAllBranches() {

        final List<TRfBranch> tRfBranchList = branchRepository.findAllByBrnhStatus(STATUS_ACTIVE.getShortValue());

        if(tRfBranchList.isEmpty() || tRfBranchList == null)
            return Collections.emptyList();

        List<BranchDTO> branchDTOList = new ArrayList<>();

        tRfBranchList.forEach(tRfBranch -> {
            branchDTOList.add(BranchMapper.INSTANCE.entityToDTO(tRfBranch));
        });

        return branchDTOList;
    }
}

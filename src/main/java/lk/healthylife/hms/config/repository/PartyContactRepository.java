package lk.healthylife.hms.config.repository;

import lk.healthylife.hms.entity.TMsPartyContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartyContactRepository extends JpaRepository<TMsPartyContact, Long> {

    List<TMsPartyContact> findAllByParty_PrtyCodeAndPtcnStatus(String prtyCode, Short ptcnStatus);

    TMsPartyContact findAllByParty_PrtyCodeAndPtcnContactTypeAndPtcnStatus(String prtyCode, String ptcnContactType, Short ptcnStatus);

    TMsPartyContact findByPtcnIdAndPtcnStatus(Long ptcnId, Short ptcnStatus);
}


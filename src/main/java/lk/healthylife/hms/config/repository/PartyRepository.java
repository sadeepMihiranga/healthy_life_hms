package lk.healthylife.hms.config.repository;

import lk.healthylife.hms.entity.TMsParty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PartyRepository extends JpaRepository<TMsParty, Long> {

    @Query("SELECT t FROM TMsParty t " +
            "WHERE t.prtyStatus = :prtyStatus " +
            "AND (:prtyType IS NULL OR (:prtyType IS NOT NULL AND t.prtyType = :prtyType)) " +
            "AND UPPER(t.prtyName) LIKE CONCAT('%', UPPER(:prtyName), '%') " +
            "AND t.branch.brnhId IN :branchIdList " +
            "ORDER BY t.lastModDate")
    Page<TMsParty> getActiveParties(@Param("prtyName") String name,
                                    @Param("prtyStatus") Short status,
                                    @Param("prtyType") String partyType,
                                    @Param("branchIdList") List<Long> branchIdList,
                                    Pageable pageable);

    TMsParty findByPrtyCodeAndPrtyStatus(String prtyCode, Short prtyStatus);

    List<TMsParty> findAllByBranch_BrnhIdInAndPrtyStatusAndPrtyType(List<Long> brnhIdList, Short prtyStatus, String prtyType);
}

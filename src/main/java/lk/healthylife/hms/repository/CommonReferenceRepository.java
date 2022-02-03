package lk.healthylife.hms.repository;

import lk.healthylife.hms.entity.TRfCommonReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommonReferenceRepository extends JpaRepository<TRfCommonReference, String> {

    List<TRfCommonReference> findAllByReferenceTypeCmrtCodeAndCmrfStatus(String cmrtCode, Short cmrfStatus);

    @Query("SELECT t FROM TRfCommonReference t " +
            "WHERE t.cmrfStatus = :cmrfStatus " +
            "AND t.cmrfCode = :cmrfCode " +
            "AND t.referenceType.cmrtCode = :cmrtCode ")
    TRfCommonReference findByCmrtCodeAndCmrfCode(@Param("cmrtCode") String cmrtCode,
                                                  @Param("cmrfCode") String cmrfCode,
                                                  @Param("cmrfStatus") Short status);
}

package lk.healthylife.hms.config.repository;

import lk.healthylife.hms.entity.TMsUserBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserBranchRepository extends JpaRepository<TMsUserBranch, String> {

    List<TMsUserBranch> findAllByUser_UserIdAndUsbrStatus(Long userId, Short usbrStatus);

    @Modifying
    @Query("UPDATE TMsUserBranch t SET t.usbrStatus = :status WHERE t.user.userId = :userId")
    Integer inactiveByUserId(@Param("userId") Long userId, @Param("status") Short status);

    TMsUserBranch findAllByUser_UserIdAndBranch_BrnhId(Long userId, Long brnhId);
}

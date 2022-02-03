package lk.healthylife.hms.repository;

import lk.healthylife.hms.entity.TMsUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<TMsUserRole, Long> {

    List<TMsUserRole> findAllByUser_UserIdAndUsrlStatus(Long userId, Short usrlStatus);

    TMsUserRole findByUser_UserIdAndRole_RoleIdAndUsrlStatus(Long userId, Long roleId, Short usrlStatus);

    TMsUserRole findByUser_UserIdAndRole_RoleId(Long userId, Long roleId);

    TMsUserRole findByUser_UserIdAndRole_RoleName(Long userId, String roleName);

    @Modifying
    @Query("UPDATE TMsUserRole t SET t.usrlStatus = :status WHERE t.user.userId = :userId")
    Integer inactiveByUserId(@Param("userId") Long userId, @Param("status") Short status);
}

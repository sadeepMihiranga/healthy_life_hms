package lk.healthylife.hms.repository;

import lk.healthylife.hms.entity.TMsRoleFunction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleFunctionRepository extends JpaRepository<TMsRoleFunction, Long> {

    List<TMsRoleFunction> findAllByRoleRoleId(Long roleId);
}

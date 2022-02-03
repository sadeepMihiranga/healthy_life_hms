package lk.healthylife.hms.repository;

import lk.healthylife.hms.entity.TMsRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<TMsRole, Long> {

    TMsRole findByRoleNameAndRoleStatus(String roleName, Short roleStatus);
}

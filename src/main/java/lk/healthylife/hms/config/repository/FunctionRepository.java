package lk.healthylife.hms.config.repository;

import lk.healthylife.hms.entity.TMsFunction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FunctionRepository extends JpaRepository<TMsFunction, String> {
}

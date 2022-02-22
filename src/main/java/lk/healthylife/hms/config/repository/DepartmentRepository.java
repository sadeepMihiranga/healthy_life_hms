package lk.healthylife.hms.config.repository;

import lk.healthylife.hms.entity.TMsDepartment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<TMsDepartment, String> {

    TMsDepartment findByDpmtCodeAndDpmtStatus(String dpmtCode, Short dpmtStatus);

    List<TMsDepartment> findAllByDpmtStatus(Short dpmtStatus);
}

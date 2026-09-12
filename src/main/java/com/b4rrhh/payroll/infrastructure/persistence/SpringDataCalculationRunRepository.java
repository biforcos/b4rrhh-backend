package com.b4rrhh.payroll.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SpringDataCalculationRunRepository extends JpaRepository<CalculationRunEntity, Long> {

	List<CalculationRunEntity> findByStatusIn(Collection<String> statuses);
}
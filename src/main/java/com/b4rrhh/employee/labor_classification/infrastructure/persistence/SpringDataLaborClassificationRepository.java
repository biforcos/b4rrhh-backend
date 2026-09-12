package com.b4rrhh.employee.labor_classification.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpringDataLaborClassificationRepository extends JpaRepository<LaborClassificationEntity, Long> {

    Optional<LaborClassificationEntity> findByEmployeeIdAndStartDate(Long employeeId, LocalDate startDate);

    List<LaborClassificationEntity> findByEmployeeIdOrderByStartDateAsc(Long employeeId);
}

package com.b4rrhh.employee.labor_classification.domain.port;

import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LaborClassificationRepository {

    Optional<LaborClassification> findByEmployeeIdAndStartDate(Long employeeId, LocalDate startDate);

    List<LaborClassification> findByEmployeeIdOrderByStartDate(Long employeeId);

    boolean existsOverlappingPeriod(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate excludeStartDateOrNull
    );

    void save(LaborClassification laborClassification);

    void update(LaborClassification laborClassification, LocalDate originalStartDate);

    /** Removes the labor classification by its functional identity: employee and start date. */
    void delete(LaborClassification laborClassification);
}

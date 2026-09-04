package com.b4rrhh.employee.working_time.domain.port;

import com.b4rrhh.employee.working_time.domain.model.WorkingTime;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkingTimeRepository {

    Optional<WorkingTime> findByEmployeeIdAndWorkingTimeNumber(Long employeeId, Integer workingTimeNumber);

    List<WorkingTime> findByEmployeeIdOrderByStartDate(Long employeeId);

    boolean existsOverlappingPeriod(Long employeeId, LocalDate startDate, LocalDate endDate);

    boolean existsOverlappingPeriodExcluding(Long employeeId, LocalDate startDate, LocalDate endDate, Integer excludeWorkingTimeNumber);

    Optional<Integer> findMaxWorkingTimeNumberByEmployeeId(Long employeeId);

    WorkingTime save(WorkingTime workingTime);

    void delete(WorkingTime workingTime);
}
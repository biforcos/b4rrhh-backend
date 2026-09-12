package com.b4rrhh.employee.working_time.domain.port;

import com.b4rrhh.employee.working_time.domain.model.WorkingTime;

import java.util.List;
import java.util.Optional;

public interface WorkingTimeRepository {

    Optional<WorkingTime> findByEmployeeIdAndWorkingTimeNumber(Long employeeId, Integer workingTimeNumber);

    List<WorkingTime> findByEmployeeIdOrderByStartDate(Long employeeId);

    Optional<Integer> findMaxWorkingTimeNumberByEmployeeId(Long employeeId);

    WorkingTime save(WorkingTime workingTime);

    void delete(WorkingTime workingTime);
}
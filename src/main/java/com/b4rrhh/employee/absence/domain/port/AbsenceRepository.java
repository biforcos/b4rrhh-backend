package com.b4rrhh.employee.absence.domain.port;

import com.b4rrhh.employee.absence.domain.model.Absence;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AbsenceRepository {
    Optional<Absence> findByKey(Long employeeId, String absenceTypeCode,
                                LocalDate startDate, int startTime);
    List<Absence> findByEmployeeIdOrderByStartDateDescStartTimeDesc(Long employeeId);
    boolean existsOverlappingAbsence(Long employeeId, LocalDate startDate, LocalDate endDate);
    boolean existsOverlappingAbsenceExcluding(Long employeeId, LocalDate startDate,
                                               LocalDate endDate, Long excludeId);
    Absence save(Absence absence);
    void deleteByKey(Long employeeId, String absenceTypeCode, LocalDate startDate, int startTime);
}

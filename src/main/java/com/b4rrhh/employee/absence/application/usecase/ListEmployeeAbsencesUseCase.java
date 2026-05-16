package com.b4rrhh.employee.absence.application.usecase;

import com.b4rrhh.employee.absence.domain.model.Absence;
import java.util.List;

public interface ListEmployeeAbsencesUseCase {
    List<Absence> listByEmployeeBusinessKey(ListEmployeeAbsencesCommand command);
}

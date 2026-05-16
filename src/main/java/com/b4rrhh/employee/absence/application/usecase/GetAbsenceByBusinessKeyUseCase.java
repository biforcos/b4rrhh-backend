package com.b4rrhh.employee.absence.application.usecase;

import com.b4rrhh.employee.absence.domain.model.Absence;

public interface GetAbsenceByBusinessKeyUseCase {
    Absence getByBusinessKey(GetAbsenceByBusinessKeyCommand command);
}

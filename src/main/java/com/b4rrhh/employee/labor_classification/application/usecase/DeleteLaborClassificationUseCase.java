package com.b4rrhh.employee.labor_classification.application.usecase;

import com.b4rrhh.employee.labor_classification.application.command.DeleteLaborClassificationCommand;

public interface DeleteLaborClassificationUseCase {

    void delete(DeleteLaborClassificationCommand command);
}

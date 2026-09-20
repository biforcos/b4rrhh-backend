package com.b4rrhh.payroll_engine.concept.application.usecase;

import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;

public interface UpdateConceptLabelUseCase {

    /**
     * Pone o cambia el nombre del concepto y devuelve el concepto al que se le puso.
     *
     * @throws com.b4rrhh.payroll_engine.concept.domain.exception.PayrollConceptNotFoundException
     *         si el concepto no existe.
     */
    PayrollConcept update(UpdateConceptLabelCommand command);
}

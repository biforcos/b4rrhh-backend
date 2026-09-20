package com.b4rrhh.payroll_engine.concept.application.usecase;

import com.b4rrhh.payroll_engine.concept.domain.model.PayslipSection;

import java.util.List;

public interface ListPayslipSectionsUseCase {

    /** Los bloques del recibo del sistema de reglas, en el orden en el que se imprimen. */
    List<PayslipSection> listByRuleSystemCode(String ruleSystemCode);
}

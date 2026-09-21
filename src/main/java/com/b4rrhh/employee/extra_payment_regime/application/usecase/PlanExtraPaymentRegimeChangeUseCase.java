package com.b4rrhh.employee.extra_payment_regime.application.usecase;

import com.b4rrhh.employee.extra_payment_regime.application.model.ExtraPaymentRegimePlan;

public interface PlanExtraPaymentRegimeChangeUseCase {

    ExtraPaymentRegimePlan plan(PlanExtraPaymentRegimeChangeCommand command);
}

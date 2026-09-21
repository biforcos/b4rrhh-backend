package com.b4rrhh.employee.extra_payment_regime.application.usecase;

import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegime;

public interface CreateExtraPaymentRegimeUseCase {

    ExtraPaymentRegime create(CreateExtraPaymentRegimeCommand command);
}
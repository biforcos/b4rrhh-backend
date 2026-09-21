package com.b4rrhh.employee.extra_payment_regime.application.usecase;

import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegime;

public interface CloseExtraPaymentRegimeUseCase {

    ExtraPaymentRegime close(CloseExtraPaymentRegimeCommand command);
}
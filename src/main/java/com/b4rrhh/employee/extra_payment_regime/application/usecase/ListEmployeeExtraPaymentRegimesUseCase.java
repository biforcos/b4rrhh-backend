package com.b4rrhh.employee.extra_payment_regime.application.usecase;

import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegime;

import java.util.List;

public interface ListEmployeeExtraPaymentRegimesUseCase {

    List<ExtraPaymentRegime> listByEmployeeBusinessKey(ListEmployeeExtraPaymentRegimesCommand command);
}
package com.b4rrhh.employee.extra_payment_regime.domain.port;

import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegime;

import java.util.List;
import java.util.Optional;

public interface ExtraPaymentRegimeRepository {

    Optional<ExtraPaymentRegime> findByEmployeeIdAndExtraPaymentRegimeNumber(Long employeeId, Integer extraPaymentRegimeNumber);

    List<ExtraPaymentRegime> findByEmployeeIdOrderByStartDate(Long employeeId);

    Optional<Integer> findMaxExtraPaymentRegimeNumberByEmployeeId(Long employeeId);

    ExtraPaymentRegime save(ExtraPaymentRegime extraPaymentRegime);

    void delete(ExtraPaymentRegime extraPaymentRegime);
}
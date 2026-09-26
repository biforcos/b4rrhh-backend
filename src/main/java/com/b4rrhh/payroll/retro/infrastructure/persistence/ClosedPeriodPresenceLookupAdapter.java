package com.b4rrhh.payroll.retro.infrastructure.persistence;

import com.b4rrhh.payroll.infrastructure.persistence.SpringDataPayrollRepository;
import com.b4rrhh.payroll.retro.application.port.ClosedPeriodPresenceLookupPort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pregunta por los recibos entregados de un periodo, y nada mas ({@code backend#130}).
 *
 * <p>Delega en el repositorio del vertical de recibos: el filtro por {@code DEFINITIVE} esta escrito
 * alli, en la consulta, que es donde el ADR-069 §2 dice que tiene que estar.
 */
@Component
public class ClosedPeriodPresenceLookupAdapter implements ClosedPeriodPresenceLookupPort {

    private final SpringDataPayrollRepository payrollRepository;

    public ClosedPeriodPresenceLookupAdapter(SpringDataPayrollRepository payrollRepository) {
        this.payrollRepository = payrollRepository;
    }

    @Override
    public List<Integer> findPresencesWithDefinitivePayroll(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String payrollPeriodCode,
            String payrollTypeCode
    ) {
        return payrollRepository.findPresenceNumbersWithDefinitivePayroll(
                ruleSystemCode, employeeTypeCode, employeeNumber, payrollPeriodCode, payrollTypeCode);
    }
}

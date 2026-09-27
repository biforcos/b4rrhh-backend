package com.b4rrhh.payroll.retro.infrastructure.persistence;

import com.b4rrhh.payroll.infrastructure.persistence.SpringDataPayrollRepository;
import com.b4rrhh.payroll.retro.application.port.CeasedPresenceWithoutReceiptsLookupPort;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Delega en el repositorio del vertical de recibos, que es el unico que consulta la tabla de recibos
 * ({@code OnlyOnePortReadsAPayrollOfAnotherPeriodTest}), con el filtro por {@code DEFINITIVE} escrito en
 * la consulta.
 */
@Component
public class CeasedPresenceWithoutReceiptsLookupAdapter implements CeasedPresenceWithoutReceiptsLookupPort {

    private final SpringDataPayrollRepository payrollRepository;

    public CeasedPresenceWithoutReceiptsLookupAdapter(SpringDataPayrollRepository payrollRepository) {
        this.payrollRepository = payrollRepository;
    }

    @Override
    public Set<Integer> findPresenceNumbers(String ruleSystemCode, String employeeTypeCode, String employeeNumber) {
        Set<Integer> presencias = new HashSet<>();
        for (Number n : payrollRepository.findCeasedPresencesWhoseLastMonthIsClosed(
                ruleSystemCode, employeeTypeCode, employeeNumber)) {
            presencias.add(n.intValue());
        }
        return presencias;
    }
}

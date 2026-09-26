package com.b4rrhh.payroll.retro.infrastructure.persistence;

import com.b4rrhh.payroll.infrastructure.persistence.SpringDataPayrollRepository;
import com.b4rrhh.payroll.retro.application.port.PaidForPeriod;
import com.b4rrhh.payroll.retro.application.port.PaidForPeriodLookupPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Suma lo pagado por un mes: su recibo cerrado mas los atrasos de ese mes ya pagados
 * ({@code backend#133}).
 *
 * <p>Delega en el repositorio del vertical de recibos, donde el filtro por {@code DEFINITIVE} esta
 * escrito en la consulta, que es donde el ADR-069 §2 dice que tiene que estar.
 */
@Component
public class PaidForPeriodLookupAdapter implements PaidForPeriodLookupPort {

    private final SpringDataPayrollRepository payrollRepository;

    public PaidForPeriodLookupAdapter(SpringDataPayrollRepository payrollRepository) {
        this.payrollRepository = payrollRepository;
    }

    @Override
    public PaidForPeriod findByEmployeeAndPeriod(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String payrollTypeCode,
            String periodCode
    ) {
        List<Object[]> filas = payrollRepository.sumPaidByConceptForPeriod(
                ruleSystemCode, employeeTypeCode, employeeNumber, payrollTypeCode, periodCode);

        Map<String, BigDecimal> porConcepto = new LinkedHashMap<>();
        for (Object[] fila : filas) {
            porConcepto.put((String) fila[0], (BigDecimal) fila[1]);
        }
        return new PaidForPeriod(porConcepto);
    }
}

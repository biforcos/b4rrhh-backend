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
        List<Object[]> filas = payrollRepository.findPaidLinesForPeriod(
                ruleSystemCode, employeeTypeCode, employeeNumber, payrollTypeCode, periodCode);

        Map<String, BigDecimal> porConcepto = new LinkedHashMap<>();
        Map<String, PaidForPeriod.PaidLine> identidad = new LinkedHashMap<>();
        Map<String, String> reciboDeLaIdentidad = new LinkedHashMap<>();
        for (Object[] fila : filas) {
            String concepto = (String) fila[0];
            porConcepto.merge(concepto, (BigDecimal) fila[1], BigDecimal::add);

            // Las filas vienen por periodo del recibo, de antes a despues. Se queda la del propio mes
            // si la hay y, si no, la ultima: la del recibo propio es la que el empleado tiene como suya.
            String recibo = (String) fila[8];
            String yaElegida = reciboDeLaIdentidad.get(concepto);
            if (yaElegida == null || !periodCode.equals(yaElegida)) {
                identidad.put(concepto, new PaidForPeriod.PaidLine(
                        (String) fila[2], (String) fila[3], (String) fila[4],
                        (Integer) fila[5], (String) fila[6], (String) fila[7]));
                reciboDeLaIdentidad.put(concepto, recibo);
            }
        }
        return new PaidForPeriod(porConcepto, identidad);
    }
}

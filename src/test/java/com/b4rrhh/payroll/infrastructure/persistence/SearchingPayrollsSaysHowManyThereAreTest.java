package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * La búsqueda de recibos pagina y dice cuántos hay (`b4rrhh/frontend#93`).
 *
 * <p>Antes cortaba en 500 sin decirlo: con 7.908 recibos en la semilla de nueve meses, la pantalla
 * enseñaba «500 nóminas encontradas», que es falso. Y ordenaba por hora de cálculo, así que la lista
 * de un empleado salía en el orden en que se habían calculado sus meses, no con el abierto primero.
 *
 * <p>Lo que se sujeta aquí, contra el esquema real:
 * <ol>
 *   <li>el total cuenta todos los que cumplen el filtro, no los de la página;</li>
 *   <li>el orden es el período más reciente primero y, dentro de un período, lo que no está cerrado
 *       antes que lo cerrado. Con eso, el primer recibo de la búsqueda sin filtros es del período
 *       abierto, que es con lo que la pantalla abre.</li>
 * </ol>
 */
@TestSobreEsquemaReal
class SearchingPayrollsSaysHowManyThereAreTest {

    @Autowired
    private SpringDataPayrollRepository repository;

    @Test
    void theTotalCountsEveryMatchAndTheOpenPeriodComesFirst() {
        // Un mes cerrado y el abierto después, calculados en el orden inverso para que el orden por
        // hora de cálculo no coincida por casualidad con el que se pide.
        save("EMP001", "202609", PayrollStatus.CALCULATED, "2026-09-01T10:00:00Z");
        save("EMP002", "202609", PayrollStatus.DEFINITIVE, "2026-09-02T10:00:00Z");
        save("EMP001", "202608", PayrollStatus.DEFINITIVE, "2026-09-03T10:00:00Z");
        save("EMP002", "202608", PayrollStatus.DEFINITIVE, "2026-09-04T10:00:00Z");
        save("EMP003", "202607", PayrollStatus.DEFINITIVE, "2026-09-05T10:00:00Z");

        Page<PayrollEntity> first = repository.findPageByFilters(null, null, null, null, PageRequest.of(0, 2));
        Page<PayrollEntity> second = repository.findPageByFilters(null, null, null, null, PageRequest.of(1, 2));

        assertEquals(5, first.getTotalElements());
        assertEquals(List.of("EMP001/202609/CALCULATED", "EMP002/202609/DEFINITIVE"), keys(first));
        assertEquals(List.of("EMP001/202608/DEFINITIVE", "EMP002/202608/DEFINITIVE"), keys(second));
    }

    @Test
    void theListOfOneEmployeeHasTheOpenPeriodFirst() {
        save("EMP001", "202607", PayrollStatus.DEFINITIVE, "2026-09-10T10:00:00Z");
        save("EMP001", "202609", PayrollStatus.CALCULATED, "2026-09-01T10:00:00Z");
        save("EMP001", "202608", PayrollStatus.DEFINITIVE, "2026-09-05T10:00:00Z");
        save("EMP002", "202609", PayrollStatus.CALCULATED, "2026-09-01T10:00:00Z");

        Page<PayrollEntity> page = repository.findPageByFilters(null, null, "EMP001", null, PageRequest.of(0, 50));

        assertEquals(3, page.getTotalElements());
        assertEquals(
                List.of("EMP001/202609/CALCULATED", "EMP001/202608/DEFINITIVE", "EMP001/202607/DEFINITIVE"),
                keys(page));
    }

    private static List<String> keys(Page<PayrollEntity> page) {
        return page.getContent().stream()
                .map(p -> p.getEmployeeNumber() + "/" + p.getPayrollPeriodCode() + "/" + p.getStatus())
                .toList();
    }

    private void save(String employeeNumber, String period, PayrollStatus status, String calculatedAt) {
        PayrollEntity payroll = new PayrollEntity();
        payroll.setRuleSystemCode("ESP");
        payroll.setEmployeeTypeCode("INTERNAL");
        payroll.setEmployeeNumber(employeeNumber);
        payroll.setPayrollPeriodCode(period);
        payroll.setPayrollTypeCode("NORMAL");
        payroll.setPresenceNumber(1);
        payroll.setStatus(status);
        payroll.setCalculatedAt(Instant.parse(calculatedAt));
        payroll.setCalculationEngineCode("ENGINE");
        payroll.setCalculationEngineVersion("1.0");
        repository.saveAndFlush(payroll);
    }
}

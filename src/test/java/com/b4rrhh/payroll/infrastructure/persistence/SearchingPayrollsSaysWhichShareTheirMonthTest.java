package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.port.PayrollSearchPage;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * La búsqueda dice de cada recibo si otra presencia del mismo empleado tiene recibo del mismo
 * período y tipo (`b4rrhh/frontend#104`).
 *
 * <p>El número de presencia distingue recibos, no describe empleados: la lista sólo lo enseña
 * cuando hay dos recibos que separar. Y la pregunta la contesta la base y no la página, porque la
 * hermana puede caer en otra página —el orden separa lo cerrado de lo abierto dentro del mes— o
 * quedar fuera del filtro de estado, y sigue existiendo.
 */
@TestSobreEsquemaReal
class SearchingPayrollsSaysWhichShareTheirMonthTest {

    @Autowired
    private SpringDataPayrollRepository repository;

    @Autowired
    private PayrollPersistenceAdapter adapter;

    @Test
    void twoPresencesInTheSameMonthAreSistersAndASecondPresenceAloneIsNot() {
        // Cesado y readmitido en abril: dos recibos de abril.
        save("EMP001", "202604", "NORMAL", 1, PayrollStatus.DEFINITIVE);
        save("EMP001", "202604", "NORMAL", 2, PayrollStatus.CALCULATED);
        // La segunda presencia en mayo va sola: es un dato de la ficha, no de la lista.
        save("EMP001", "202605", "NORMAL", 2, PayrollStatus.CALCULATED);
        // Un EXTRA del mismo mes no es hermana del NORMAL: otro tipo es otro recibo.
        save("EMP002", "202604", "NORMAL", 1, PayrollStatus.CALCULATED);
        save("EMP002", "202604", "EXTRA", 1, PayrollStatus.CALCULATED);

        PayrollSearchPage page = adapter.findPageByFilters(null, null, null, null, 0, 50);

        assertEquals(Map.of(
                "EMP001/202604/NORMAL/1", true,
                "EMP001/202604/NORMAL/2", true,
                "EMP001/202605/NORMAL/2", false,
                "EMP002/202604/NORMAL/1", false,
                "EMP002/202604/EXTRA/1", false), sharing(page));
    }

    @Test
    void theSisterCountsEvenWhenTheFilterOrThePageLeavesItOut() {
        save("EMP001", "202604", "NORMAL", 1, PayrollStatus.DEFINITIVE);
        save("EMP001", "202604", "NORMAL", 2, PayrollStatus.CALCULATED);

        PayrollSearchPage onlyOpen = adapter.findPageByFilters(null, null, null, PayrollStatus.CALCULATED, 0, 50);
        PayrollSearchPage firstOfOne = adapter.findPageByFilters(null, null, null, null, 0, 1);

        assertEquals(Map.of("EMP001/202604/NORMAL/2", true), sharing(onlyOpen));
        assertEquals(1, firstOfOne.items().size());
        assertEquals(List.of(true), firstOfOne.items().stream().map(firstOfOne::sharesItsMonth).toList());
    }

    private static Map<String, Boolean> sharing(PayrollSearchPage page) {
        return page.items().stream().collect(Collectors.toMap(
                SearchingPayrollsSaysWhichShareTheirMonthTest::key, page::sharesItsMonth));
    }

    private static String key(Payroll p) {
        return p.getEmployeeNumber() + "/" + p.getPayrollPeriodCode() + "/" + p.getPayrollTypeCode()
                + "/" + p.getPresenceNumber();
    }

    private void save(String employeeNumber, String period, String type, int presence, PayrollStatus status) {
        PayrollEntity payroll = new PayrollEntity();
        payroll.setRuleSystemCode("ESP");
        payroll.setEmployeeTypeCode("INTERNAL");
        payroll.setEmployeeNumber(employeeNumber);
        payroll.setPayrollPeriodCode(period);
        payroll.setPayrollTypeCode(type);
        payroll.setPresenceNumber(presence);
        payroll.setStatus(status);
        payroll.setCalculatedAt(Instant.parse("2026-09-01T10:00:00Z"));
        payroll.setCalculationEngineCode("ENGINE");
        payroll.setCalculationEngineVersion("1.0");
        repository.saveAndFlush(payroll);
    }
}

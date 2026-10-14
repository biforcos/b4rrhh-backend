package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * La lista de recibos busca por parte de la matrícula o del nombre (b4rrhh/frontend#141).
 *
 * <p>«Número o nombre…» decía la caja, y sólo encontraba la matrícula entera y exacta. Ahora «25»,
 * «0025» y «emp25» encuentran EMP000025, y «torres» encuentra a Eva Torres, cuyo nombre no está en el
 * recibo sino en {@code employee.employee}.</p>
 */
@TestSobreEsquemaReal
class SearchingPayrollsFindsByPartOfTheNumberOrTheNameTest {

    @Autowired
    private SpringDataPayrollRepository springData;

    @Autowired
    private PayrollRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findsByTheDigitsOfTheNumberWithoutItsZerosOrByPartOfTheName() {
        employee("EMP000025", "EVA", "TORRES");
        employee("EMP000031", "PEPE", "ULLOA");
        save("EMP000025");
        save("EMP000031");

        for (String text : new String[] {"25", "0025", "emp25", "EMP000025", "torres", "Eva Torres"}) {
            assertEquals(List.of("EMP000025"), numbers(text), "buscando " + text);
        }
        assertEquals(List.of("EMP000025", "EMP000031"), numbers("EMP"));
    }

    // La matrícula entera es la del salto desde la ficha (frontend#68): trae a ese y a nadie más. Y
    // con prefijo, los ceros son sólo relleno: «EMP25» no es EMP000125 ni EMP000250.
    @Test
    void theWholeNumberOrThePrefixedOneBringsThatEmployeeAndNoOneElse() {
        save("EMP000025");
        save("EMP000125");
        save("EMP000250");

        for (String text : new String[] {"EMP000025", "emp000025", "EMP25", "emp0025"}) {
            assertEquals(List.of("EMP000025"), numbers(text), "buscando " + text);
        }
        assertEquals(List.of("EMP000025", "EMP000125", "EMP000250"), numbers("25"));
    }

    @Test
    void theExactNumberOfTheJumpFromTheFileStaysExact() {
        save("EMP000025");
        save("EMP000250");

        assertEquals(1, repository.findPageByFilters(null, null, "EMP000025", null, 0, 50).total());
    }

    private List<String> numbers(String text) {
        return repository.searchPage(null, "202609", text, null, 0, 50).items().stream()
                .map(Payroll::getEmployeeNumber)
                .toList();
    }

    private void employee(String number, String firstName, String lastName1) {
        jdbcTemplate.update("""
                insert into employee.employee (rule_system_code, employee_type_code, employee_number, first_name,
                                               last_name_1, created_at, updated_at)
                values ('ESP', 'INTERNAL', ?, ?, ?, current_timestamp, current_timestamp)
                """, number, firstName, lastName1);
    }

    private void save(String employeeNumber) {
        PayrollEntity payroll = new PayrollEntity();
        payroll.setRuleSystemCode("ESP");
        payroll.setEmployeeTypeCode("INTERNAL");
        payroll.setEmployeeNumber(employeeNumber);
        payroll.setPayrollPeriodCode("202609");
        payroll.setPayrollTypeCode("NORMAL");
        payroll.setPresenceNumber(1);
        payroll.setStatus(PayrollStatus.CALCULATED);
        payroll.setCalculatedAt(Instant.parse("2026-09-30T10:00:00Z"));
        payroll.setCalculationEngineCode("ENGINE");
        payroll.setCalculationEngineVersion("1.0");
        springData.saveAndFlush(payroll);
    }
}

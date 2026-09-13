package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.payroll.application.port.PayrollLaunchEligibleInputContext;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La antiguedad que la nomina guarda en su foto es la del PRIMER alta, tambien cuando la
 * presencia se interrumpio (backend#91).
 *
 * <p>El caso es el de {@code EMP000001} en la demo: alta 25/11/2023, cese por jubilacion el
 * 14/01/2024, readmision el 08/03/2024. La ficha dice «2 años y 9 meses» porque cuenta desde la
 * presencia mas antigua, y la decision del issue es que eso es lo correcto: la antiguedad es una
 * fecha —un punto de partida— y no un cronometro.</p>
 *
 * <p>Lo que este test impide es el atajo obvio: rellenar el hueco con {@code presenceStartDate},
 * que es la fecha que ya viajaba y esta justo al lado. Para un readmitido da otra fecha, y
 * entonces la ficha y el recibo dirian numeros distintos del mismo empleado el mismo dia. Un
 * readmitido es el <b>unico</b> caso en el que las dos fechas se separan, asi que sin este test
 * el atajo pasa en verde.</p>
 */
@TestSobreEsquemaReal
class SeniorityIsTheFirstHireAlsoForARehiredEmployeeTest {

    private static final LocalDate PRIMER_ALTA = LocalDate.of(2023, 11, 25);
    private static final LocalDate CESE = LocalDate.of(2024, 1, 14);
    private static final LocalDate READMISION = LocalDate.of(2024, 3, 8);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PayrollLaunchEligibleInputLookupAdapter adapter;

    @Test
    void aRehiredEmployeeKeepsTheSeniorityOfTheFirstHire() {
        long employeeId = insertEmployee("EMP_ANT_1");
        insertPresence(employeeId, 1, PRIMER_ALTA, CESE);
        insertPresence(employeeId, 2, READMISION, null);

        PayrollLaunchEligibleInputContext context = contextOfSecondPresence("EMP_ANT_1");

        assertThat(context.seniorityDate())
                .as("la antiguedad es el primer alta, no la readmision")
                .isEqualTo(PRIMER_ALTA);
    }

    // La otra mitad, y la que de verdad caza el atajo: las dos fechas viajan juntas y NO son la
    // misma. Un `seniorityDate = presenceStartDate` pasaria el test de arriba si alguien lo
    // escribiera al reves, y aqui no.
    @Test
    void theSeniorityIsNotTheStartOfThePresenceThisPayrollBelongsTo() {
        long employeeId = insertEmployee("EMP_ANT_2");
        insertPresence(employeeId, 1, PRIMER_ALTA, CESE);
        insertPresence(employeeId, 2, READMISION, null);

        PayrollLaunchEligibleInputContext context = contextOfSecondPresence("EMP_ANT_2");

        assertThat(context.presenceStartDate()).isEqualTo(READMISION);
        assertThat(context.seniorityDate())
                .as("si estas dos fechas se igualan, el recibo contradice a la ficha")
                .isNotEqualTo(context.presenceStartDate());
    }

    // Y el caso comun no se rompe: quien nunca se fue tiene una sola presencia, y entonces las
    // dos fechas coinciden porque son la misma, no porque se copien.
    @Test
    void anEmployeeWhoNeverLeftHasBothDatesEqualBecauseTheyAreTheSameDate() {
        long employeeId = insertEmployee("EMP_ANT_3");
        insertPresence(employeeId, 1, PRIMER_ALTA, null);

        PayrollLaunchEligibleInputContext context = adapter.findByUnitAndPeriod(
                "ESP", "INTERNAL", "EMP_ANT_3", 1,
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)
        ).orElseThrow();

        assertThat(context.seniorityDate()).isEqualTo(PRIMER_ALTA);
        assertThat(context.presenceStartDate()).isEqualTo(PRIMER_ALTA);
    }

    /** La unidad de un mes en el que solo esta vigente la segunda presencia. */
    private PayrollLaunchEligibleInputContext contextOfSecondPresence(String employeeNumber) {
        Optional<PayrollLaunchEligibleInputContext> context = adapter.findByUnitAndPeriod(
                "ESP", "INTERNAL", employeeNumber, 2,
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)
        );
        assertThat(context).as("la segunda presencia sigue abierta en abril de 2026").isPresent();
        return context.get();
    }

    private long insertEmployee(String employeeNumber) {
        jdbcTemplate.update("""
                insert into employee.employee
                    (rule_system_code, employee_type_code, employee_number, first_name, last_name_1, status)
                values ('ESP', 'INTERNAL', ?, 'Nombre', 'Apellido', 'ACTIVE')
                """, employeeNumber);
        return jdbcTemplate.queryForObject(
                "select id from employee.employee where employee_number = ?", Long.class, employeeNumber);
    }

    private void insertPresence(long employeeId, int presenceNumber, LocalDate start, LocalDate end) {
        jdbcTemplate.update("""
                insert into employee.presence
                    (employee_id, presence_number, company_code, entry_reason_code, exit_reason_code,
                     start_date, end_date)
                values (?, ?, 'ES01', ?, ?, ?, ?)
                """,
                employeeId,
                presenceNumber,
                presenceNumber == 1 ? "HIRE" : "REHIRE",
                end == null ? null : "RETIREMENT",
                Date.valueOf(start),
                end == null ? null : Date.valueOf(end));
    }
}

package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.application.port.RuleEntityUsageCheckPort;
import com.b4rrhh.rulesystem.application.port.RuleEntityUsageParticipant;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityInUseException;
import com.b4rrhh.rulesystem.domain.model.RuleEntityReference;
import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * backend#28: cada vertical que guarda códigos de catálogo por texto declara dónde, y borrar
 * un código que alguna de ellas usa —hoy o en 2019— da {@code RuleEntityInUseException} con
 * el recuento por recurso, no un borrado silencioso.
 *
 * Un caso por vertical: siembra una fila de verdad en la tabla de producción con un código
 * sembrado por las migraciones, y comprueba que el participante lo cuenta para ese tipo, y
 * que no lo cuenta ni en otra reglamentación ni para otro tipo.
 */
@TestSobreEsquemaReal
class RuleEntityUsageParticipantsIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private List<RuleEntityUsageParticipant> participants;

    @Autowired
    private RuleEntityUsageCheckPort usageCheckPort;

    @Autowired
    private DeleteRuleEntityUseCase deleteRuleEntityUseCase;

    /** recurso, tipo, código sembrado, y el insert de una fila que lo referencia. */
    static Stream<Arguments> verticals() {
        return Stream.of(
                Arguments.of("presences", "EMPLOYEE_PRESENCE_ENTRY_REASON", "HIRING", """
                        insert into employee.presence (employee_id, presence_number, company_code, entry_reason_code, start_date)
                        values (?, 1, 'ES01', 'HIRING', date '2026-01-01')"""),
                Arguments.of("presences", "EMPLOYEE_PRESENCE_EXIT_REASON", "RETIREMENT", """
                        insert into employee.presence (employee_id, presence_number, company_code, entry_reason_code, exit_reason_code, start_date, end_date)
                        values (?, 1, 'ES01', 'HIRING', 'RETIREMENT', date '2026-01-01', date '2026-06-30')"""),
                Arguments.of("presences", "COMPANY", "ES01", """
                        insert into employee.presence (employee_id, presence_number, company_code, entry_reason_code, start_date)
                        values (?, 1, 'ES01', 'HIRING', date '2026-01-01')"""),
                Arguments.of("addresses", "EMPLOYEE_ADDRESS_TYPE", "FISCAL", """
                        insert into employee.address (employee_id, address_number, address_type_code, street, city, country_code, start_date)
                        values (?, 1, 'FISCAL', 'Calle 1', 'Madrid', 'ESP', date '2026-01-01')"""),
                Arguments.of("addresses", "COUNTRY", "ESP", """
                        insert into employee.address (employee_id, address_number, address_type_code, street, city, country_code, start_date)
                        values (?, 1, 'FISCAL', 'Calle 1', 'Madrid', 'ESP', date '2026-01-01')"""),
                Arguments.of("contacts", "CONTACT_TYPE", "COMPANY_MOBILE", """
                        insert into employee.contact (employee_id, contact_type_code, contact_value)
                        values (?, 'COMPANY_MOBILE', '600000000')"""),
                Arguments.of("identifiers", "EMPLOYEE_IDENTIFIER_TYPE", "NATIONAL_ID", """
                        insert into employee.identifier (employee_id, identifier_type_code, identifier_value)
                        values (?, 'NATIONAL_ID', '00000000T')"""),
                Arguments.of("identifiers", "COUNTRY", "ESP", """
                        insert into employee.identifier (employee_id, identifier_type_code, identifier_value, issuing_country_code)
                        values (?, 'NATIONAL_ID', '00000000T', 'ESP')"""),
                Arguments.of("contracts", "CONTRACT", "100", """
                        insert into employee.contract (employee_id, contract_code, contract_subtype_code, start_date)
                        values (?, '100', '01', date '2026-01-01')"""),
                Arguments.of("contracts", "CONTRACT_SUBTYPE", "01", """
                        insert into employee.contract (employee_id, contract_code, contract_subtype_code, start_date)
                        values (?, '100', '01', date '2026-01-01')"""),
                Arguments.of("labor-classifications", "AGREEMENT", "99002405011982", """
                        insert into employee.labor_classification (employee_id, agreement_code, agreement_category_code, start_date)
                        values (?, '99002405011982', '99002405-G1', date '2026-01-01')"""),
                Arguments.of("labor-classifications", "AGREEMENT_CATEGORY", "99002405-G1", """
                        insert into employee.labor_classification (employee_id, agreement_code, agreement_category_code, start_date)
                        values (?, '99002405011982', '99002405-G1', date '2026-01-01')"""),
                Arguments.of("work-centers", "WORK_CENTER", "BRANCH_EAST", """
                        insert into employee.work_center (employee_id, work_center_assignment_number, work_center_code, start_date)
                        values (?, 1, 'BRANCH_EAST', date '2026-01-01')"""),
                Arguments.of("cost-centers", "COST_CENTER", "CC_ADMIN", """
                        insert into employee.cost_center (employee_id, cost_center_code, allocation_percentage, start_date)
                        values (?, 'CC_ADMIN', 100, date '2026-01-01')""")
        );
    }

    @ParameterizedTest(name = "{0} declares {1}")
    @MethodSource("verticals")
    void eachVerticalCountsTheCodesItStores(String resource, String typeCode, String code, String insert) {
        RuleEntityUsageParticipant participant = participants.stream()
                .filter(candidate -> candidate.resource().equals(resource))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no participant declares " + resource));
        long before = participant.countReferences("ESP", typeCode, code);

        jdbcTemplate.update(insert, DatosDePrueba.empleado(jdbcTemplate));

        assertThat(participant.countReferences("ESP", typeCode, code)).isEqualTo(before + 1);
        assertThat(participant.countReferences("FRA", typeCode, code)).as("otra reglamentación").isZero();
        assertThat(participant.countReferences("ESP", "SOME_OTHER_TYPE", code)).as("otro tipo").isZero();
    }

    @Test
    void aCodeUsedOnlyInTheHistoryIsStillInUse() {
        Long employeeId = DatosDePrueba.empleado(jdbcTemplate);
        DatosDePrueba.presencia(jdbcTemplate, employeeId, 1, LocalDate.of(2018, 3, 1), LocalDate.of(2019, 6, 30));

        List<RuleEntityReference> references =
                usageCheckPort.findReferences("ESP", "EMPLOYEE_PRESENCE_EXIT_REASON", "TERMINATION");

        assertThat(references).anySatisfy(reference -> {
            assertThat(reference.resource()).isEqualTo("presences");
            assertThat(reference.count()).isGreaterThanOrEqualTo(1);
        });
    }

    @Test
    void deletingACodeReferencedByAPresenceIsRefusedSayingHowManyAndWhere() {
        Long ruleEntityId = DatosDePrueba.ruleEntity(jdbcTemplate, "EMPLOYEE_PRESENCE_ENTRY_REASON",
                "TST_REASON", "Test reason", LocalDate.of(1900, 1, 1), null);
        Long employeeId = DatosDePrueba.empleado(jdbcTemplate);
        jdbcTemplate.update("""
                insert into employee.presence (employee_id, presence_number, company_code, entry_reason_code, start_date, end_date)
                values (?, 1, 'ES01', 'TST_REASON', date '2018-01-01', date '2018-12-31')
                """, employeeId);
        jdbcTemplate.update("""
                insert into employee.presence (employee_id, presence_number, company_code, entry_reason_code, start_date)
                values (?, 2, 'ES01', 'TST_REASON', date '2026-01-01')
                """, employeeId);

        assertThatThrownBy(() -> deleteRuleEntityUseCase.delete(new DeleteRuleEntityCommand(
                "ESP", "EMPLOYEE_PRESENCE_ENTRY_REASON", "TST_REASON", LocalDate.of(1900, 1, 1))))
                .isInstanceOf(RuleEntityInUseException.class)
                .hasMessage("Rule entity is in use: ESP/EMPLOYEE_PRESENCE_ENTRY_REASON/TST_REASON"
                        + " is referenced by 2 presences");

        Integer stillThere = jdbcTemplate.queryForObject(
                "select count(*) from rulesystem.rule_entity where id = ?", Integer.class, ruleEntityId);
        assertThat(stillThere).as("el código sigue ahí").isEqualTo(1);
    }
}

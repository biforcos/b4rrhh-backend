package com.b4rrhh.rulesystem.employeeaddresstypeprofile.infrastructure.persistence;

import com.b4rrhh.rulesystem.employeeaddresstypeprofile.domain.model.EmployeeAddressTypeCoverage;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La guardia de la cobertura de los tipos de direccion (backend#53), sobre el esquema real.
 *
 * Que la extension exista, este declarada, caiga en cascada y tenga fila para cada tipo lo
 * comprueba ya {@code EveryRuleEntityExtensionIsDeclaredAndEnforcedTest} (ADR-053 §6). Lo que
 * solo puede comprobar esta es la regla propia del perfil: en cada sistema de reglas hay
 * <b>exactamente un</b> tipo de direccion obligatorio — ni cero, que dejaria al empleado sin
 * domicilio que exigir, ni dos, que exigirian dos series a la vez.
 *
 * La sonda provoca los dos fallos dentro de la transaccion del test, que se deshace sola: es
 * la disciplina de {@code RuleEntityTypeClassificationGuardTest} (ADR-054 §7). Una guardia que
 * nunca se ha visto en rojo no se sabe si mira algo.
 */
@TestSobreEsquemaReal
class EmployeeAddressTypeCoverageGuardTest {

    private static final String MANDATORY_PER_RULE_SYSTEM = """
            select re.rule_system_code,
                   count(*) filter (where p.coverage = 'MANDATORY') as mandatory_types
              from rulesystem.rule_entity re
              left join rulesystem.employee_address_type_profile p on p.address_type_rule_entity_id = re.id
             where re.rule_entity_type_code = 'EMPLOYEE_ADDRESS_TYPE'
             group by re.rule_system_code
             order by re.rule_system_code
            """;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EmployeeAddressTypeProfilePersistenceAdapter adapter;

    @Test
    void everyRuleSystemWithAddressTypesMarksExactlyOneAsMandatory() {
        Map<String, Long> offenders = ruleSystemsWithoutExactlyOneMandatoryType();

        assertThat(mandatoryTypesPerRuleSystem()).as("hay sistemas de reglas con tipos de direccion").isNotEmpty();
        assertThat(offenders)
                .withFailMessage("""
                        Sistemas de reglas que no tienen exactamente un tipo de direccion obligatorio \
                        (sistema -> cuantos MANDATORY): %s

                        Cero deja al empleado sin domicilio que exigir; dos exigen dos series a la vez. \
                        Se arregla la semilla de employee_address_type_profile, no la guardia (backend#53).
                        """, offenders)
                .isEmpty();
    }

    // La sonda, en rojo por exceso: un segundo tipo obligatorio en ESP aparece solo.
    @Test
    void aSecondMandatoryTypeShowsUpByItself() {
        jdbcTemplate.update("""
                update rulesystem.employee_address_type_profile p
                   set coverage = 'MANDATORY'
                  from rulesystem.rule_entity re
                 where re.id = p.address_type_rule_entity_id
                   and re.rule_system_code = 'ESP'
                   and re.rule_entity_type_code = 'EMPLOYEE_ADDRESS_TYPE'
                   and re.code = 'FISCAL'
                """);

        assertThat(ruleSystemsWithoutExactlyOneMandatoryType()).containsExactly(Map.entry("ESP", 2L));
    }

    // Y en rojo por defecto: un sistema de reglas sin ningun tipo obligatorio aparece igual.
    @Test
    void aRuleSystemWithoutAMandatoryTypeShowsUpByItself() {
        jdbcTemplate.update("""
                update rulesystem.employee_address_type_profile p
                   set coverage = 'OPTIONAL'
                  from rulesystem.rule_entity re
                 where re.id = p.address_type_rule_entity_id
                   and re.rule_system_code = 'ESP'
                   and re.rule_entity_type_code = 'EMPLOYEE_ADDRESS_TYPE'
                """);

        assertThat(ruleSystemsWithoutExactlyOneMandatoryType()).containsExactly(Map.entry("ESP", 0L));
    }

    // Lo que lee la vertical: el domicilio de ESP es HOME, los demas son opcionales, y un
    // tipo que no existe no se inventa una cobertura.
    @Test
    void theAdapterReadsTheSeededCoverage() {
        assertThat(adapter.findCoverageByAddressType("ESP", "HOME")).contains(EmployeeAddressTypeCoverage.MANDATORY);
        assertThat(adapter.findCoverageByAddressType("ESP", "FISCAL")).contains(EmployeeAddressTypeCoverage.OPTIONAL);
        assertThat(adapter.findCoverageByAddressType("ESP", "MAILING")).contains(EmployeeAddressTypeCoverage.OPTIONAL);
        assertThat(adapter.findCoverageByAddressType("ESP", "ZZ_UNKNOWN")).isEqualTo(Optional.empty());
    }

    private Map<String, Long> ruleSystemsWithoutExactlyOneMandatoryType() {
        Map<String, Long> offenders = new TreeMap<>();
        mandatoryTypesPerRuleSystem().forEach((ruleSystemCode, mandatoryTypes) -> {
            if (mandatoryTypes != 1L) {
                offenders.put(ruleSystemCode, mandatoryTypes);
            }
        });
        return offenders;
    }

    private Map<String, Long> mandatoryTypesPerRuleSystem() {
        Map<String, Long> counts = new TreeMap<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(MANDATORY_PER_RULE_SYSTEM);
        for (Map<String, Object> row : rows) {
            counts.put((String) row.get("rule_system_code"), ((Number) row.get("mandatory_types")).longValue());
        }
        return counts;
    }
}

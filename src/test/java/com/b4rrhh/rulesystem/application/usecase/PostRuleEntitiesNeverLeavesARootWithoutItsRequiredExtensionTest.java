package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.domain.exception.RuleEntityTypeIsMaintainedByItsOwnEndpointException;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * La guardia que faltaba del ADR-053 (backend#88). La guardia 3 de
 * {@code EveryRuleEntityExtensionIsDeclaredAndEnforcedTest} vigila los datos sembrados; ésta
 * vigila el <b>camino de código</b>: que no exista una forma de crear por API una raíz sin la
 * extensión que su tipo declara obligatoria.
 *
 * <p>Hacía falta porque {@code POST /rule-entities} era esa forma, y desde el #34 y el #35 una
 * empresa o un centro creados por ahí se vuelven ilegibles: el {@code GET}, el {@code PUT} y la
 * lista de todos sus hermanos fallan con un 500, y no hay pantalla desde la que arreglarlo.</p>
 *
 * <p>Los tres tests van contra el metamodelo, nunca contra una lista de tipos: el primero
 * recorre lo que {@code rule_entity_extension} declara hoy, el segundo comprueba que un tipo
 * sin extensiones sigue entrando por la puerta genérica —que es su puerta— y el tercero declara
 * una extensión obligatoria para un tipo nuevo dentro de la transacción del test y ve aparecer
 * el rechazo <b>sin tocar una línea de código</b>. Ése es el que distingue esta forma de la de
 * escribir en Java los tipos que tienen pantalla propia.</p>
 */
@TestSobreEsquemaReal
class PostRuleEntitiesNeverLeavesARootWithoutItsRequiredExtensionTest {

    @Autowired
    private CreateRuleEntityUseCase createRuleEntityUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void noTypeWithARequiredExtensionCanBeCreatedThroughTheGenericEndpoint() {
        List<String> typesWithRequiredExtension = jdbcTemplate.queryForList("""
                select distinct rule_entity_type_code
                  from rulesystem.rule_entity_extension
                 where required
                 order by rule_entity_type_code
                """, String.class);
        assertThat(typesWithRequiredExtension).as("hay extensiones required declaradas").isNotEmpty();

        Map<String, String> accepted = new TreeMap<>();
        for (String typeCode : typesWithRequiredExtension) {
            Throwable thrown = catchThrowable(() -> createRuleEntityUseCase.create(
                    new CreateRuleEntityCommand("ESP", typeCode, "ZZ_PROBE", "Probe", null,
                            LocalDate.of(2020, 1, 1), null)));

            if (!(thrown instanceof RuleEntityTypeIsMaintainedByItsOwnEndpointException)) {
                accepted.put(typeCode, thrown == null ? "creada" : thrown.toString());
            }
        }

        assertThat(accepted)
                .withFailMessage("""
                        Estos tipos declaran una extensión required y POST /rule-entities no los \
                        rechazó (tipo -> qué pasó): %s

                        Una raíz creada así queda sin su extensión obligatoria, y desde el \
                        backend#34 y el #35 eso la vuelve ilegible: tumba también la lista de sus \
                        hermanos. Lo que se arregla es el alta, no el aviso.
                        """, accepted)
                .isEmpty();
    }

    // La otra mitad: el endpoint genérico sigue siendo la puerta de los maestros simples, que
    // son una fila en rule_entity y nada más (ADR-053 §4). Un rechazo que se llevara a éstos por
    // delante sería peor que el defecto.
    @Test
    void aTypeWithoutExtensionsStillGoesThroughTheGenericEndpoint() {
        String typeCode = jdbcTemplate.queryForObject("""
                select t.code from rulesystem.rule_entity_type t
                 where not exists (
                        select 1 from rulesystem.rule_entity_extension e
                         where e.rule_entity_type_code = t.code and e.required)
                 order by t.code
                 limit 1
                """, String.class);

        assertThat(createRuleEntityUseCase.create(new CreateRuleEntityCommand(
                "ESP", typeCode, "ZZ_PROBE", "Probe", null, LocalDate.of(2020, 1, 1), null))
        ).isNotNull();
    }

    // La prueba de que el criterio sale del metamodelo: se declara la extensión obligatoria de un
    // tipo que no conoce nadie y el rechazo aparece solo, con el mensaje diciendo a dónde ir. El
    // DDL y los inserts van dentro de la transacción del test y se deshacen con ella.
    @Test
    void aNewTypeIsCoveredTheDayItDeclaresItsRequiredExtension() {
        jdbcTemplate.execute("""
                create table rulesystem.zz_probe_profile (
                    zz_probe_rule_entity_id bigint not null
                        references rulesystem.rule_entity(id) on delete cascade
                )""");
        jdbcTemplate.update("""
                insert into rulesystem.rule_entity_type
                    (code, name, active, literal_class, maintenance_mode, group_code, api_collection_path)
                select 'ZZ_PROBE_TYPE', 'Probe', true, 'DOMAIN_VOCABULARY', 'MAINTAINED', group_code,
                       '/zz-probe-things'
                  from rulesystem.rule_entity_type where code = 'COST_CENTER'
                """);
        jdbcTemplate.update("""
                insert into rulesystem.rule_entity_extension
                    (rule_entity_type_code, extension_code, table_name, cardinality, required)
                values ('ZZ_PROBE_TYPE', 'PROFILE', 'rulesystem.zz_probe_profile', '1:1', true)
                """);

        Throwable thrown = catchThrowable(() -> createRuleEntityUseCase.create(
                new CreateRuleEntityCommand("ESP", "ZZ_PROBE_TYPE", "ZZ_PROBE", "Probe", null,
                        LocalDate.of(2020, 1, 1), null)));

        assertThat(thrown)
                .isInstanceOf(RuleEntityTypeIsMaintainedByItsOwnEndpointException.class)
                .hasMessageContaining("ZZ_PROBE_TYPE")
                .hasMessageContaining("rulesystem.zz_probe_profile")
                .hasMessageContaining("POST /zz-probe-things");
    }
}

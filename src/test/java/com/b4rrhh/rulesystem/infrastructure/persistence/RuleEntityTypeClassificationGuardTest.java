package com.b4rrhh.rulesystem.infrastructure.persistence;

import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * La sonda de la clasificación de tipos de entidad (ADR-054 §7, backend#37).
 *
 * La mayor parte de la guardia no la hace ningún test: la clausura del grupo la impone la
 * clave ajena a {@code rule_entity_type_group} y la completitud los tres {@code not null}
 * sin defecto de la V111. Esta sonda comprueba que esa imposición existe de verdad — el
 * mismo movimiento que la de {@code EveryRuleEntityExtensionIsDeclaredAndEnforcedTest}:
 * se provoca el fallo dentro de la transacción del test, que se deshace sola.
 *
 * No hay guardia de comportamiento para el modo cerrado, a propósito: el ADR-054 §7 la
 * retiró porque el comportamiento de un tipo de empleado vive en las filas de ámbito de
 * {@code concept_assignment}, no en constantes Java.
 */
@TestSobreEsquemaReal
class RuleEntityTypeClassificationGuardTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Un tipo sin clasificar es indistinguible de uno escondido a propósito (ADR-054 §6):
    // el insert tiene que fallar, y el mensaje tiene que decir qué tipo era — se mira la
    // causa raíz de Postgres, no el mensaje de Spring, que repite el SQL entero y daría
    // la aserción por buena aunque el detalle no nombrara la fila.
    @Test
    void aTypeWithoutItsThreeDecisionsCannotExist() {
        Throwable violation = catchThrowable(() -> jdbcTemplate.update("""
                insert into rulesystem.rule_entity_type (code, name, active)
                values ('ZZ_PROBE_TYPE', 'Probe', true)
                """));

        assertThat(violation).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(((DataIntegrityViolationException) violation).getMostSpecificCause().getMessage())
                .contains("ZZ_PROBE_TYPE")
                .contains("not-null");
    }
}

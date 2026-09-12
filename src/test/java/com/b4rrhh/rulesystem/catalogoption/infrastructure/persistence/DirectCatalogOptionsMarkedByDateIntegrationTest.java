package com.b4rrhh.rulesystem.catalogoption.infrastructure.persistence;

import com.b4rrhh.rulesystem.catalogoption.application.query.GetDirectCatalogOptionsQuery;
import com.b4rrhh.rulesystem.catalogoption.application.usecase.GetDirectCatalogOptionsService;
import com.b4rrhh.rulesystem.catalogoption.application.usecase.GetDirectCatalogOptionsUseCase;
import com.b4rrhh.rulesystem.catalogoption.domain.model.DirectCatalogOption;
import com.b4rrhh.rulesystem.infrastructure.persistence.SpringDataRuleEntityRepository;
import com.b4rrhh.rulesystem.translation.infrastructure.persistence.SpringDataRuleEntityTranslationRepository;
import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La fecha de referencia de las opciones de catálogo **fecha la vigencia, no filtra**
 * (backend#32 / b4rrhh/frontend#32).
 *
 * Este test es el del backend#30 dado la vuelta. Aquél comprobaba que con una fecha de
 * referencia quedaban fuera el código cerrado en 2020 y el que empieza en 2030, que era el
 * comportamiento de entonces; ahora salen los tres y lo que cambia es la marca. El escenario
 * sembrado es el mismo a propósito: es el mismo caso, contestado al revés.
 *
 * El porqué, resumido: en este dominio **elegir un código no vigente es frecuente** —la
 * corrección administrativa, «esto se grabó mal, ponle el código antiguo»—, y si la excepción
 * es frecuente no es una excepción. Esconder esos códigos obliga a un modo especial que el
 * usuario tiene que saber que existe, y los modos que hay que conocer no los conoce nadie.
 *
 * Va sobre el esquema real y con filas reales, como el del #30: el motivo original sigue
 * valiendo —un mock devuelve lo que le digas aunque el JPQL sea inválido— y ahora además
 * cubre el caso use, que es quien pone la marca.
 */
@TestSobreEsquemaReal
class DirectCatalogOptionsMarkedByDateIntegrationTest {

    private static final String TYPE = "EMPLOYEE_PRESENCE_ENTRY_REASON";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SpringDataRuleEntityRepository springDataRuleEntityRepository;

    @Autowired
    private SpringDataRuleEntityTranslationRepository springDataRuleEntityTranslationRepository;

    private GetDirectCatalogOptionsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetDirectCatalogOptionsService(new RuleEntityDirectCatalogOptionReadAdapter(
                springDataRuleEntityRepository, springDataRuleEntityTranslationRepository));
        DatosDePrueba.ruleEntity(jdbcTemplate, TYPE, "TST_CLOSED", "Closed in 2020",
                LocalDate.of(1900, 1, 1), LocalDate.of(2020, 12, 31));
        DatosDePrueba.ruleEntity(jdbcTemplate, TYPE, "TST_FUTURE", "Starts in 2030",
                LocalDate.of(2030, 1, 1), null);
    }

    @Test
    void withAReferenceDateEveryCodeComesBackMarkedForThatDay() {
        Map<String, Boolean> vigencia = vigenciaPorCodigo(LocalDate.of(2026, 1, 1));

        assertThat(vigencia).containsKeys("HIRING", "TST_CLOSED", "TST_FUTURE");
        assertThat(vigencia.get("HIRING")).as("abierto desde siempre").isTrue();
        assertThat(vigencia.get("TST_CLOSED")).as("cerrado en 2020").isFalse();
        assertThat(vigencia.get("TST_FUTURE")).as("empieza en 2030").isFalse();
    }

    // La marca es de esa fecha, no de hoy: en 2020 el cerrado estaba vigente y el
    // de 2030 no, y en 2030 al reves. Es lo que hace util pedir la fecha.
    @Test
    void theMarkFollowsTheDateThatWasAskedFor() {
        assertThat(vigenciaPorCodigo(LocalDate.of(2020, 12, 31)))
                .containsEntry("TST_CLOSED", true)
                .containsEntry("TST_FUTURE", false);

        assertThat(vigenciaPorCodigo(LocalDate.of(2030, 1, 1)))
                .containsEntry("TST_CLOSED", false)
                .containsEntry("TST_FUTURE", true);
    }

    @Test
    void withoutAReferenceDateEveryCodeComesBackMarkedForToday() {
        Map<String, Boolean> vigencia = vigenciaPorCodigo(null);

        assertThat(vigencia).containsKeys("HIRING", "TST_CLOSED", "TST_FUTURE");
        assertThat(vigencia.get("TST_CLOSED")).isFalse();
        assertThat(vigencia.get("TST_FUTURE")).isFalse();
    }

    // Dado de baja y no vigente son cosas distintas: lo dado de baja no se ofrece nunca,
    // con fecha o sin ella.
    @Test
    void aCodeThatIsNotActiveNeverComesBack() {
        DatosDePrueba.ruleEntity(jdbcTemplate, TYPE, "TST_INACTIVE", "Dado de baja",
                LocalDate.of(1900, 1, 1), null);
        jdbcTemplate.update("""
                update rulesystem.rule_entity set active = false
                 where rule_system_code = 'ESP' and rule_entity_type_code = ? and code = 'TST_INACTIVE'
                """, TYPE);

        assertThat(vigenciaPorCodigo(LocalDate.of(2026, 1, 1))).doesNotContainKey("TST_INACTIVE");
        assertThat(vigenciaPorCodigo(null)).doesNotContainKey("TST_INACTIVE");
    }

    private Map<String, Boolean> vigenciaPorCodigo(LocalDate referenceDate) {
        List<DirectCatalogOption> items = useCase
                .get(new GetDirectCatalogOptionsQuery("ESP", TYPE, referenceDate, null))
                .items();

        return items.stream().collect(Collectors.toMap(DirectCatalogOption::code, DirectCatalogOption::active));
    }
}

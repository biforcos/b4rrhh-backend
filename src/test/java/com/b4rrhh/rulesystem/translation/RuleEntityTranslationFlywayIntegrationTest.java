package com.b4rrhh.rulesystem.translation;

import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.rulesystem.translation.application.usecase.GetRuleEntityTranslationCoverageUseCase;
import com.b4rrhh.rulesystem.translation.application.usecase.RuleEntityTranslationCoverage;
import com.b4rrhh.rulesystem.translation.application.usecase.RuleEntityTranslationCoverage.MissingCode;
import com.b4rrhh.rulesystem.translation.application.usecase.RuleEntityTranslationCoverage.TypeCoverage;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * La tabla de traducciones sobre el esquema real (ADR-052 §1, §3 y §5; backend#23).
 *
 * La primera prueba es la que hace honesto el cambio: con la tabla vacía —que es como llega
 * la migración— cada código de {@code rule_entity} sigue resolviéndose exactamente al
 * literal base, pida el idioma que pida el cliente. Traducir es meter filas, y hasta que se
 * meten no cambia nada.
 */
@TestSobreEsquemaReal
class RuleEntityTranslationFlywayIntegrationTest {

    private static final String ENTRY_REASON = "EMPLOYEE_PRESENCE_ENTRY_REASON";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RuleEntityLabelResolver resolver;

    @Autowired
    private GetRuleEntityTranslationCoverageUseCase coverageUseCase;

    @Test
    void withAnEmptyTranslationTableEveryCodeResolvesToItsBaseLiteralWhateverTheLanguage() {
        Integer translations = jdbcTemplate.queryForObject(
                "select count(*) from rulesystem.rule_entity_translation", Integer.class);
        assertThat(translations).as("la migración no siembra traducciones").isZero();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select rule_system_code, rule_entity_type_code, code, name
                  from rulesystem.rule_entity
                """);
        assertThat(rows).isNotEmpty();

        for (Map<String, Object> row : rows) {
            String ruleSystemCode = (String) row.get("rule_system_code");
            String typeCode = (String) row.get("rule_entity_type_code");
            String code = (String) row.get("code");
            String baseLiteral = ((String) row.get("name")).trim();

            for (String language : new String[] {null, "es-ES", "fr-FR", "en"}) {
                assertThat(resolver.resolveName(ruleSystemCode, typeCode, code, language))
                        .as("%s/%s/%s con idioma %s", ruleSystemCode, typeCode, code, language)
                        .contains(baseLiteral);
            }
        }
    }

    @Test
    void aTranslatedRowIsServedForItsLanguageAndTheBaseLiteralForAnyOther() {
        insertTranslation("ESP", ENTRY_REASON, "HIRING", "es-ES", "Contratación");

        assertThat(resolver.resolveName("ESP", ENTRY_REASON, "HIRING", "es-ES")).contains("Contratación");
        assertThat(resolver.resolveName("ESP", ENTRY_REASON, "HIRING", "fr-FR")).contains("Hiring");
        assertThat(resolver.resolveName("ESP", ENTRY_REASON, "HIRING", null)).contains("Hiring");
    }

    // Traducir por id y no por (tipo, código) significa que el español de HIRING/FRA es otra
    // fila. Está asumido en el ADR-052 §1; el test lo deja escrito para que nadie lo
    // descubra en producción.
    @Test
    void aTranslationBelongsToOneRuleSystemOnly() {
        insertTranslation("ESP", ENTRY_REASON, "HIRING", "es-ES", "Contratación");

        assertThat(resolver.resolveName("FRA", ENTRY_REASON, "HIRING", "es-ES")).contains("Hiring");
    }

    // Un caso por invocación: la primera inserción rechazada aborta la transacción del test.
    @ParameterizedTest
    @ValueSource(strings = {"es_ES", "ES", "es-es", "spa"})
    void theSchemaRejectsALanguageCodeThatIsNotShortBcp47(String badLanguage) {
        assertThatThrownBy(() -> insertTranslation("ESP", ENTRY_REASON, "HIRING", badLanguage, "Contratación"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theCoverageReportMatchesWhatIsSeeded() {
        insertTranslation("ESP", ENTRY_REASON, "HIRING", "es-ES", "Contratación");
        insertTranslation("FRA", ENTRY_REASON, "HIRING", "es-ES", "Contratación");
        Long entryReasons = jdbcTemplate.queryForObject(
                "select count(*) from rulesystem.rule_entity where rule_entity_type_code = ?", Long.class, ENTRY_REASON);
        Integer types = jdbcTemplate.queryForObject(
                "select count(distinct rule_entity_type_code) from rulesystem.rule_entity", Integer.class);

        RuleEntityTranslationCoverage coverage = coverageUseCase.getCoverage("es-ES");

        assertThat(coverage.types()).hasSize(types);
        TypeCoverage entryReasonCoverage = coverage.types().stream()
                .filter(type -> type.ruleEntityTypeCode().equals(ENTRY_REASON))
                .findFirst()
                .orElseThrow();
        assertThat(entryReasonCoverage.total()).isEqualTo(entryReasons);
        assertThat(entryReasonCoverage.translated()).isEqualTo(2);
        assertThat(entryReasonCoverage.missing()).isEqualTo(entryReasons - 2);
        assertThat(entryReasonCoverage.missingCodes())
                .hasSize((int) (entryReasons - 2))
                .contains(new MissingCode("PRT", "HIRING", "Hiring"))
                .doesNotContain(new MissingCode("ESP", "HIRING", "Hiring"), new MissingCode("FRA", "HIRING", "Hiring"));

        // Todo lo demás sigue sin traducir, y el informe lo dice
        coverage.types().stream()
                .filter(type -> !type.ruleEntityTypeCode().equals(ENTRY_REASON))
                .forEach(type -> assertThat(type.translated()).as(type.ruleEntityTypeCode()).isZero());
    }

    private void insertTranslation(String ruleSystemCode, String typeCode, String code, String language, String name) {
        int inserted = jdbcTemplate.update("""
                insert into rulesystem.rule_entity_translation (rule_entity_id, language_code, name)
                select id, ?, ?
                  from rulesystem.rule_entity
                 where rule_system_code = ?
                   and rule_entity_type_code = ?
                   and code = ?
                """, language, name, ruleSystemCode, typeCode, code);
        assertThat(inserted).as("%s/%s/%s viene en la semilla", ruleSystemCode, typeCode, code).isEqualTo(1);
    }
}

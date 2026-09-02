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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * La tabla de traducciones sobre el esquema real (ADR-052 §1, §3 y §5; backend#23).
 *
 * Hasta la V114 la tabla llegaba vacía y la primera prueba lo afirmaba: cada código se
 * resolvía al literal base pidiera lo que pidiera el cliente. Desde backend#40 la migración
 * siembra el castellano de los tipos de vocabulario del dominio, y lo que se afirma es lo
 * que decidió el ADR-052 §2: la semilla cubre esos tipos enteros, no toca a las citas
 * reglamentarias ni a los nombres propios, y para cualquier otro idioma —o sin idioma— todo
 * sigue cayendo al literal base. Traducir sigue siendo meter filas; ahora hay filas.
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
    void forALanguageWithoutTranslationsEveryCodeResolvesToItsBaseLiteral() {
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

            for (String language : new String[] {null, "fr-FR", "en"}) {
                assertThat(resolver.resolveName(ruleSystemCode, typeCode, code, language))
                        .as("%s/%s/%s con idioma %s", ruleSystemCode, typeCode, code, language)
                        .contains(baseLiteral);
            }
        }
    }

    // La semilla de la V114 (backend#40): el castellano de todo el vocabulario del dominio y
    // de nada más. Qué tipos son traducibles lo dice el metamodelo (ADR-054), no una lista.
    @Test
    void theSeedTranslatesEveryDomainVocabularyCodeToSpanishAndNothingElse() {
        Map<String, String> literalClassByType = new HashMap<>();
        jdbcTemplate.query("select code, literal_class from rulesystem.rule_entity_type",
                rs -> { literalClassByType.put(rs.getString(1), rs.getString(2)); });

        RuleEntityTranslationCoverage coverage = coverageUseCase.getCoverage("es-ES");

        assertThat(coverage.types()).isNotEmpty();
        for (TypeCoverage type : coverage.types()) {
            String literalClass = literalClassByType.get(type.ruleEntityTypeCode());
            if ("DOMAIN_VOCABULARY".equals(literalClass)) {
                assertThat(type.missingCodes())
                        .as("%s sin traducir al castellano", type.ruleEntityTypeCode())
                        .isEmpty();
                assertThat(type.translated())
                        .as("%s traducido entero", type.ruleEntityTypeCode())
                        .isEqualTo(type.total())
                        .isPositive();
            } else {
                assertThat(type.translated())
                        .as("%s es %s y no se traduce", type.ruleEntityTypeCode(), literalClass)
                        .isZero();
            }
        }
    }

    // El literal base sigue intacto: la semilla añade filas en la tabla de traducciones y
    // no toca rule_entity.name, que es el inglés neutro (ADR-052 §1).
    @Test
    void aTranslatedRowIsServedForItsLanguageAndTheBaseLiteralForAnyOther() {
        assertThat(resolver.resolveName("ESP", ENTRY_REASON, "HIRING", "es-ES")).contains("Contratación");
        assertThat(resolver.resolveName("ESP", ENTRY_REASON, "HIRING", "fr-FR")).contains("Hiring");
        assertThat(resolver.resolveName("ESP", ENTRY_REASON, "HIRING", null)).contains("Hiring");
    }

    // Traducir por id y no por (tipo, código) significa que el francés de HIRING/FRA es otra
    // fila. Está asumido en el ADR-052 §1; el test lo deja escrito para que nadie lo
    // descubra en producción.
    @Test
    void aTranslationBelongsToOneRuleSystemOnly() {
        insertTranslation("ESP", ENTRY_REASON, "HIRING", "fr-FR", "Embauche");

        assertThat(resolver.resolveName("ESP", ENTRY_REASON, "HIRING", "fr-FR")).contains("Embauche");
        assertThat(resolver.resolveName("FRA", ENTRY_REASON, "HIRING", "fr-FR")).contains("Hiring");
    }

    // Un caso por invocación: la primera inserción rechazada aborta la transacción del test.
    @ParameterizedTest
    @ValueSource(strings = {"es_ES", "ES", "es-es", "spa"})
    void theSchemaRejectsALanguageCodeThatIsNotShortBcp47(String badLanguage) {
        assertThatThrownBy(() -> insertTranslation("ESP", ENTRY_REASON, "HIRING", badLanguage, "Contratación"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // El informe de cobertura lista los huecos. Se prueba con un idioma que la semilla no
    // trae, para que los huecos sean los que mete este test y no los de la V114.
    @Test
    void theCoverageReportMatchesWhatIsSeeded() {
        insertTranslation("ESP", ENTRY_REASON, "HIRING", "fr-FR", "Embauche");
        insertTranslation("FRA", ENTRY_REASON, "HIRING", "fr-FR", "Embauche");
        Long entryReasons = jdbcTemplate.queryForObject(
                "select count(*) from rulesystem.rule_entity where rule_entity_type_code = ?", Long.class, ENTRY_REASON);
        Integer types = jdbcTemplate.queryForObject(
                "select count(distinct rule_entity_type_code) from rulesystem.rule_entity", Integer.class);

        RuleEntityTranslationCoverage coverage = coverageUseCase.getCoverage("fr-FR");

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

        // Todo lo demás sigue sin traducir al francés, y el informe lo dice
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

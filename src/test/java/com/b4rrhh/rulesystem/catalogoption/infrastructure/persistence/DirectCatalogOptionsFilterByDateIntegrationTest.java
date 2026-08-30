package com.b4rrhh.rulesystem.catalogoption.infrastructure.persistence;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * backend#30: el filtro por fecha de las opciones de catálogo no se había ejecutado nunca con
 * éxito contra Postgres —{@code :referenceDate is null} sin tipo deducible daba
 * «could not determine data type of parameter»— y sólo lo cubrían tests con mock. Este va
 * sobre el esquema real y con filas reales.
 */
@TestSobreEsquemaReal
class DirectCatalogOptionsFilterByDateIntegrationTest {

    private static final String TYPE = "EMPLOYEE_PRESENCE_ENTRY_REASON";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SpringDataRuleEntityRepository springDataRuleEntityRepository;

    @Autowired
    private SpringDataRuleEntityTranslationRepository springDataRuleEntityTranslationRepository;

    private RuleEntityDirectCatalogOptionReadAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RuleEntityDirectCatalogOptionReadAdapter(
                springDataRuleEntityRepository, springDataRuleEntityTranslationRepository);
        DatosDePrueba.ruleEntity(jdbcTemplate, TYPE, "TST_CLOSED", "Closed in 2020",
                LocalDate.of(1900, 1, 1), LocalDate.of(2020, 12, 31));
        DatosDePrueba.ruleEntity(jdbcTemplate, TYPE, "TST_FUTURE", "Starts in 2030",
                LocalDate.of(2030, 1, 1), null);
    }

    @Test
    void withAReferenceDateTheRowsClosedBeforeItAndTheOnesNotYetStartedAreLeftOut() {
        List<String> codes = codes(adapter.findDirectOptions("ESP", TYPE, LocalDate.of(2026, 1, 1), null));

        assertThat(codes).contains("HIRING").doesNotContain("TST_CLOSED", "TST_FUTURE");
    }

    @Test
    void theReferenceDateIsInclusiveOnBothEnds() {
        assertThat(codes(adapter.findDirectOptions("ESP", TYPE, LocalDate.of(2020, 12, 31), null)))
                .contains("TST_CLOSED");
        assertThat(codes(adapter.findDirectOptions("ESP", TYPE, LocalDate.of(2030, 1, 1), null)))
                .contains("TST_FUTURE");
    }

    @Test
    void withoutAReferenceDateEveryActiveRowComesBack() {
        assertThat(codes(adapter.findDirectOptions("ESP", TYPE, null, null)))
                .contains("HIRING", "TST_CLOSED", "TST_FUTURE");
    }

    private static List<String> codes(List<DirectCatalogOption> options) {
        return options.stream().map(DirectCatalogOption::code).toList();
    }
}

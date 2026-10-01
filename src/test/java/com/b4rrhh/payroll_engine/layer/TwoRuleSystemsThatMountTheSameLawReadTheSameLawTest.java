package com.b4rrhh.payroll_engine.layer;

import com.b4rrhh.payroll_engine.execution.domain.port.ItPrestacionTramoRepository;
import com.b4rrhh.payroll_engine.execution.domain.port.SsCotizacionTiposRepository;
import com.b4rrhh.payroll_engine.execution.domain.port.SsCotizacionTopesRepository;
import com.b4rrhh.payroll_engine.execution.domain.port.SsDesempleoModalidadRepository;
import com.b4rrhh.payroll_engine.execution.domain.port.SsTarifaPrimasAtRepository;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * backend#159, paso 5 del camino 5 (workspace#20, ADR-077): la ley de nómina vive en la capa 4 y la
 * comparte cualquier reglamentación que monte esa capa. {@code ES2} es el caso de la puerta del
 * camino —misma ley, otro esquema de empresa—: monta {@code COM}, {@code INT}, {@code ESP} y
 * {@code NOM_ESP}, y su propia capa 5. Sin una sola fila de ley sembrada para ella, lee por los cinco
 * puertos exactamente lo que lee ESP. Y una reglamentación con otra capa 4 no lee nada de la de ESP.
 */
@TestSobreEsquemaReal
class TwoRuleSystemsThatMountTheSameLawReadTheSameLawTest {

    private static final LocalDate MARCH_2026 = LocalDate.of(2026, 3, 15);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SsCotizacionTopesRepository topes;

    @Autowired
    private SsCotizacionTiposRepository tipos;

    @Autowired
    private SsTarifaPrimasAtRepository tarifaAt;

    @Autowired
    private SsDesempleoModalidadRepository modalidades;

    @Autowired
    private ItPrestacionTramoRepository tramosIt;

    @BeforeEach
    void aSecondRuleSystemWithTheSpanishLawAndItsOwnCompanyScheme() {
        jdbcTemplate.update("""
                with rs as (
                    insert into rulesystem.rule_system (code, name, country_code) values ('ES2', 'España 2', 'ESP')
                    returning code
                ), own as (
                    insert into rulesystem.layer (code, name, level) values ('NOM_ES2_EMP', 'Nómina de empresa · España 2', 5)
                    returning code
                )
                insert into rulesystem.rule_system_layer (rule_system_code, level, layer_code)
                select rs.code, v.level, v.layer_code
                  from rs, (values (1, 'COM'), (2, 'INT'), (3, 'ESP'), (4, 'NOM_ESP'), (5, 'NOM_ES2_EMP'))
                          as v(level, layer_code)
                 where exists (select 1 from own)
                """);
        jdbcTemplate.execute("set constraints all immediate");
    }

    @Test
    void theLimitsAreTheSame() {
        assertThat(topes.findActive("ES2", "01", "MENSUAL", "COMUNES", MARCH_2026))
                .isPresent()
                .isEqualTo(topes.findActive("ESP", "01", "MENSUAL", "COMUNES", MARCH_2026));
        assertThat(topes.findActive("ES2", "01", "MENSUAL", "COMUNES", MARCH_2026).orElseThrow().baseMax())
                .isEqualByComparingTo(new BigDecimal("5101.20"));
    }

    @Test
    void theRatesTheAtTariffTheUnemploymentModalityAndTheSickLeaveTranchesAreTheSame() {
        assertThat(tipos.findRate("ES2", "CC_TRAB", MARCH_2026))
                .isPresent().isEqualTo(tipos.findRate("ESP", "CC_TRAB", MARCH_2026));
        assertThat(tarifaAt.findForCnae("ES2", "4711", MARCH_2026))
                .isPresent().isEqualTo(tarifaAt.findForCnae("ESP", "4711", MARCH_2026));
        assertThat(modalidades.findModalidad("ES2", "401", MARCH_2026))
                .contains("DETERMINADA").isEqualTo(modalidades.findModalidad("ESP", "401", MARCH_2026));
        assertThat(tramosIt.findActive("ES2", "IT_COMMON", "DELEGADO_75", MARCH_2026))
                .isPresent().isEqualTo(tramosIt.findActive("ESP", "IT_COMMON", "DELEGADO_75", MARCH_2026));
    }

    // FRA monta su propia capa 4, vacía: la ley española no se le cuela por compartir COM e INT.
    @Test
    void aRuleSystemWithAnotherLawLayerReadsNoneOfIt() {
        assertThat(topes.findActive("FRA", "01", "MENSUAL", "COMUNES", MARCH_2026)).isEmpty();
        assertThat(tipos.findRate("FRA", "CC_TRAB", MARCH_2026)).isEmpty();
    }
}

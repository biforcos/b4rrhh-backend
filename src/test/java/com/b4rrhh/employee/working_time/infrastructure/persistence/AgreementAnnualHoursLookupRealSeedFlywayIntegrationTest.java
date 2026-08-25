package com.b4rrhh.employee.working_time.infrastructure.persistence;

import com.b4rrhh.support.EsquemaRealInitializer;

import com.b4rrhh.rulesystem.agreementprofile.infrastructure.persistence.AgreementCatalogLookupAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false"
})
// El esquema no lo declara el test: es el de produccion, aplicado por Flyway
// una vez y clonado para este contexto (ver EsquemaReal). La semilla del
// convenio real (V61) viene incluida; antes este test montaba su propia base
// con un subconjunto congelado de migraciones.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = EsquemaRealInitializer.class)
@Import({AgreementAnnualHoursLookupAdapter.class, AgreementCatalogLookupAdapter.class})
class AgreementAnnualHoursLookupRealSeedFlywayIntegrationTest {

    private static final String REAL_AGREEMENT_CODE = "99002405011982";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AgreementAnnualHoursLookupAdapter adapter;

    @Test
    void flywaySeedCreatesRealAgreementProfileAndLookupResolvesAnnualHours() {
        Integer agreementCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from rulesystem.rule_entity
                where rule_system_code = ?
                  and rule_entity_type_code = 'AGREEMENT'
                  and code = ?
                """,
                Integer.class,
                "ESP",
                REAL_AGREEMENT_CODE
        );
        Integer profileCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from rulesystem.agreement_profile profile
                join rulesystem.rule_entity agreement on agreement.id = profile.agreement_rule_entity_id
                where agreement.rule_system_code = ?
                  and agreement.rule_entity_type_code = 'AGREEMENT'
                  and agreement.code = ?
                """,
                Integer.class,
                "ESP",
                REAL_AGREEMENT_CODE
        );
        BigDecimal annualHours = jdbcTemplate.queryForObject(
                """
                select profile.annual_hours
                from rulesystem.agreement_profile profile
                join rulesystem.rule_entity agreement on agreement.id = profile.agreement_rule_entity_id
                where agreement.rule_system_code = ?
                  and agreement.rule_entity_type_code = 'AGREEMENT'
                  and agreement.code = ?
                """,
                BigDecimal.class,
                "ESP",
                REAL_AGREEMENT_CODE
        );

        BigDecimal resolvedAnnualHours = adapter.resolveAnnualHours("ESP", REAL_AGREEMENT_CODE);

        assertEquals(1, agreementCount);
        assertEquals(1, profileCount);
        assertEquals(0, new BigDecimal("1736.00").compareTo(annualHours));
        assertEquals(0, new BigDecimal("1736.00").compareTo(resolvedAnnualHours));
    }
}
package com.b4rrhh.employee.working_time.infrastructure.persistence;

import com.b4rrhh.support.TestSobreEsquemaReal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestSobreEsquemaReal
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
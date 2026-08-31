package com.b4rrhh.employee.working_time.infrastructure.persistence;

import com.b4rrhh.rulesystem.agreementprofile.infrastructure.persistence.AgreementProfileEntity;
import com.b4rrhh.rulesystem.infrastructure.persistence.RuleEntityEntity;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestSobreEsquemaReal
// Antes esto corria contra H2 con un DDL a mano. Ahora el esquema es el real,
// con sus semillas: el convenio 99002405011982 ya viene sembrado con su perfil
// (V61), asi que aqui se usan codigos propios sin sembrar; el convenio real
// tiene su test en AgreementAnnualHoursLookupRealSeedFlywayIntegrationTest.
class AgreementAnnualHoursLookupAdapterTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String AGREEMENT_CODE = "ZAGR_CON_PERFIL";
    private static final String AGREEMENT_WITHOUT_PROFILE_CODE = "ZAGR_SIN_PERFIL";

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AgreementAnnualHoursLookupAdapter adapter;

    @Test
    void resolveAnnualHoursReturnsConfiguredHoursForPersistedRealisticAgreementCode() {
    RuleEntityEntity agreement = agreementEntity(
        AGREEMENT_CODE,
        "Convenio colectivo general de centros y servicios de atencion a personas con discapacidad"
    );
        entityManager.persist(agreement);
        entityManager.flush();

    AgreementProfileEntity profile = profileEntity(agreement.getId(), AGREEMENT_CODE,
                "Convenio colectivo general centros atencion personas con discapacidad",
                new BigDecimal("1736.00"));
        entityManager.persist(profile);
        entityManager.flush();

    BigDecimal result = adapter.resolveAnnualHours(RULE_SYSTEM_CODE, AGREEMENT_CODE);

        assertEquals(0, new BigDecimal("1736.00").compareTo(result));
    }

    @Test
    void resolveAnnualHoursThrowsWhenAgreementNotFound() {
        assertThrows(IllegalStateException.class,
            () -> adapter.resolveAnnualHours(RULE_SYSTEM_CODE, "UNKNOWN_CODE"),
                "Should throw when agreement is not in catalog");
    }

    @Test
    void resolveAnnualHoursThrowsWhenProfileMissingForKnownAgreement() {
        RuleEntityEntity agreement = agreementEntity(
            AGREEMENT_WITHOUT_PROFILE_CODE,
            "Convenio sin agreement_profile"
        );
        entityManager.persist(agreement);
        entityManager.flush();

        assertThrows(IllegalStateException.class,
            () -> adapter.resolveAnnualHours(RULE_SYSTEM_CODE, AGREEMENT_WITHOUT_PROFILE_CODE),
                "Should throw when agreement exists but has no profile configured");
    }

    private RuleEntityEntity agreementEntity(String code, String name) {
        RuleEntityEntity entity = new RuleEntityEntity();
        entity.setRuleSystemCode(RULE_SYSTEM_CODE);
        entity.setRuleEntityTypeCode("AGREEMENT");
        entity.setCode(code);
        entity.setName(name);
        entity.setActive(true);
        entity.setStartDate(LocalDate.of(2023, 1, 1));
        return entity;
    }

    private AgreementProfileEntity profileEntity(Long ruleEntityId, String officialNumber,
                                                  String displayName, BigDecimal annualHours) {
        AgreementProfileEntity entity = new AgreementProfileEntity();
        entity.setAgreementRuleEntityId(ruleEntityId);
        entity.setOfficialAgreementNumber(officialNumber);
        entity.setDisplayName(displayName);
        entity.setAnnualHours(annualHours);
        entity.setIsActive(true);
        return entity;
    }
}


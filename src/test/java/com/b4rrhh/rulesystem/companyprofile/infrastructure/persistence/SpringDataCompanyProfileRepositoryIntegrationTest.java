package com.b4rrhh.rulesystem.companyprofile.infrastructure.persistence;

import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestSobreEsquemaReal
class SpringDataCompanyProfileRepositoryIntegrationTest {

    @Autowired
    private SpringDataCompanyProfileRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void enforcesUniqueConstraintPerCompanyRuleEntity() {
        Long empresa = empresaDePrueba("TST_CO_1");
        repository.saveAndFlush(profileEntity(empresa, "Acme Spain SA"));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(profileEntity(empresa, "Another Legal Name"))
        );
    }

    @Test
    void findsByCompanyRuleEntityId() {
        Long empresa = empresaDePrueba("TST_CO_2");
        repository.saveAndFlush(profileEntity(empresa, "Acme Spain SA"));

        assertTrue(repository.findByCompanyRuleEntityId(empresa).isPresent());
    }

    /**
     * El perfil tiene clave ajena a rulesystem.rule_entity. El DDL a mano no la
     * declaraba, y por eso valia cualquier id inventado.
     */
    private Long empresaDePrueba(String code) {
        return DatosDePrueba.ruleEntity(
                jdbcTemplate, "COMPANY", code, "Empresa de prueba", LocalDate.of(2020, 1, 1), null);
    }

    private CompanyProfileEntity profileEntity(Long companyRuleEntityId, String legalName) {
        CompanyProfileEntity entity = new CompanyProfileEntity();
        entity.setCompanyRuleEntityId(companyRuleEntityId);
        entity.setLegalName(legalName);
        return entity;
    }
}
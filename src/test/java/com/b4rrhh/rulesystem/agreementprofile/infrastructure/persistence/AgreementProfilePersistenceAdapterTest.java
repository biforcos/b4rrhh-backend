package com.b4rrhh.rulesystem.agreementprofile.infrastructure.persistence;

import com.b4rrhh.rulesystem.agreementprofile.domain.model.AgreementProfile;
import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// La sexta clase con H2, y la que el grep no veia: no escribia jdbc:h2 en
// ningun sitio porque dejaba que @DataJpaTest sustituyera el datasource por
// "la base embebida que haya en el classpath". Al borrar el jar del pom dejo
// de arrancar (#2).
//
// El esquema que H2 sacaba del mapeo JPA no tenia la clave ajena que
// produccion si tiene: agreement_profile.agreement_rule_entity_id ->
// rule_entity(id) (V59). Por eso un 1L fijo colaba. Aqui el convenio se crea,
// y el que "no existe" es un id fuera de rango.
@TestSobreEsquemaReal
class AgreementProfilePersistenceAdapterTest {

    private static final long CONVENIO_INEXISTENTE = 999_999_999L;

    @Autowired
    private AgreementProfilePersistenceAdapter adapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long agreementRuleEntityId;

    @BeforeEach
    void convenioSinPerfil() {
        agreementRuleEntityId = DatosDePrueba.ruleEntity(
                jdbcTemplate, "AGREEMENT", "TST-CA-2024", "Convenio de prueba",
                LocalDate.of(2024, 1, 1), null);
    }

    @Test
    void savesPersistsAgreementProfileByAgreementRuleEntityId() {
        AgreementProfile profile = new AgreementProfile(
                "CA-2024-001",
                "Collective Agreement 2024",
                "CA-2024",
                new BigDecimal("1560.00"),
                true
        );

        AgreementProfile saved = adapter.save(agreementRuleEntityId, profile);

        assertNotNull(saved);
        assertEquals("CA-2024-001", saved.getOfficialAgreementNumber());
        assertEquals("Collective Agreement 2024", saved.getDisplayName());
        assertEquals(0, new BigDecimal("1560.00").compareTo(saved.getAnnualHours()));
        assertTrue(saved.isActive());
    }

    @Test
    void findByAgreementRuleEntityIdReturnsProfileWhenExists() {
        AgreementProfile profile = new AgreementProfile(
                "CA-2024-001",
                "Collective Agreement 2024",
                "CA-2024",
                new BigDecimal("1560.00"),
                true
        );
        adapter.save(agreementRuleEntityId, profile);

        Optional<AgreementProfile> found = adapter.findByAgreementRuleEntityId(agreementRuleEntityId);

        assertTrue(found.isPresent());
        assertEquals("CA-2024-001", found.get().getOfficialAgreementNumber());
    }

    @Test
    void findByAgreementRuleEntityIdReturnsEmptyWhenNotFound() {
        Optional<AgreementProfile> found = adapter.findByAgreementRuleEntityId(CONVENIO_INEXISTENTE);

        assertTrue(found.isEmpty());
    }

    @Test
    void updateModifiesExistingProfile() {
        AgreementProfile original = new AgreementProfile(
                "CA-2024-001",
                "Original Name",
                "ORIG",
                new BigDecimal("1560.00"),
                true
        );
        adapter.save(agreementRuleEntityId, original);

        AgreementProfile updated = original.update(
                "CA-2024-002",
                "Updated Name",
                "UPD",
                new BigDecimal("1680.00"),
                true
        );
        adapter.save(agreementRuleEntityId, updated);

        Optional<AgreementProfile> fetched = adapter.findByAgreementRuleEntityId(agreementRuleEntityId);
        assertTrue(fetched.isPresent());
        assertEquals("CA-2024-002", fetched.get().getOfficialAgreementNumber());
        assertEquals("Updated Name", fetched.get().getDisplayName());
        assertEquals(0, new BigDecimal("1680.00").compareTo(fetched.get().getAnnualHours()));
    }
}

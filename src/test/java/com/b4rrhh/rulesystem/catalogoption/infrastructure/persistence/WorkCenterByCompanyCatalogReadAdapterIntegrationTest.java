package com.b4rrhh.rulesystem.catalogoption.infrastructure.persistence;

import com.b4rrhh.rulesystem.catalogoption.domain.model.WorkCenterByCompanyOption;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import com.b4rrhh.rulesystem.workcenter.infrastructure.persistence.SpringDataWorkCenterProfileRepository;
import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestSobreEsquemaReal
// Aqui el DDL a mano era ademas enganoso: creaba rule_entity sin la clave
// ajena de work_center_profile y daba por hecho que los ids serian 1, 2 y 3.
// Era el test de la consulta nativa de SpringDataWorkCenterProfileRepository; desde backend#157 los
// centros los resuelve el puerto de entidades y el perfil sólo dice de qué empresa son.
class WorkCenterByCompanyCatalogReadAdapterIntegrationTest {

    @Autowired
    private RuleEntityRepository ruleEntityRepository;

    @Autowired
    private SpringDataWorkCenterProfileRepository profileRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpFixtures() {
        // Los codigos y la empresa son de este test: las migraciones siembran
        // MAIN_OFFICE, BRANCH_* y REMOTE para las empresas ES01 y ES02, asi que
        // no se pisan con estos.
        Long madrid = centro("MADRID_01", "Madrid 01", LocalDate.of(2020, 1, 1), null);
        Long barcelona = centro("BARCELONA_01", "Barcelona 01", LocalDate.of(2020, 1, 1), null);
        Long antiguo = centro("LEGACY_01", "Legacy 01", LocalDate.of(2010, 1, 1), LocalDate.of(2020, 12, 31));

        perfil(madrid, "COMP");
        perfil(barcelona, "OTHER");
        perfil(antiguo, "COMP");
    }

    private Long centro(String code, String name, LocalDate startDate, LocalDate endDate) {
        return DatosDePrueba.ruleEntity(jdbcTemplate, "WORK_CENTER", code, name, startDate, endDate);
    }

    private void perfil(Long workCenterRuleEntityId, String companyCode) {
        jdbcTemplate.update("""
                insert into rulesystem.work_center_profile (work_center_rule_entity_id, company_code)
                values (?, ?)
                """, workCenterRuleEntityId, companyCode);
    }

    @Test
    void findsApplicableWorkCentersForCompanyAndReferenceDate() {
        List<WorkCenterByCompanyOption> rows =
                new WorkCenterByCompanyCatalogReadAdapter(ruleEntityRepository, profileRepository).findByCompany(
                        "ESP",
                        "COMP",
                        LocalDate.of(2026, 4, 15),
                        null
                );

        assertEquals(1, rows.size());
        assertEquals("MADRID_01", rows.get(0).code());
        assertEquals("Madrid 01", rows.get(0).name());
    }
}

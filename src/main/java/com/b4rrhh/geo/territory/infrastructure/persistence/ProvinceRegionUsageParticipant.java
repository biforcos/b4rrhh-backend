package com.b4rrhh.geo.territory.infrastructure.persistence;

import com.b4rrhh.rulesystem.application.port.RuleEntityUsageParticipant;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Las provincias que cuelgan de una comunidad (backend#168). Ninguna dirección guarda la comunidad:
 * sale de su provincia ({@code province_profile.region_code}, V174), y es eso lo que se rompería al
 * borrar una comunidad con provincias. Las provincias de la reglamentación las da el puerto de las
 * entidades ({@code NobodyReadsRuleEntityOutsideItsPortTest}); aquí sólo se leen sus perfiles.
 */
@Component
public class ProvinceRegionUsageParticipant implements RuleEntityUsageParticipant {

    private static final String REGION = "REGION";
    private static final String PROVINCE = "PROVINCE";

    private final NamedParameterJdbcTemplate jdbc;
    private final RuleEntityRepository ruleEntities;

    public ProvinceRegionUsageParticipant(JdbcTemplate jdbcTemplate, RuleEntityRepository ruleEntities) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.ruleEntities = ruleEntities;
    }

    @Override
    public String resource() {
        return "provinces";
    }

    @Override
    public long countReferences(String ruleSystemCode, String ruleEntityTypeCode, String code) {
        if (!REGION.equals(ruleEntityTypeCode)) {
            return 0;
        }
        List<Long> provinces = ruleEntities.findByFilters(ruleSystemCode, PROVINCE, null, null, null)
                .stream().map(RuleEntity::getId).toList();
        if (provinces.isEmpty()) {
            return 0;
        }
        Long count = jdbc.queryForObject("""
                select count(*) from rulesystem.province_profile
                 where province_rule_entity_id in (:provinces) and region_code = :region
                """, new MapSqlParameterSource("provinces", provinces).addValue("region", code), Long.class);
        return count == null ? 0 : count;
    }
}

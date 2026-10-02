package com.b4rrhh.geo.territory.infrastructure.catalog;

import com.b4rrhh.geo.territory.application.port.TerritoryCatalogPort;
import com.b4rrhh.geo.territory.domain.model.Country;
import com.b4rrhh.geo.territory.domain.model.Province;
import com.b4rrhh.geo.territory.domain.model.Region;
import com.b4rrhh.geo.territory.domain.model.StreetType;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import com.b4rrhh.rulesystem.domain.port.RuleSystemRepository;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.text.Collator;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * El territorio que es catálogo (V174, backend#158). Las entidades se piden al puerto, que es
 * quien sabe qué capa monta cada reglamentación en cada nivel ({@code NobodyReadsRuleEntityOutsideItsPortTest});
 * aquí sólo se leen los perfiles, por el id de la entidad.
 */
@Component
public class TerritoryCatalogAdapter implements TerritoryCatalogPort {

    private final RuleSystemRepository ruleSystems;
    private final RuleEntityRepository ruleEntities;
    private final RuleEntityLabelResolver labels;
    private final NamedParameterJdbcTemplate jdbc;

    public TerritoryCatalogAdapter(
            RuleSystemRepository ruleSystems,
            RuleEntityRepository ruleEntities,
            RuleEntityLabelResolver labels,
            JdbcTemplate jdbcTemplate
    ) {
        this.ruleSystems = ruleSystems;
        this.ruleEntities = ruleEntities;
        this.labels = labels;
        this.jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public boolean ruleSystemExists(String ruleSystemCode) {
        return ruleSystems.findByCode(ruleSystemCode).isPresent();
    }

    @Override
    public List<Country> countries(String ruleSystemCode, String languageCode) {
        List<RuleEntity> countries = active(ruleSystemCode, "COUNTRY");
        Map<Long, String> names = labels.resolveLabels(countries, languageCode);
        Collator collator = Collator.getInstance(Locale.forLanguageTag("es"));
        return countries.stream()
                .map(country -> new Country(country.getCode(), names.getOrDefault(country.getId(), country.getName())))
                .sorted(Comparator.comparing(Country::name, collator))
                .toList();
    }

    @Override
    public List<Province> provinces(String ruleSystemCode) {
        Map<String, Region> regions = new HashMap<>();
        List<RuleEntity> regionEntities = active(ruleSystemCode, "REGION");
        Map<Long, String> regionIso = profiles("""
                select region_rule_entity_id as id, iso_3166_2 as value
                  from rulesystem.region_profile where region_rule_entity_id in (:ids)
                """, regionEntities);
        for (RuleEntity region : regionEntities) {
            regions.put(region.getCode(), new Region(region.getCode(), region.getName(), regionIso.get(region.getId())));
        }

        List<RuleEntity> provinces = active(ruleSystemCode, "PROVINCE");
        Map<Long, String> provinceIso = profiles("""
                select province_rule_entity_id as id, iso_3166_2 as value
                  from rulesystem.province_profile where province_rule_entity_id in (:ids)
                """, provinces);
        Map<Long, String> provinceRegion = profiles("""
                select province_rule_entity_id as id, region_code as value
                  from rulesystem.province_profile where province_rule_entity_id in (:ids)
                """, provinces);
        return provinces.stream()
                .map(province -> new Province(
                        province.getCode(),
                        province.getName(),
                        provinceIso.get(province.getId()),
                        regions.get(provinceRegion.get(province.getId()))))
                .sorted(Comparator.comparing(Province::code))
                .toList();
    }

    @Override
    public List<StreetType> streetTypes(String ruleSystemCode) {
        return active(ruleSystemCode, "STREET_TYPE").stream()
                .map(type -> new StreetType(type.getCode(), type.getName()))
                .sorted(Comparator.comparing(StreetType::code))
                .toList();
    }

    private List<RuleEntity> active(String ruleSystemCode, String typeCode) {
        return ruleEntities.findByFilters(ruleSystemCode, typeCode, null, true, null);
    }

    private Map<Long, String> profiles(String sql, List<RuleEntity> entities) {
        Map<Long, String> values = new HashMap<>();
        if (entities.isEmpty()) {
            return values;
        }
        List<Long> ids = entities.stream().map(RuleEntity::getId).toList();
        jdbc.query(sql, new MapSqlParameterSource("ids", ids),
                rs -> {
                    values.put(rs.getLong("id"), rs.getString("value"));
                });
        return values;
    }
}

package com.b4rrhh.geo.territory;

import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lo que siembran la V174 y la V176 (backend#154, ADR-078), contra el esquema real: las
 * comunidades, las provincias y los tipos de vía de la capa ESP, y los municipios y códigos
 * postales de {@code geo}, con la vigencia «desde al menos».
 */
@TestSobreEsquemaReal
class TheTerritoryMasterIsSeededTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void theNationalLayerHoldsNineteenRegionsFiftyTwoProvincesAndNinetyThreeStreetTypes() {
        assertThat(count("select count(*) from rulesystem.rule_entity where layer_code = 'ESP' and rule_entity_type_code = 'REGION'"))
                .isEqualTo(19);
        assertThat(count("select count(*) from rulesystem.rule_entity where layer_code = 'ESP' and rule_entity_type_code = 'PROVINCE'"))
                .isEqualTo(52);
        assertThat(count("select count(*) from rulesystem.rule_entity where layer_code = 'ESP' and rule_entity_type_code = 'STREET_TYPE'"))
                .isEqualTo(93);
        assertThat(count("select count(*) from rulesystem.rule_entity_type where code in ('REGION', 'PROVINCE', 'STREET_TYPE') and level = 3"))
                .isEqualTo(3);
    }

    @Test
    void everyProvinceHasARegionOfItsOwnLayer() {
        assertThat(count("""
                select count(*)
                  from rulesystem.rule_entity p
                  join rulesystem.province_profile pp on pp.province_rule_entity_id = p.id
                  join rulesystem.rule_entity r
                    on r.layer_code = p.layer_code and r.rule_entity_type_code = 'REGION' and r.code = pp.region_code
                 where p.rule_entity_type_code = 'PROVINCE'
                """)).isEqualTo(52);
        assertThat(jdbcTemplate.queryForMap("""
                select pp.region_code, pp.iso_3166_2
                  from rulesystem.rule_entity p
                  join rulesystem.province_profile pp on pp.province_rule_entity_id = p.id
                 where p.rule_entity_type_code = 'PROVINCE' and p.code = '46'
                """)).isEqualTo(Map.of("region_code", "10", "iso_3166_2", "ES-V"));
    }

    @Test
    void theMunicipalitiesAreTheEightThousandOfTheIneAndInForceSinceAtLeastTheirFirstRelation() {
        assertThat(count("select count(*) from geo.municipality where country_code = 'ESP'")).isEqualTo(8132);
        assertThat(count("select count(*) from geo.municipality where end_date is not null")).isZero();

        // Lo de la relación de 2021 entra «desde al menos» 2021; Usansolo, desde la de 2024.
        assertThat(jdbcTemplate.queryForObject(
                "select start_date from geo.municipality where code = '46250'", LocalDate.class))
                .isEqualTo(LocalDate.of(2021, 1, 1));
        assertThat(jdbcTemplate.queryForObject(
                "select start_date from geo.municipality where code = '48916'", LocalDate.class))
                .isEqualTo(LocalDate.of(2024, 1, 1));

        // Un renombre no es un municipio nuevo: mismo código, el nombre más reciente.
        assertThat(jdbcTemplate.queryForObject(
                "select name from geo.municipality where code = '24036'", String.class))
                .isEqualTo("Valle de Ancares");
    }

    @Test
    void thePostalCodesSuggestAMunicipalityOfTheirOwnProvinceOrNone() {
        assertThat(count("select count(*) from geo.postal_code where country_code = 'ESP'")).isEqualTo(11150);
        assertThat(count("select count(*) from geo.postal_code where suggested_municipality_code is not null"))
                .isEqualTo(6068);
        assertThat(count("""
                select count(*) from geo.postal_code
                 where left(suggested_municipality_code, 2) <> left(code, 2)
                """)).isZero();
        // El 46250 es L'Alcúdia, y la columna de municipio de GeoNames dice Sagunto.
        assertThat(jdbcTemplate.queryForObject(
                "select suggested_municipality_code from geo.postal_code where code = '46250'", String.class))
                .isEqualTo("46019");
    }

    private long count(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }
}

package com.b4rrhh.geo.address.infrastructure.persistence;

import com.b4rrhh.rulesystem.application.port.RuleEntityUsageParticipant;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

/**
 * Cuenta las direcciones de una tabla que usan un tipo de vía, una provincia o un país (backend#168).
 * Las tres direcciones —empleado, empresa y centro— tienen la misma estructura (backend#155), así
 * que la tabla y de quién es son lo único que cambia.
 *
 * <p>No declara columnas para la guardia de integridad, y es a propósito: una dirección «sin
 * normalizar» puede guardar un tipo de vía o una provincia que no están, y la marca lo dice
 * ({@code EveryCatalogColumnIsDeclaredOrExemptedTest} las exime por eso). Aquí sólo se cuenta, para
 * que borrar uno que sí se usa se niegue en vez de pasar en silencio.</p>
 *
 * <p>Dos maneras de contar, una por cada forma de validar:</p>
 * <ul>
 *   <li><b>Tipo de vía y provincia</b>, por el <b>país de la dirección</b>, que es por donde las
 *   valida el backend#155: el país de la reglamentación que pregunta. Un empleado de FRA que vive en
 *   Valencia usa la «CL» de ESP.</li>
 *   <li><b>País</b>, por la <b>reglamentación de quien tiene la dirección</b>: los países viven en
 *   INT, que montan varias, y el borrado pregunta a cada una y suma; contar por país los contaría
 *   una vez por cada una. De quién es cada empresa o centro lo dice el puerto de las entidades
 *   ({@code NobodyReadsRuleEntityOutsideItsPortTest}): aquí sólo se leen los perfiles por su id.</li>
 * </ul>
 *
 * <p>Tabla y columnas son constantes de las subclases: nada de lo que se interpola en el SQL sale
 * de la petición.</p>
 */
public abstract class PostalAddressRuleEntityUsageParticipant implements RuleEntityUsageParticipant {

    static final String STREET_TYPE = "STREET_TYPE";
    static final String PROVINCE = "PROVINCE";
    static final String COUNTRY = "COUNTRY";

    /** De quién es la dirección: el tipo de la entidad dueña y la columna que la apunta. */
    protected record Owner(String ruleEntityTypeCode, String idColumn) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final RuleEntityRepository ruleEntities;
    private final String resource;
    private final String table;
    private final Owner owner;

    /**
     * @param table la tabla de las direcciones, con su esquema
     * @param owner de quién es cada dirección, para contar el país; null si el país ya lo cuenta el
     *              participante que declara la columna
     */
    protected PostalAddressRuleEntityUsageParticipant(
            JdbcTemplate jdbcTemplate,
            RuleEntityRepository ruleEntities,
            String resource,
            String table,
            Owner owner
    ) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.ruleEntities = ruleEntities;
        this.resource = resource;
        this.table = table;
        this.owner = owner;
    }

    @Override
    public final String resource() {
        return resource;
    }

    /** La tabla que cuenta, con su esquema; para el candado de que ninguna se quede fuera. */
    public final String table() {
        return table;
    }

    /** Si cuenta también el país; si no, lo cuenta el participante que declara la columna. */
    public final boolean countsCountry() {
        return owner != null;
    }

    @Override
    public final long countReferences(String ruleSystemCode, String ruleEntityTypeCode, String code) {
        return switch (ruleEntityTypeCode) {
            case STREET_TYPE -> byCountryOfTheAddress(ruleSystemCode, "street_type_code", code);
            case PROVINCE -> byCountryOfTheAddress(ruleSystemCode, "subdivision_code", code);
            case COUNTRY -> owner == null ? 0 : byOwnerRuleSystem(ruleSystemCode, code);
            default -> 0;
        };
    }

    private long byCountryOfTheAddress(String ruleSystemCode, String column, String code) {
        Long count = jdbc.queryForObject("select count(*) from " + table + " a"
                        + " where a.country_code = (select rs.country_code from rulesystem.rule_system rs where rs.code = :ruleSystem)"
                        + " and a." + column + " = :code",
                new MapSqlParameterSource("ruleSystem", ruleSystemCode).addValue("code", code), Long.class);
        return count == null ? 0 : count;
    }

    private long byOwnerRuleSystem(String ruleSystemCode, String countryCode) {
        List<Long> owners = ruleEntities.findByFilters(ruleSystemCode, owner.ruleEntityTypeCode(), null, null, null)
                .stream().map(RuleEntity::getId).toList();
        if (owners.isEmpty()) {
            return 0;
        }
        Long count = jdbc.queryForObject("select count(*) from " + table + " a"
                        + " where a." + owner.idColumn() + " in (:owners) and a.country_code = :code",
                new MapSqlParameterSource("owners", owners).addValue("code", countryCode), Long.class);
        return count == null ? 0 : count;
    }
}

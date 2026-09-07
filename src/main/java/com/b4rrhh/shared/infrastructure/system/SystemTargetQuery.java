package com.b4rrhh.shared.infrastructure.system;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Construye el SystemTarget preguntandoselo al servidor, no a la configuracion.
 *
 * Va por SQL directo y no por los repositorios de dominio por lo mismo que
 * DemoCountsQuery: no es una lectura de negocio. Y el nombre de la base sale de
 * current_database() —lo dice el propio Postgres— en vez de leerse de
 * spring.datasource.url, que es lo que le pedimos que hiciera y no lo que hizo.
 */
@Component
public class SystemTargetQuery {

    private final JdbcTemplate jdbcTemplate;

    public SystemTargetQuery(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SystemTarget target() {
        return new SystemTarget(database(), schemaVersion(), employees());
    }

    private String database() {
        String name = jdbcTemplate.queryForObject("select current_database()", String.class);
        return hostAndPortFrom(jdbcUrl()) + "/" + name;
    }

    /**
     * La ultima migracion aplicada con exito. Se ordena por installed_rank y no
     * por version porque version es texto: '99' iria despues de '120'. Las
     * repetibles no tienen version y quedan fuera.
     */
    private String schemaVersion() {
        List<String> versions = jdbcTemplate.queryForList(
                "select version from public.flyway_schema_history "
                        + "where success = true and version is not null "
                        + "order by installed_rank desc limit 1",
                String.class);
        return versions.isEmpty() ? null : versions.getFirst();
    }

    private long employees() {
        Long total = jdbcTemplate.queryForObject("select count(*) from employee.employee", Long.class);
        return total == null ? 0 : total;
    }

    private String jdbcUrl() {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            return connection.getMetaData().getURL();
        } catch (SQLException e) {
            throw new IllegalStateException("No se puede leer la URL de la conexion a la base", e);
        }
    }

    /**
     * El host y el puerto de una URL JDBC, sin nada mas.
     *
     * Se corta por el '?' antes de mirar nada: los parametros de una URL JDBC
     * pueden llevar la contrasena, y esta respuesta la lee un cliente. Si la
     * URL no tiene la forma esperada se devuelve '?' en vez de adivinar: un
     * host inventado seria peor que uno que no se sabe, porque compararia bien.
     */
    static String hostAndPortFrom(String jdbcUrl) {
        if (jdbcUrl == null) {
            return "?";
        }
        int parameterStart = jdbcUrl.indexOf('?');
        String withoutParameters = parameterStart < 0 ? jdbcUrl : jdbcUrl.substring(0, parameterStart);
        int authorityStart = withoutParameters.indexOf("//");
        if (authorityStart < 0) {
            return "?";
        }
        String authority = withoutParameters.substring(authorityStart + 2);
        int databaseStart = authority.indexOf('/');
        String hostAndPort = databaseStart < 0 ? authority : authority.substring(0, databaseStart);
        return hostAndPort.isBlank() ? "?" : hostAndPort;
    }
}

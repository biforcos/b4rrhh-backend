package com.b4rrhh.shared.infrastructure.system;

import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Los tres campos contra el esquema real (workforce-loader#8).
 *
 * Importa que sea el esquema real y no uno de test: dos de los tres campos
 * salen de sitios que solo existen ahi —public.flyway_schema_history y
 * employee.employee—, y el valor del endpoint es justamente que no mienta.
 * Si alguien mueve cualquiera de las dos, revienta aqui.
 *
 * La consulta se instancia a mano, como DemoCountsQuery: el contexto
 * compartido de los tests no la lleva.
 */
@TestSobreEsquemaReal
class SystemTargetQueryIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void namesTheDatabaseTheBackendIsActuallyConnectedTo() {
        String expectedName = jdbcTemplate.queryForObject("select current_database()", String.class);

        SystemTarget target = new SystemTargetQuery(jdbcTemplate).target();

        assertNotNull(expectedName);
        assertTrue(
                target.database().endsWith("/" + expectedName),
                "Esperaba que la base terminara en /" + expectedName + " y fue " + target.database());
        assertTrue(
                target.database().length() > expectedName.length() + 1,
                "La base tiene que llevar tambien donde vive, no solo el nombre: " + target.database());
    }

    @Test
    void reportsTheLastAppliedMigration() {
        String expectedVersion = jdbcTemplate.queryForObject(
                "select version from public.flyway_schema_history "
                        + "where success = true and version is not null "
                        + "order by installed_rank desc limit 1",
                String.class);

        SystemTarget target = new SystemTargetQuery(jdbcTemplate).target();

        assertNotNull(expectedVersion, "El esquema real se construye con Flyway: su historia tiene que estar ahi");
        assertEquals(expectedVersion, target.schemaVersion());
    }

    @Test
    void countsTheEmployeesThatAreAlreadyThere() {
        SystemTargetQuery query = new SystemTargetQuery(jdbcTemplate);
        long before = query.target().employees();

        DatosDePrueba.empleado(jdbcTemplate);

        assertEquals(before + 1, query.target().employees());
    }
}

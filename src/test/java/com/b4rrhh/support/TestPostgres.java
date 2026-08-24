package com.b4rrhh.support;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Un unico Postgres para toda la ejecucion de la suite.
 *
 * Se arranca la primera vez que alguien toca esta clase y vive hasta que
 * termina la JVM: Testcontainers deja un contenedor centinela (Ryuk) que se
 * encarga de matarlo aunque el build reviente por la mitad.
 *
 * Es un singleton a proposito. Levantar un Postgres por clase de test seria
 * mas puro, pero cuesta segundos cada vez; el aislamiento real ya lo dan los
 * propios tests, que se crean una base de datos con nombre aleatorio.
 *
 * La imagen es la misma que usa docker/postgres/docker-compose.yaml. Si un dia
 * subes una, sube la otra: que los tests pasen contra una version distinta de
 * la que corre en produccion es exactamente el fallo que esto viene a evitar.
 *
 * Si en el entorno hay TEST_DB_HOST, esta clase no arranca nada y usa el
 * Postgres que le digan. Es lo que hace el pipeline: ver la nota de abajo.
 */
public final class TestPostgres {

    /**
     * Postgres puesto por quien lanza la suite. Se activa poniendo TEST_DB_HOST
     * en el entorno; entonces no se arranca ningun contenedor.
     *
     * Existe por el pipeline. El runner de Gitea es dind: los puertos que
     * Testcontainers publicaria quedan en el demonio, no en el contenedor del
     * job, y el job no llega a ellos. Levantando el Postgres en la misma red
     * del job y pasando aqui su nombre, se hablan por el DNS de Docker y no
     * hace falta que ningun puerto salga a ninguna parte.
     *
     * En tu maquina no pongas nada: sin la variable, todo sigue igual.
     */
    private static final String HOST_EXTERNO = variable("TEST_DB_HOST", null);

    private static final PostgreSQLContainer<?> CONTAINER = arrancarSiHaceFalta();

    private static PostgreSQLContainer<?> arrancarSiHaceFalta() {
        if (HOST_EXTERNO != null) {
            return null;
        }
        PostgreSQLContainer<?> contenedor = new PostgreSQLContainer<>("postgres:16")
                .withDatabaseName("b4rrhh")
                .withUsername("b4rrhh")
                .withPassword("b4rrhh")
                // Margen sobre el limite por defecto (100). No sustituye a
                // limitar el pool: es el colchon para cuando la suite crezca.
                .withCommand("postgres", "-c", "max_connections=300");
        contenedor.start();
        return contenedor;
    }

    private TestPostgres() {
    }

    public static String host() {
        return CONTAINER == null ? HOST_EXTERNO : CONTAINER.getHost();
    }

    public static int port() {
        return CONTAINER == null
                ? Integer.parseInt(variable("TEST_DB_PORT", "5432"))
                : CONTAINER.getFirstMappedPort();
    }

    public static String username() {
        return CONTAINER == null ? variable("TEST_DB_USERNAME", "b4rrhh") : CONTAINER.getUsername();
    }

    public static String password() {
        return CONTAINER == null ? variable("TEST_DB_PASSWORD", "b4rrhh") : CONTAINER.getPassword();
    }

    /** La base que ya existe al arrancar; sirve de base administrativa. */
    public static String defaultDatabase() {
        return CONTAINER == null ? variable("TEST_DB_NAME", "b4rrhh") : CONTAINER.getDatabaseName();
    }

    /** Vacia y sin definir son lo mismo: en un YAML es facil dejar una a medias. */
    private static String variable(String nombre, String pordefecto) {
        String valor = System.getenv(nombre);
        return valor == null || valor.isBlank() ? pordefecto : valor;
    }

    public static String jdbcUrl(String database) {
        return "jdbc:postgresql://" + host() + ":" + port() + "/" + database;
    }
}

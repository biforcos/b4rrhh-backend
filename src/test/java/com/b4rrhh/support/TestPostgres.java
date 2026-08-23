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
 */
public final class TestPostgres {

    private static final PostgreSQLContainer<?> CONTAINER =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("b4rrhh")
                    .withUsername("b4rrhh")
                    .withPassword("b4rrhh");

    static {
        CONTAINER.start();
    }

    private TestPostgres() {
    }

    public static String host() {
        return CONTAINER.getHost();
    }

    public static int port() {
        return CONTAINER.getFirstMappedPort();
    }

    public static String username() {
        return CONTAINER.getUsername();
    }

    public static String password() {
        return CONTAINER.getPassword();
    }

    /** La base que crea la imagen al arrancar; sirve de base administrativa. */
    public static String defaultDatabase() {
        return CONTAINER.getDatabaseName();
    }

    public static String jdbcUrl(String database) {
        return "jdbc:postgresql://" + host() + ":" + port() + "/" + database;
    }
}

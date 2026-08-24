package com.b4rrhh.support;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Da a cada contexto de test su propia base de datos, virgen, dentro del
 * Postgres efimero de TestPostgres.
 *
 * Por que una base por contexto y no una compartida: estos tests no aplican el
 * esquema completo. Cada uno copia un SUBCONJUNTO de migraciones a un
 * directorio temporal y apunta spring.flyway.locations ahi. Sobre una base
 * compartida, el segundo en arrancar se encontraria un flyway_schema_history
 * con versiones que el no conoce y tablas que ya existen.
 *
 * Nadie borra estas bases: el contenedor entero muere al terminar la JVM y se
 * las lleva. Crear una base vacia son milisegundos.
 */
public class TestPostgresInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        String base = "test_" + SECUENCIA.incrementAndGet() + "_" + Long.toHexString(System.nanoTime());
        crearBase(base);

        TestPropertyValues.of(
                "spring.datasource.url=" + TestPostgres.jdbcUrl(base),
                "spring.datasource.username=" + TestPostgres.username(),
                "spring.datasource.password=" + TestPostgres.password(),
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                // Spring cachea los contextos de test durante TODA la suite, y
                // cada uno se queda con su pool. Con el tamano por defecto (10)
                // y ~17 contextos, Postgres se queda sin conexiones y falla con
                // "sorry, too many clients already". Los tests van en serie:
                // con dos conexiones sobra, y minimum-idle=0 las suelta cuando
                // el contexto esta parado.
                "spring.datasource.hikari.maximum-pool-size=2",
                "spring.datasource.hikari.minimum-idle=0"
        ).applyTo(applicationContext.getEnvironment());
    }

    private void crearBase(String nombre) {
        String urlAdmin = TestPostgres.jdbcUrl(TestPostgres.defaultDatabase());
        try (Connection conexion = DriverManager.getConnection(
                     urlAdmin, TestPostgres.username(), TestPostgres.password());
             Statement sentencia = conexion.createStatement()) {
            // El nombre lo genera esta clase, no viene de fuera: no hay
            // interpolacion de nada que no controlemos.
            sentencia.execute("CREATE DATABASE " + nombre);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo crear la base de test " + nombre, e);
        }
    }
}

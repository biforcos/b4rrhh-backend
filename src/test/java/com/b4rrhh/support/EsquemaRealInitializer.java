package com.b4rrhh.support;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Da a cada contexto de test una base con el esquema real de produccion.
 *
 * Es el hermano de TestPostgresInitializer: aquel entrega una base vacia, para
 * los tests que se aplican ellos mismos un subconjunto de migraciones; este
 * entrega una copia de EsquemaReal, con todas las tablas y todas las semillas.
 *
 * Sigue siendo una base por contexto: los tests que no van en transaccion
 * (los de atomicidad) dejan filas escritas, y no deben verlas los demas.
 */
public class EsquemaRealInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        String base = "real_" + SECUENCIA.incrementAndGet() + "_" + Long.toHexString(System.nanoTime());
        TestPostgres.crearBase(base, EsquemaReal.plantilla());

        TestPropertyValues.of(
                "spring.datasource.url=" + TestPostgres.jdbcUrl(base),
                "spring.datasource.username=" + TestPostgres.username(),
                "spring.datasource.password=" + TestPostgres.password(),
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                // Mismo motivo que en TestPostgresInitializer: los contextos se
                // cachean durante toda la suite y cada uno se queda con su pool.
                "spring.datasource.hikari.maximum-pool-size=2",
                "spring.datasource.hikari.minimum-idle=0"
        ).applyTo(applicationContext.getEnvironment());
    }
}

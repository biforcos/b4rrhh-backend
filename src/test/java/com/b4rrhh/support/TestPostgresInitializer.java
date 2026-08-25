package com.b4rrhh.support;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Da a cada contexto de test su propia base de datos, virgen, dentro del
 * Postgres efimero de TestPostgres.
 *
 * Por que una base por contexto y no una compartida: estos tests se crean el
 * esquema ellos mismos, con DDL a mano en @BeforeEach (issue #1). Sobre una
 * base compartida, el segundo en arrancar se encontraria tablas que ya existen.
 *
 * La base se borra cuando Spring cierra el contexto (ver BaseDeTest).
 */
public class TestPostgresInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        String base = "test_" + SECUENCIA.incrementAndGet() + "_" + Long.toHexString(System.nanoTime());
        TestPostgres.crearBase(base, null);
        BaseDeTest.conectar(applicationContext, base, () -> TestPostgres.borrarBase(base));
    }
}

package com.b4rrhh.support;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Da a cada contexto de test una base con el esquema real de produccion.
 *
 * Es el hermano de TestPostgresInitializer: aquel entrega una base vacia, para
 * los tests que se aplican ellos mismos el esquema; este entrega una copia de
 * EsquemaReal, con todas las tablas y todas las semillas.
 *
 * Sigue siendo una base por contexto: los tests que no van en transaccion
 * (los de atomicidad) dejan filas escritas, y no deben verlas los demas. Por
 * eso mismo importa que los tests compartan contexto siempre que puedan (ver
 * TestSobreEsquemaReal): cada contexto distinto es un clon mas.
 *
 * La base se borra cuando Spring cierra el contexto (ver BaseDeTest).
 */
public class EsquemaRealInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        String base = "real_" + SECUENCIA.incrementAndGet() + "_" + Long.toHexString(System.nanoTime());
        EsquemaReal.clon(base);
        BaseDeTest.conectar(applicationContext, base, () -> EsquemaReal.clonCerrado(base));
    }
}

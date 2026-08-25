package com.b4rrhh.support;

import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Lo que comparten los dos initializers: apuntar el datasource del contexto a
 * una base y asegurarse de que la base muere con el contexto.
 *
 * El borrado se registra como DisposableBean ANTES del refresh. Spring
 * destruye los singletons en orden inverso al de registro, asi que este corre
 * el ultimo, con el pool de Hikari ya cerrado. Cubre los tres finales posibles
 * de un contexto: expulsion de la cache de tests (tope 32; cada expulsion
 * dejaba antes una base huerfana), @DirtiesContext, y el apagado de la JVM.
 */
final class BaseDeTest {

    private BaseDeTest() {
    }

    static void conectar(ConfigurableApplicationContext contexto, String base, Runnable alCerrar) {
        TestPropertyValues.of(
                "spring.datasource.url=" + TestPostgres.jdbcUrl(base),
                "spring.datasource.username=" + TestPostgres.username(),
                "spring.datasource.password=" + TestPostgres.password(),
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                // Spring cachea los contextos de test durante TODA la suite, y
                // cada uno se queda con su pool. Con el tamano por defecto (10)
                // y decenas de contextos, Postgres se queda sin conexiones y
                // falla con "sorry, too many clients already". Los tests van en
                // serie: con dos conexiones sobra, y minimum-idle=0 las suelta
                // cuando el contexto esta parado.
                "spring.datasource.hikari.maximum-pool-size=2",
                "spring.datasource.hikari.minimum-idle=0"
        ).applyTo(contexto.getEnvironment());

        if (contexto.getBeanFactory() instanceof DefaultSingletonBeanRegistry registro) {
            registro.registerDisposableBean("borradoDeLaBaseDeTest_" + base, alCerrar::run);
        } else {
            throw new IllegalStateException("No se puede registrar el borrado de la base " + base
                    + ": el bean factory no es un DefaultSingletonBeanRegistry");
        }
    }
}

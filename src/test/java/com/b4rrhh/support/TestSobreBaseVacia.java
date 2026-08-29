package com.b4rrhh.support;

import com.b4rrhh.employee.journey.infrastructure.persistence.EmployeeJourneyLookupAdapter;
import com.b4rrhh.employee.journey.infrastructure.persistence.JourneyContractReadAdapter;
import com.b4rrhh.employee.journey.infrastructure.persistence.JourneyCostCenterReadAdapter;
import com.b4rrhh.employee.journey.infrastructure.persistence.JourneyLaborClassificationReadAdapter;
import com.b4rrhh.employee.journey.infrastructure.persistence.JourneyPresenceReadAdapter;
import com.b4rrhh.employee.journey.infrastructure.persistence.JourneyWorkCenterReadAdapter;
import com.b4rrhh.payroll.basesalary.infrastructure.persistence.PayrollObjectBindingLookupAdapter;
import com.b4rrhh.payroll.basesalary.infrastructure.persistence.PayrollTableRowLookupAdapter;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Un test JPA sobre una base Postgres vacia, en la que el propio test se crea
 * su esquema a mano en {@code @BeforeEach}.
 *
 * Es el pasado del proyecto, no su futuro: ese DDL a mano lleva tiempo
 * divergiendo del real y el issue #1 lo va llevando a {@link TestSobreEsquemaReal}.
 * Hasta entonces, al menos corre contra el motor de produccion y no contra H2:
 * comprobar una restriccion de integridad en una base que no es la de
 * produccion solo demuestra que H2 la respeta.
 *
 * Todos los tests que la llevan comparten un contexto y una base (ver la nota
 * sobre la cache de contextos en {@link TestSobreEsquemaReal}). Eso obliga a
 * que cada uno deje la base como la necesita en su {@code @BeforeEach}:
 * {@code drop table if exists} y {@code create}, no {@code create} a secas.
 * Los tests con una {@code @TestConfiguration} anidada no pueden compartir
 * contexto y siguen declarando lo suyo.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = TestPostgresInitializer.class)
@Import({
        // journey
        JourneyPresenceReadAdapter.class,
        JourneyContractReadAdapter.class,
        JourneyLaborClassificationReadAdapter.class,
        JourneyWorkCenterReadAdapter.class,
        JourneyCostCenterReadAdapter.class,
        EmployeeJourneyLookupAdapter.class,
        // payroll: lookups del salario base
        PayrollObjectBindingLookupAdapter.class,
        PayrollTableRowLookupAdapter.class
})
public @interface TestSobreBaseVacia {
}

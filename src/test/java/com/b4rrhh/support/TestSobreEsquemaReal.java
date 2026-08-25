package com.b4rrhh.support;

import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeBusinessKeyLookupSupport;
import com.b4rrhh.employee.working_time.application.service.DefaultWorkingTimePresenceConsistencyValidator;
import com.b4rrhh.employee.working_time.application.service.StandardWorkingTimeDerivationPolicy;
import com.b4rrhh.employee.working_time.application.usecase.CreateWorkingTimeService;
import com.b4rrhh.employee.working_time.infrastructure.persistence.AgreementAnnualHoursLookupAdapter;
import com.b4rrhh.employee.working_time.infrastructure.persistence.EmployeeAgreementContextLookupAdapter;
import com.b4rrhh.employee.working_time.infrastructure.persistence.EmployeeWorkingTimeLookupAdapter;
import com.b4rrhh.employee.working_time.infrastructure.persistence.WorkingTimePersistenceAdapter;
import com.b4rrhh.employee.working_time.infrastructure.persistence.WorkingTimePresenceConsistencyAdapter;
import com.b4rrhh.payroll.agreementplus.application.service.CalculateAgreementPlusService;
import com.b4rrhh.payroll.basesalary.application.service.CalculateBaseSalaryService;
import com.b4rrhh.payroll.basesalary.infrastructure.persistence.EmployeeAgreementCategoryLookupAdapter;
import com.b4rrhh.payroll.basesalary.infrastructure.persistence.EmployeeByBusinessKeyLookupAdapter;
import com.b4rrhh.payroll.basesalary.infrastructure.persistence.PayrollObjectActivationLookupAdapter;
import com.b4rrhh.payroll.basesalary.infrastructure.persistence.PayrollObjectBindingLookupAdapter;
import com.b4rrhh.payroll.basesalary.infrastructure.persistence.PayrollTableRowLookupAdapter;
import com.b4rrhh.payroll_engine.concept.infrastructure.persistence.PayrollConceptPersistenceAdapter;
import com.b4rrhh.payroll_engine.eligibility.infrastructure.persistence.ConceptAssignmentPersistenceAdapter;
import com.b4rrhh.payroll_engine.table.infrastructure.persistence.PayrollTableRowManagementAdapter;
import com.b4rrhh.rulesystem.agreementprofile.infrastructure.persistence.AgreementCatalogLookupAdapter;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Un test JPA sobre el esquema real de produccion.
 *
 * El esquema no lo declara el test: es el de produccion, aplicado por Flyway
 * una vez y clonado para el contexto (ver EsquemaReal). Postgres de verdad, no
 * el H2 que Spring pone por defecto. Las semillas de las migraciones vienen
 * incluidas: un test que cuente filas o inserte codigos tiene que apartarse
 * de lo que siembran (rule system TST en vez de ESP, por ejemplo).
 *
 * Por que una sola anotacion y no que cada test declare lo suyo: Spring cachea
 * los contextos de test por configuracion, y cualquier diferencia, un
 * {@code @Import} distinto sin ir mas lejos, es otro contexto, o sea, otro
 * clon de la base y otros segundos de arranque. Con esta anotacion todos los
 * tests declaran EXACTAMENTE la misma configuracion y comparten un unico
 * contexto y un unico clon (issue #12). Por eso el {@code @Import} de aqui es
 * la union de lo que necesita cada test: importar de mas no cuesta nada;
 * importar distinto, si.
 *
 * Si un test nuevo necesita otro adapter, se anade AQUI, no en el test.
 * Cualquier otra anotacion de Spring en la clase de test (un
 * {@code @Import} propio, un {@code @TestPropertySource}, un
 * {@code @Transactional} a nivel de clase) la saca del contexto compartido.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = EsquemaRealInitializer.class)
@Import({
        // working_time
        CreateWorkingTimeService.class,
        WorkingTimePersistenceAdapter.class,
        EmployeeWorkingTimeLookupAdapter.class,
        EmployeeBusinessKeyLookupSupport.class,
        EmployeeAgreementContextLookupAdapter.class,
        AgreementAnnualHoursLookupAdapter.class,
        AgreementCatalogLookupAdapter.class,
        DefaultWorkingTimePresenceConsistencyValidator.class,
        WorkingTimePresenceConsistencyAdapter.class,
        StandardWorkingTimeDerivationPolicy.class,
        // payroll: salario base y plus convenio
        CalculateBaseSalaryService.class,
        CalculateAgreementPlusService.class,
        PayrollObjectBindingLookupAdapter.class,
        PayrollTableRowLookupAdapter.class,
        PayrollObjectActivationLookupAdapter.class,
        EmployeeAgreementCategoryLookupAdapter.class,
        EmployeeByBusinessKeyLookupAdapter.class,
        // payroll_engine
        PayrollConceptPersistenceAdapter.class,
        ConceptAssignmentPersistenceAdapter.class,
        PayrollTableRowManagementAdapter.class
})
public @interface TestSobreEsquemaReal {
}

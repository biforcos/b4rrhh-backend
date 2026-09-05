package com.b4rrhh.support;

import com.b4rrhh.employee.absence.infrastructure.persistence.AbsenceRuleEntityUsageParticipant;
import com.b4rrhh.employee.address.application.service.AddressCatalogValidator;
import com.b4rrhh.employee.address.application.service.AddressTimelineService;
import com.b4rrhh.employee.address.application.usecase.CreateAddressService;
import com.b4rrhh.employee.address.application.usecase.DeleteAddressService;
import com.b4rrhh.employee.address.application.usecase.PlanAddressChangeService;
import com.b4rrhh.employee.address.application.usecase.UpdateAddressService;
import com.b4rrhh.employee.address.infrastructure.persistence.AddressPersistenceAdapter;
import com.b4rrhh.employee.address.infrastructure.persistence.AddressPresenceLookupAdapter;
import com.b4rrhh.employee.address.infrastructure.persistence.AddressRuleEntityUsageParticipant;
import com.b4rrhh.employee.address.infrastructure.persistence.AddressTypeCoverageLookupAdapter;
import com.b4rrhh.employee.address.infrastructure.persistence.EmployeeAddressLookupAdapter;
import com.b4rrhh.employee.employee.infrastructure.persistence.EmployeeRuleEntityUsageParticipant;
import com.b4rrhh.employee.payroll_input.infrastructure.persistence.PayrollInputRuleEntityUsageParticipant;
import com.b4rrhh.employee.contact.infrastructure.persistence.ContactRuleEntityUsageParticipant;
import com.b4rrhh.employee.contract.infrastructure.persistence.ContractRuleEntityUsageParticipant;
import com.b4rrhh.employee.cost_center.infrastructure.persistence.CostCenterRuleEntityUsageParticipant;
import com.b4rrhh.employee.identifier.infrastructure.persistence.IdentifierRuleEntityUsageParticipant;
import com.b4rrhh.employee.journey.infrastructure.persistence.EmployeeJourneyLookupAdapter;
import com.b4rrhh.employee.journey.infrastructure.persistence.JourneyContractReadAdapter;
import com.b4rrhh.employee.journey.infrastructure.persistence.JourneyCostCenterReadAdapter;
import com.b4rrhh.employee.journey.infrastructure.persistence.JourneyLaborClassificationReadAdapter;
import com.b4rrhh.employee.journey.infrastructure.persistence.JourneyPresenceReadAdapter;
import com.b4rrhh.employee.journey.infrastructure.persistence.JourneyWorkCenterReadAdapter;
import com.b4rrhh.employee.labor_classification.infrastructure.persistence.LaborClassificationRuleEntityUsageParticipant;
import com.b4rrhh.employee.presence.infrastructure.persistence.PresenceRuleEntityUsageParticipant;
import com.b4rrhh.employee.workcenter.infrastructure.persistence.WorkCenterRuleEntityUsageParticipant;
import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeBusinessKeyLookupSupport;
import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeOwnedLookupSupport;
import com.b4rrhh.employee.working_time.application.service.DefaultWorkingTimePresenceConsistencyValidator;
import com.b4rrhh.employee.working_time.application.service.StandardWorkingTimeDerivationPolicy;
import com.b4rrhh.employee.working_time.application.service.WorkingTimeTimelineService;
import com.b4rrhh.employee.working_time.application.usecase.CreateWorkingTimeService;
import com.b4rrhh.employee.working_time.application.usecase.DeleteWorkingTimeService;
import com.b4rrhh.employee.working_time.application.usecase.PlanWorkingTimeChangeService;
import com.b4rrhh.employee.working_time.application.usecase.UpdateWorkingTimeService;
import com.b4rrhh.employee.working_time.infrastructure.persistence.AgreementAnnualHoursLookupAdapter;
import com.b4rrhh.employee.working_time.infrastructure.persistence.EmployeeAgreementContextLookupAdapter;
import com.b4rrhh.employee.working_time.infrastructure.persistence.EmployeeWorkingTimeLookupAdapter;
import com.b4rrhh.employee.working_time.infrastructure.persistence.WorkingTimePersistenceAdapter;
import com.b4rrhh.employee.working_time.infrastructure.persistence.WorkingTimePresenceConsistencyAdapter;
import com.b4rrhh.payroll.basesalary.infrastructure.persistence.PayrollObjectBindingLookupAdapter;
import com.b4rrhh.payroll.basesalary.infrastructure.persistence.PayrollTableRowLookupAdapter;
import com.b4rrhh.payroll_engine.concept.infrastructure.persistence.PayrollConceptOperandPersistenceAdapter;
import com.b4rrhh.payroll_engine.concept.infrastructure.persistence.PayrollConceptPersistenceAdapter;
import com.b4rrhh.payroll_engine.eligibility.infrastructure.persistence.ConceptAssignmentPersistenceAdapter;
import com.b4rrhh.payroll_engine.table.infrastructure.persistence.PayrollTableRowManagementAdapter;
import com.b4rrhh.rulesystem.agreementprofile.infrastructure.persistence.AgreementCatalogLookupAdapter;
import com.b4rrhh.rulesystem.agreementprofile.infrastructure.persistence.AgreementProfilePersistenceAdapter;
import com.b4rrhh.rulesystem.application.usecase.DeleteRuleEntityService;
import com.b4rrhh.rulesystem.employeeaddresstypeprofile.infrastructure.persistence.EmployeeAddressTypeProfilePersistenceAdapter;
import com.b4rrhh.rulesystem.infrastructure.persistence.RuleEntityPersistenceAdapter;
import com.b4rrhh.rulesystem.infrastructure.persistence.RuleEntityUsageCheckAdapter;
import com.b4rrhh.rulesystem.infrastructure.persistence.RuleSystemPersistenceAdapter;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.rulesystem.translation.application.usecase.GetRuleEntityTranslationCoverageService;
import com.b4rrhh.rulesystem.translation.infrastructure.persistence.RuleEntityTranslationCoverageReadAdapter;
import com.b4rrhh.rulesystem.translation.infrastructure.persistence.RuleEntityTranslationPersistenceAdapter;
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
        UpdateWorkingTimeService.class,
        DeleteWorkingTimeService.class,
        PlanWorkingTimeChangeService.class,
        WorkingTimeTimelineService.class,
        WorkingTimePersistenceAdapter.class,
        EmployeeWorkingTimeLookupAdapter.class,
        EmployeeBusinessKeyLookupSupport.class,
        EmployeeAgreementContextLookupAdapter.class,
        AgreementAnnualHoursLookupAdapter.class,
        AgreementCatalogLookupAdapter.class,
        DefaultWorkingTimePresenceConsistencyValidator.class,
        WorkingTimePresenceConsistencyAdapter.class,
        StandardWorkingTimeDerivationPolicy.class,
        // address: la serie por (empleado, tipo) sobre el componente temporal (backend#53)
        CreateAddressService.class,
        UpdateAddressService.class,
        DeleteAddressService.class,
        PlanAddressChangeService.class,
        AddressTimelineService.class,
        AddressCatalogValidator.class,
        AddressPersistenceAdapter.class,
        AddressPresenceLookupAdapter.class,
        AddressTypeCoverageLookupAdapter.class,
        EmployeeAddressLookupAdapter.class,
        EmployeeOwnedLookupSupport.class,
        RuleSystemPersistenceAdapter.class,
        // journey: lectura de las cinco pistas del historial (backend#1)
        JourneyPresenceReadAdapter.class,
        JourneyContractReadAdapter.class,
        JourneyLaborClassificationReadAdapter.class,
        JourneyWorkCenterReadAdapter.class,
        JourneyCostCenterReadAdapter.class,
        EmployeeJourneyLookupAdapter.class,
        // payroll: binding de objetos y tablas
        PayrollObjectBindingLookupAdapter.class,
        PayrollTableRowLookupAdapter.class,
        // payroll_engine
        PayrollConceptPersistenceAdapter.class,
        // payroll_engine: las aristas de operandos, para el guardian de ADR-058 (backend#63)
        PayrollConceptOperandPersistenceAdapter.class,
        ConceptAssignmentPersistenceAdapter.class,
        PayrollTableRowManagementAdapter.class,
        // rulesystem: traducciones de rule_entity (ADR-052)
        RuleEntityPersistenceAdapter.class,
        RuleEntityTranslationPersistenceAdapter.class,
        RuleEntityLabelResolver.class,
        RuleEntityTranslationCoverageReadAdapter.class,
        GetRuleEntityTranslationCoverageService.class,
        // rulesystem: perfil de convenio (#2)
        AgreementProfilePersistenceAdapter.class,
        // rulesystem: cobertura de cada tipo de direccion (backend#53)
        EmployeeAddressTypeProfilePersistenceAdapter.class,
        // rulesystem: borrado de codigos (backend#26)
        DeleteRuleEntityService.class,
        RuleEntityUsageCheckAdapter.class,
        // employee: donde cada vertical guarda codigos de catalogo (backend#28)
        AddressRuleEntityUsageParticipant.class,
        ContactRuleEntityUsageParticipant.class,
        ContractRuleEntityUsageParticipant.class,
        CostCenterRuleEntityUsageParticipant.class,
        IdentifierRuleEntityUsageParticipant.class,
        LaborClassificationRuleEntityUsageParticipant.class,
        PresenceRuleEntityUsageParticipant.class,
        WorkCenterRuleEntityUsageParticipant.class,
        AbsenceRuleEntityUsageParticipant.class,
        EmployeeRuleEntityUsageParticipant.class,
        PayrollInputRuleEntityUsageParticipant.class
})
public @interface TestSobreEsquemaReal {
}

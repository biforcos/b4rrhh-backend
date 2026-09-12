package com.b4rrhh.employee.lifecycle.application.usecase;

import com.b4rrhh.employee.contract.application.command.CreateContractCommand;
import com.b4rrhh.employee.contract.application.command.ListEmployeeContractsCommand;
import com.b4rrhh.employee.contract.application.usecase.CreateContractUseCase;
import com.b4rrhh.employee.contract.application.usecase.ListEmployeeContractsUseCase;
import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.cost_center.application.usecase.CreateCostCenterDistributionCommand;
import com.b4rrhh.employee.cost_center.application.usecase.CreateCostCenterDistributionUseCase;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionInvalidException;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterAllocation;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionWindow;
import com.b4rrhh.employee.employee.application.service.EmployeeTypeCatalogValidator;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.employee.domain.exception.EmployeeTypeInvalidException;
import com.b4rrhh.employee.employee.domain.port.EmployeeRepository;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import com.b4rrhh.employee.labor_classification.application.command.CreateLaborClassificationCommand;
import com.b4rrhh.employee.labor_classification.application.command.ListEmployeeLaborClassificationsCommand;
import com.b4rrhh.employee.labor_classification.application.usecase.CreateLaborClassificationUseCase;
import com.b4rrhh.employee.labor_classification.application.usecase.ListEmployeeLaborClassificationsUseCase;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.lifecycle.application.command.RehireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.RehireEmployeeResult;
import com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeBusinessValidationException;
import com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeCatalogValueInvalidException;
import com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeConflictException;
import com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeDistributionInvalidException;
import com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeEmployeeNotFoundException;
import com.b4rrhh.employee.presence.application.usecase.CreatePresenceCommand;
import com.b4rrhh.employee.presence.application.usecase.CreatePresenceUseCase;
import com.b4rrhh.employee.presence.application.usecase.ListEmployeePresencesUseCase;
import com.b4rrhh.employee.presence.domain.model.Presence;
import com.b4rrhh.employee.working_time.application.usecase.CreateWorkingTimeCommand;
import com.b4rrhh.employee.working_time.application.usecase.CreateWorkingTimeUseCase;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesCommand;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesUseCase;
import com.b4rrhh.employee.working_time.domain.exception.InvalidWorkingTimePercentageException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeIsACorrectionException;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimeDerivedHours;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimeOccurrence;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimePeriod;
import com.b4rrhh.employee.workcenter.application.usecase.CreateWorkCenterCommand;
import com.b4rrhh.employee.workcenter.application.usecase.CreateWorkCenterUseCase;
import com.b4rrhh.employee.workcenter.application.usecase.ListEmployeeWorkCentersUseCase;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterCompanyMismatchException;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import com.b4rrhh.employee.workcenter.domain.port.WorkCenterCompanyLookupPort;
import com.b4rrhh.employee.workcenter.domain.service.WorkCenterCompanyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RehireEmployeeServiceTest {

    @Mock
    private GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKeyUseCase;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private ListEmployeePresencesUseCase listEmployeePresencesUseCase;
    @Mock
    private ListEmployeeContractsUseCase listEmployeeContractsUseCase;
    @Mock
    private ListEmployeeLaborClassificationsUseCase listEmployeeLaborClassificationsUseCase;
    @Mock
    private ListEmployeeWorkCentersUseCase listEmployeeWorkCentersUseCase;
        @Mock
        private ListEmployeeWorkingTimesUseCase listEmployeeWorkingTimesUseCase;
    @Mock
    private CreatePresenceUseCase createPresenceUseCase;
    @Mock
    private CreateLaborClassificationUseCase createLaborClassificationUseCase;
    @Mock
    private CreateContractUseCase createContractUseCase;
    @Mock
    private CreateWorkCenterUseCase createWorkCenterUseCase;
    @Mock
    private CreateCostCenterDistributionUseCase createCostCenterDistributionUseCase;
        @Mock
        private CreateWorkingTimeUseCase createWorkingTimeUseCase;
        @Mock
        private WorkCenterCompanyLookupPort workCenterCompanyLookupPort;
    @Mock
    private RuleEntityRepository employeeTypeRuleEntityRepository;

        private WorkCenterCompanyValidator workCenterCompanyValidator;
    private EmployeeTypeCatalogValidator employeeTypeCatalogValidator;
    private RehireEmployeeService service;

    @BeforeEach
    void setUp() {
                workCenterCompanyValidator = new WorkCenterCompanyValidator(workCenterCompanyLookupPort);
        employeeTypeCatalogValidator = new EmployeeTypeCatalogValidator(employeeTypeRuleEntityRepository);

        RuleEntity validEmployeeType = new RuleEntity(
                1L, "ESP", "EMPLOYEE_TYPE", "INTERNAL",
                "Internal Employee", null, true,
                java.time.LocalDate.of(1900, 1, 1), null,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );
        lenient().when(employeeTypeRuleEntityRepository.findByBusinessKey("ESP", "EMPLOYEE_TYPE", "INTERNAL"))
                .thenReturn(java.util.Optional.of(validEmployeeType));

        service = new RehireEmployeeService(
                getEmployeeByBusinessKeyUseCase,
                employeeRepository,
                listEmployeePresencesUseCase,
                listEmployeeContractsUseCase,
                listEmployeeLaborClassificationsUseCase,
                listEmployeeWorkCentersUseCase,
                                listEmployeeWorkingTimesUseCase,
                createPresenceUseCase,
                createLaborClassificationUseCase,
                createContractUseCase,
                createWorkCenterUseCase,
                                createCostCenterDistributionUseCase,
                                createWorkingTimeUseCase,
                                workCenterCompanyValidator,
                employeeTypeCatalogValidator
        );

                lenient().when(listEmployeeWorkingTimesUseCase.listByEmployeeBusinessKey(any(ListEmployeeWorkingTimesCommand.class)))
                                .thenReturn(List.of());
                lenient().when(createWorkingTimeUseCase.create(any(CreateWorkingTimeCommand.class)))
                                .thenAnswer(invocation -> {
                                        CreateWorkingTimeCommand command = invocation.getArgument(0);
                                        return activeWorkingTime(2, command.startDate(), command.workingTimePercentage());
                                });
    }

    @Test
    void rehiresEmployeeAndPropagatesRehireDateToAllInitialRecords() {
        RehireEmployeeCommand command = validCommand();
        when(workCenterCompanyLookupPort.findCompanyCode("ESP", "MADRID_01", LocalDate.of(2026, 4, 15)))
                .thenReturn(Optional.of("ES01"));

        Presence closedPresence = closedPresence(LocalDate.of(2026, 3, 31));
        Presence newPresence = activePresence(LocalDate.of(2026, 4, 15));
        LaborClassification newLabor = activeLabor(LocalDate.of(2026, 4, 15));
        Contract newContract = activeContract(LocalDate.of(2026, 4, 15));
        WorkCenter newWorkCenter = activeWorkCenter(LocalDate.of(2026, 4, 15));
        WorkingTime previousClosedWorkingTime = closedWorkingTime(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), new BigDecimal("50"));
        WorkingTime newWorkingTime = activeWorkingTime(2, LocalDate.of(2026, 4, 15), new BigDecimal("80"));

        when(getEmployeeByBusinessKeyUseCase.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(employee("TERMINATED")));
        when(listEmployeePresencesUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(closedPresence), List.of(newPresence));
        when(listEmployeeContractsUseCase.listByEmployeeBusinessKey(any(ListEmployeeContractsCommand.class)))
                .thenReturn(List.of(new Contract(1L, "CON", "SUB", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))));
        when(listEmployeeLaborClassificationsUseCase.listByEmployeeBusinessKey(any(ListEmployeeLaborClassificationsCommand.class)))
                .thenReturn(List.of(new LaborClassification(1L, "AGR", "CAT", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))));
        when(listEmployeeWorkCentersUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(new WorkCenter(10L, 1L, 1, "WC1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), LocalDateTime.now(), LocalDateTime.now())));
        when(listEmployeeWorkingTimesUseCase.listByEmployeeBusinessKey(any(ListEmployeeWorkingTimesCommand.class)))
                .thenReturn(List.of(previousClosedWorkingTime));
        when(createPresenceUseCase.create(any(CreatePresenceCommand.class))).thenReturn(newPresence);
        when(createLaborClassificationUseCase.create(any(CreateLaborClassificationCommand.class))).thenReturn(newLabor);
        when(createContractUseCase.create(any(CreateContractCommand.class))).thenReturn(newContract);
        when(createWorkCenterUseCase.create(any(CreateWorkCenterCommand.class))).thenReturn(newWorkCenter);
        when(createWorkingTimeUseCase.create(any(CreateWorkingTimeCommand.class))).thenReturn(newWorkingTime);
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee("ACTIVE"));

        RehireEmployeeResult result = service.rehire(command);

        assertEquals("ACTIVE", result.status());
        assertEquals(LocalDate.of(2026, 4, 15), result.rehireDate());
        assertEquals(1, result.newPresenceNumber());
        assertEquals("CON", result.newContractTypeCode());
        assertEquals("METAL", result.newAgreementCode());
        assertEquals(1, result.newWorkCenterAssignmentNumber());
        assertNull(result.newCostCenter());
        assertTrue(result.created());
        assertNotNull(result.newWorkingTime());
        assertEquals(2, result.newWorkingTime().workingTimeNumber());
        assertEquals(0, new BigDecimal("80").compareTo(result.newWorkingTime().workingTimePercentage()));
        assertEquals(new BigDecimal("32.00"), result.newWorkingTime().weeklyHours());
        assertEquals(LocalDate.of(2026, 4, 15), result.newWorkingTime().startDate());
        assertNull(result.newWorkingTime().endDate());
        assertFalse(previousClosedWorkingTime.getWorkingTimeNumber().equals(result.newWorkingTime().workingTimeNumber()));

        ArgumentCaptor<CreatePresenceCommand> presenceCaptor = ArgumentCaptor.forClass(CreatePresenceCommand.class);
        verify(createPresenceUseCase).create(presenceCaptor.capture());
        assertEquals(LocalDate.of(2026, 4, 15), presenceCaptor.getValue().startDate());

        ArgumentCaptor<CreateLaborClassificationCommand> laborCaptor = ArgumentCaptor.forClass(CreateLaborClassificationCommand.class);
        verify(createLaborClassificationUseCase).create(laborCaptor.capture());
        assertEquals(LocalDate.of(2026, 4, 15), laborCaptor.getValue().startDate());

        ArgumentCaptor<CreateContractCommand> contractCaptor = ArgumentCaptor.forClass(CreateContractCommand.class);
        verify(createContractUseCase).create(contractCaptor.capture());
        assertEquals(LocalDate.of(2026, 4, 15), contractCaptor.getValue().startDate());

        ArgumentCaptor<CreateWorkCenterCommand> workCenterCaptor = ArgumentCaptor.forClass(CreateWorkCenterCommand.class);
        verify(createWorkCenterUseCase).create(workCenterCaptor.capture());
        assertEquals(LocalDate.of(2026, 4, 15), workCenterCaptor.getValue().startDate());

        ArgumentCaptor<CreateWorkingTimeCommand> workingTimeCaptor = ArgumentCaptor.forClass(CreateWorkingTimeCommand.class);
        verify(createWorkingTimeUseCase).create(workingTimeCaptor.capture());
        assertEquals(LocalDate.of(2026, 4, 15), workingTimeCaptor.getValue().startDate());
        assertEquals(0, new BigDecimal("80").compareTo(workingTimeCaptor.getValue().workingTimePercentage()));

        verify(createCostCenterDistributionUseCase, never()).create(any(CreateCostCenterDistributionCommand.class));
    }

    @Test
    void failsWhenWorkingTimeBlockIsMissing() {
        RehireEmployeeCommand command = new RehireEmployeeCommand(
                "esp",
                "internal",
                "EMP001",
                LocalDate.of(2026, 4, 15),
                "rehire",
                "es01",
                "metal",
                "oficial_1",
                "con",
                "sub",
                "madrid_01",
                null,
                null
        );

        assertThrows(com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeRequestInvalidException.class, () -> service.rehire(command));
        verify(createPresenceUseCase, never()).create(any(CreatePresenceCommand.class));
    }

    @Test
    void failsWhenWorkingTimePercentageIsMissing() {
        RehireEmployeeCommand command = new RehireEmployeeCommand(
                "esp",
                "internal",
                "EMP001",
                LocalDate.of(2026, 4, 15),
                "rehire",
                "es01",
                "metal",
                "oficial_1",
                "con",
                "sub",
                "madrid_01",
                null,
                new RehireEmployeeCommand.RehireEmployeeWorkingTimeCommand(null)
        );

        assertThrows(com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeRequestInvalidException.class, () -> service.rehire(command));
        verify(createPresenceUseCase, never()).create(any(CreatePresenceCommand.class));
    }

    @Test
    void failsWhenWorkingTimePercentageIsInvalid() {
        when(workCenterCompanyLookupPort.findCompanyCode("ESP", "MADRID_01", LocalDate.of(2026, 4, 15)))
                .thenReturn(Optional.of("ES01"));

        Presence closedPresence = closedPresence(LocalDate.of(2026, 3, 31));
        Presence newPresence = activePresence(LocalDate.of(2026, 4, 15));

        when(getEmployeeByBusinessKeyUseCase.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(employee("TERMINATED")));
        when(listEmployeePresencesUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(closedPresence), List.of(newPresence));
        when(listEmployeeContractsUseCase.listByEmployeeBusinessKey(any(ListEmployeeContractsCommand.class)))
                .thenReturn(List.of(new Contract(1L, "CON", "SUB", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))));
        when(listEmployeeLaborClassificationsUseCase.listByEmployeeBusinessKey(any(ListEmployeeLaborClassificationsCommand.class)))
                .thenReturn(List.of(new LaborClassification(1L, "AGR", "CAT", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))));
        when(listEmployeeWorkCentersUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(new WorkCenter(10L, 1L, 1, "WC1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), LocalDateTime.now(), LocalDateTime.now())));
        when(createPresenceUseCase.create(any(CreatePresenceCommand.class))).thenReturn(newPresence);
        when(createLaborClassificationUseCase.create(any(CreateLaborClassificationCommand.class))).thenReturn(activeLabor(LocalDate.of(2026, 4, 15)));
        when(createContractUseCase.create(any(CreateContractCommand.class))).thenReturn(activeContract(LocalDate.of(2026, 4, 15)));
        when(createWorkCenterUseCase.create(any(CreateWorkCenterCommand.class))).thenReturn(activeWorkCenter(LocalDate.of(2026, 4, 15)));
        when(createWorkingTimeUseCase.create(any(CreateWorkingTimeCommand.class)))
                .thenThrow(new InvalidWorkingTimePercentageException("workingTimePercentage must be greater than 0 and less than or equal to 100"));

        assertThrows(RehireEmployeeBusinessValidationException.class, () -> service.rehire(validCommand()));
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    /**
     * Igual que el de arriba, pero con una excepcion de invariante que el flujo
     * no nombra en ningun sitio: sale como error de validacion solo porque es
     * del supertipo (backend#59). Se fuerza porque la reincorporacion tampoco
     * puede provocarla hoy.
     */
    @Test
    void failsWhenWorkingTimeSeriesInvariantNobodyNamesIsViolated() {
        when(workCenterCompanyLookupPort.findCompanyCode("ESP", "MADRID_01", LocalDate.of(2026, 4, 15)))
                .thenReturn(Optional.of("ES01"));

        Presence closedPresence = closedPresence(LocalDate.of(2026, 3, 31));
        Presence newPresence = activePresence(LocalDate.of(2026, 4, 15));

        when(getEmployeeByBusinessKeyUseCase.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(employee("TERMINATED")));
        when(listEmployeePresencesUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(closedPresence), List.of(newPresence));
        when(listEmployeeContractsUseCase.listByEmployeeBusinessKey(any(ListEmployeeContractsCommand.class)))
                .thenReturn(List.of(new Contract(1L, "CON", "SUB", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))));
        when(listEmployeeLaborClassificationsUseCase.listByEmployeeBusinessKey(any(ListEmployeeLaborClassificationsCommand.class)))
                .thenReturn(List.of(new LaborClassification(1L, "AGR", "CAT", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))));
        when(listEmployeeWorkCentersUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(new WorkCenter(10L, 1L, 1, "WC1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), LocalDateTime.now(), LocalDateTime.now())));
        when(createPresenceUseCase.create(any(CreatePresenceCommand.class))).thenReturn(newPresence);
        when(createLaborClassificationUseCase.create(any(CreateLaborClassificationCommand.class))).thenReturn(activeLabor(LocalDate.of(2026, 4, 15)));
        when(createContractUseCase.create(any(CreateContractCommand.class))).thenReturn(activeContract(LocalDate.of(2026, 4, 15)));
        when(createWorkCenterUseCase.create(any(CreateWorkCenterCommand.class))).thenReturn(activeWorkCenter(LocalDate.of(2026, 4, 15)));
        when(createWorkingTimeUseCase.create(any(CreateWorkingTimeCommand.class)))
                .thenThrow(new WorkingTimeIsACorrectionException(
                        "ESP", "INTERNAL", "EMP001",
                        new WorkingTimeOccurrence(1, LocalDate.of(2026, 4, 15), null),
                        new WorkingTimePeriod(LocalDate.of(2026, 4, 15), null)));

        assertThrows(RehireEmployeeBusinessValidationException.class, () -> service.rehire(validCommand()));
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void failsWhenEmployeeNotFound() {
        when(getEmployeeByBusinessKeyUseCase.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.empty());

        assertThrows(RehireEmployeeEmployeeNotFoundException.class, () -> service.rehire(validCommand()));
        verify(createPresenceUseCase, never()).create(any(CreatePresenceCommand.class));
    }

    @Test
    void mapsInvalidEmployeeTypeToLifecycleException() {
        when(getEmployeeByBusinessKeyUseCase.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(employee("TERMINATED")));
        when(workCenterCompanyLookupPort.findCompanyCode("ESP", "MADRID_01", LocalDate.of(2026, 4, 15)))
                .thenReturn(Optional.of("ES01"));
        when(listEmployeePresencesUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(closedPresence(LocalDate.of(2026, 3, 31))));
        when(listEmployeeContractsUseCase.listByEmployeeBusinessKey(any(ListEmployeeContractsCommand.class)))
                .thenReturn(List.of());
        when(listEmployeeLaborClassificationsUseCase.listByEmployeeBusinessKey(any(ListEmployeeLaborClassificationsCommand.class)))
                .thenReturn(List.of());
        when(listEmployeeWorkCentersUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of());
        // Override the lenient stub: employee type NOT found → validator throws EmployeeTypeInvalidException
        when(employeeTypeRuleEntityRepository.findByBusinessKey("ESP", "EMPLOYEE_TYPE", "INTERNAL"))
                .thenReturn(Optional.empty());

        assertThrows(RehireEmployeeCatalogValueInvalidException.class, () -> service.rehire(validCommand()));
        verify(createPresenceUseCase, never()).create(any(CreatePresenceCommand.class));
    }

    @Test
    void failsFastWhenWorkCenterDoesNotBelongToCompany() {
        when(getEmployeeByBusinessKeyUseCase.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(employee("TERMINATED")));
        when(workCenterCompanyLookupPort.findCompanyCode("ESP", "MADRID_01", LocalDate.of(2026, 4, 15)))
                .thenReturn(Optional.of("OTHER"));

        assertThrows(WorkCenterCompanyMismatchException.class, () -> service.rehire(validCommand()));
        verify(createPresenceUseCase, never()).create(any(CreatePresenceCommand.class));
    }

    @Test
    void failsIfActivePresenceAlreadyExists() {
        when(workCenterCompanyLookupPort.findCompanyCode("ESP", "MADRID_01", LocalDate.of(2026, 4, 15)))
                .thenReturn(Optional.of("ES01"));
        when(getEmployeeByBusinessKeyUseCase.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(employee("TERMINATED")));
        when(listEmployeePresencesUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(activePresence(LocalDate.of(2026, 4, 15))));
        when(listEmployeeContractsUseCase.listByEmployeeBusinessKey(any(ListEmployeeContractsCommand.class)))
                .thenReturn(List.of(activeContract(LocalDate.of(2026, 4, 15))));
        when(listEmployeeLaborClassificationsUseCase.listByEmployeeBusinessKey(any(ListEmployeeLaborClassificationsCommand.class)))
                .thenReturn(List.of(activeLabor(LocalDate.of(2026, 4, 15))));
        when(listEmployeeWorkCentersUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(activeWorkCenter(LocalDate.of(2026, 4, 15))));
        when(listEmployeeWorkingTimesUseCase.listByEmployeeBusinessKey(any(ListEmployeeWorkingTimesCommand.class)))
                .thenReturn(List.of(activeWorkingTime(2, LocalDate.of(2026, 4, 15), new BigDecimal("80"))));

        assertThrows(RehireEmployeeConflictException.class, () -> service.rehire(validCommand()));
        verify(createPresenceUseCase, never()).create(any(CreatePresenceCommand.class));
    }

    @Test
    void failsIfNoPreviousClosedPresenceExists() {
        when(workCenterCompanyLookupPort.findCompanyCode("ESP", "MADRID_01", LocalDate.of(2026, 4, 15)))
                .thenReturn(Optional.of("ES01"));
        when(getEmployeeByBusinessKeyUseCase.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(employee("TERMINATED")));
        when(listEmployeePresencesUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of());
        when(listEmployeeContractsUseCase.listByEmployeeBusinessKey(any(ListEmployeeContractsCommand.class)))
                .thenReturn(List.of());
        when(listEmployeeLaborClassificationsUseCase.listByEmployeeBusinessKey(any(ListEmployeeLaborClassificationsCommand.class)))
                .thenReturn(List.of());
        when(listEmployeeWorkCentersUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of());

        assertThrows(RehireEmployeeConflictException.class, () -> service.rehire(validCommand()));
        verify(createPresenceUseCase, never()).create(any(CreatePresenceCommand.class));
    }

    @Test
    void failsIfRehireDateIsEqualToLastClosedPresenceEndDate() {
        when(workCenterCompanyLookupPort.findCompanyCode("ESP", "MADRID_01", LocalDate.of(2026, 4, 15)))
                .thenReturn(Optional.of("ES01"));
        // Same-day terminate + rehire must be rejected
        when(getEmployeeByBusinessKeyUseCase.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(employee("TERMINATED")));
        when(listEmployeePresencesUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(closedPresence(LocalDate.of(2026, 4, 15))));
        when(listEmployeeContractsUseCase.listByEmployeeBusinessKey(any(ListEmployeeContractsCommand.class)))
                .thenReturn(List.of());
        when(listEmployeeLaborClassificationsUseCase.listByEmployeeBusinessKey(any(ListEmployeeLaborClassificationsCommand.class)))
                .thenReturn(List.of());
        when(listEmployeeWorkCentersUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of());

        assertThrows(RehireEmployeeConflictException.class, () -> service.rehire(validCommand()));
        verify(createPresenceUseCase, never()).create(any(CreatePresenceCommand.class));
    }

    @Test
    void failsIfRehireDateIsBeforeLastClosedPresenceEndDate() {
        RehireEmployeeCommand command = new RehireEmployeeCommand(
                "esp", "internal", "EMP001",
                LocalDate.of(2026, 3, 1),
                "rehire", "es01", "metal", "oficial_1", "con", "sub", "madrid_01",
                null,
                workingTimeCommand(new BigDecimal("80"))
        );

        when(workCenterCompanyLookupPort.findCompanyCode("ESP", "MADRID_01", LocalDate.of(2026, 3, 1)))
                .thenReturn(Optional.of("ES01"));

        when(getEmployeeByBusinessKeyUseCase.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(employee("TERMINATED")));
        when(listEmployeePresencesUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(closedPresence(LocalDate.of(2026, 3, 31))));
        when(listEmployeeContractsUseCase.listByEmployeeBusinessKey(any(ListEmployeeContractsCommand.class)))
                .thenReturn(List.of());
        when(listEmployeeLaborClassificationsUseCase.listByEmployeeBusinessKey(any(ListEmployeeLaborClassificationsCommand.class)))
                .thenReturn(List.of());
        when(listEmployeeWorkCentersUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of());

        assertThrows(RehireEmployeeConflictException.class, () -> service.rehire(command));
        verify(createPresenceUseCase, never()).create(any(CreatePresenceCommand.class));
    }

    @Test
    void returnsIdempotentSuccessWhenActiveCycleIsEquivalent() {
        Presence activePresence = activePresence(LocalDate.of(2026, 4, 15));
        when(workCenterCompanyLookupPort.findCompanyCode("ESP", "MADRID_01", LocalDate.of(2026, 4, 15)))
                .thenReturn(Optional.of("ES01"));

        when(getEmployeeByBusinessKeyUseCase.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(employee("ACTIVE")));
        when(listEmployeePresencesUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(activePresence));
        when(listEmployeeContractsUseCase.listByEmployeeBusinessKey(any(ListEmployeeContractsCommand.class)))
                .thenReturn(List.of(activeContract(LocalDate.of(2026, 4, 15))));
        when(listEmployeeLaborClassificationsUseCase.listByEmployeeBusinessKey(any(ListEmployeeLaborClassificationsCommand.class)))
                .thenReturn(List.of(activeLabor(LocalDate.of(2026, 4, 15))));
        when(listEmployeeWorkCentersUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(activeWorkCenter(LocalDate.of(2026, 4, 15))));
        when(listEmployeeWorkingTimesUseCase.listByEmployeeBusinessKey(any(ListEmployeeWorkingTimesCommand.class)))
                .thenReturn(List.of(activeWorkingTime(2, LocalDate.of(2026, 4, 15), new BigDecimal("80"))));

        RehireEmployeeResult result = service.rehire(validCommand());

        assertEquals("ACTIVE", result.status());
        assertEquals(1, result.newPresenceNumber());
        assertFalse(result.created());
        assertNotNull(result.newWorkingTime());
        assertEquals(0, new BigDecimal("80").compareTo(result.newWorkingTime().workingTimePercentage()));
        verify(createPresenceUseCase, never()).create(any(CreatePresenceCommand.class));
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void failsWithConflictWhenActiveCycleIsNotEquivalent() {
        when(workCenterCompanyLookupPort.findCompanyCode("ESP", "MADRID_01", LocalDate.of(2026, 4, 15)))
                .thenReturn(Optional.of("ES01"));
        when(getEmployeeByBusinessKeyUseCase.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(employee("ACTIVE")));
        when(listEmployeePresencesUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(activePresence(LocalDate.of(2026, 4, 15))));
        when(listEmployeeContractsUseCase.listByEmployeeBusinessKey(any(ListEmployeeContractsCommand.class)))
                .thenReturn(List.of(activeContract(LocalDate.of(2026, 4, 15))));
        when(listEmployeeLaborClassificationsUseCase.listByEmployeeBusinessKey(any(ListEmployeeLaborClassificationsCommand.class)))
                .thenReturn(List.of(activeLabor(LocalDate.of(2026, 4, 15))));
        when(listEmployeeWorkCentersUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(new WorkCenter(
                        20L, 100L, 1, "WC9",
                        LocalDate.of(2026, 4, 15), null,
                        LocalDateTime.now(), LocalDateTime.now()
                )));
        when(listEmployeeWorkingTimesUseCase.listByEmployeeBusinessKey(any(ListEmployeeWorkingTimesCommand.class)))
                .thenReturn(List.of(activeWorkingTime(2, LocalDate.of(2026, 4, 15), new BigDecimal("80"))));

        assertThrows(RehireEmployeeConflictException.class, () -> service.rehire(validCommand()));
    }

    @Test
    void rehiresUsingLatestClosedPresenceAsTemporalReferenceAcrossMultiplePreviousCycles() {
        RehireEmployeeCommand command = new RehireEmployeeCommand(
                "esp", "internal", "EMP001",
                LocalDate.of(2026, 6, 1),
                "rehire", "es01", "metal", "oficial_1", "con", "sub", "madrid_01",
                null,
                workingTimeCommand(new BigDecimal("80"))
        );

        when(workCenterCompanyLookupPort.findCompanyCode("ESP", "MADRID_01", LocalDate.of(2026, 6, 1)))
                .thenReturn(Optional.of("ES01"));

        Presence firstCycleClosed = new Presence(
                10L, 100L, 1, "ES01", "HIRE", "VOL",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                LocalDateTime.now(), LocalDateTime.now()
        );
        Presence secondCycleClosed = new Presence(
                11L, 100L, 2, "ES01", "REHIRE", "VOL",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 31),
                LocalDateTime.now(), LocalDateTime.now()
        );
        Presence thirdCycleActive = new Presence(
                12L, 100L, 3, "ES01", "REHIRE", null,
                LocalDate.of(2026, 6, 1), null,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(getEmployeeByBusinessKeyUseCase.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(employee("TERMINATED")));
        when(listEmployeePresencesUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(firstCycleClosed, secondCycleClosed), List.of(thirdCycleActive));
        when(listEmployeeContractsUseCase.listByEmployeeBusinessKey(any(ListEmployeeContractsCommand.class)))
                .thenReturn(List.of(
                        new Contract(100L, "CON", "SUB", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)),
                        new Contract(100L, "CON", "SUB", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 31))
                ));
        when(listEmployeeLaborClassificationsUseCase.listByEmployeeBusinessKey(any(ListEmployeeLaborClassificationsCommand.class)))
                .thenReturn(List.of(
                        new LaborClassification(100L, "METAL", "OFICIAL_1", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)),
                        new LaborClassification(100L, "METAL", "OFICIAL_1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 31))
                ));
        when(listEmployeeWorkCentersUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(
                        new WorkCenter(30L, 100L, 1, "MADRID_01", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), LocalDateTime.now(), LocalDateTime.now()),
                        new WorkCenter(31L, 100L, 2, "MADRID_01", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 31), LocalDateTime.now(), LocalDateTime.now())
                ));
        when(listEmployeeWorkingTimesUseCase.listByEmployeeBusinessKey(any(ListEmployeeWorkingTimesCommand.class)))
                .thenReturn(List.of(
                        closedWorkingTime(1, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), new BigDecimal("100")),
                        closedWorkingTime(2, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 31), new BigDecimal("60"))
                ));
        when(createPresenceUseCase.create(any(CreatePresenceCommand.class))).thenReturn(thirdCycleActive);
        when(createLaborClassificationUseCase.create(any(CreateLaborClassificationCommand.class))).thenReturn(activeLabor(LocalDate.of(2026, 6, 1)));
        when(createContractUseCase.create(any(CreateContractCommand.class))).thenReturn(activeContract(LocalDate.of(2026, 6, 1)));
        when(createWorkCenterUseCase.create(any(CreateWorkCenterCommand.class))).thenReturn(new WorkCenter(
                32L, 100L, 3, "MADRID_01",
                LocalDate.of(2026, 6, 1), null,
                LocalDateTime.now(), LocalDateTime.now()
        ));
        when(createWorkingTimeUseCase.create(any(CreateWorkingTimeCommand.class)))
                .thenReturn(activeWorkingTime(3, LocalDate.of(2026, 6, 1), new BigDecimal("80")));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee("ACTIVE"));

        RehireEmployeeResult result = service.rehire(command);

        assertEquals(LocalDate.of(2026, 6, 1), result.rehireDate());
        assertEquals(3, result.newPresenceNumber());
        assertEquals(3, result.newWorkCenterAssignmentNumber());
        assertTrue(result.created());
        assertEquals(3, result.newWorkingTime().workingTimeNumber());
    }

    @Test
    void failsWhenRehireDateIsAfterOlderCycleButNotAfterLatestClosedPresence() {
        RehireEmployeeCommand command = new RehireEmployeeCommand(
                "esp", "internal", "EMP001",
                LocalDate.of(2026, 4, 1),
                "rehire", "es01", "metal", "oficial_1", "con", "sub", "madrid_01",
                null,
                workingTimeCommand(new BigDecimal("80"))
        );
        when(workCenterCompanyLookupPort.findCompanyCode("ESP", "MADRID_01", LocalDate.of(2026, 4, 1)))
                .thenReturn(Optional.of("ES01"));

        Presence olderClosed = new Presence(
                10L, 100L, 1, "ES01", "HIRE", "VOL",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                LocalDateTime.now(), LocalDateTime.now()
        );
        Presence latestClosed = new Presence(
                11L, 100L, 2, "ES01", "REHIRE", "VOL",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 31),
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(getEmployeeByBusinessKeyUseCase.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(employee("TERMINATED")));
        when(listEmployeePresencesUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(olderClosed, latestClosed));
        when(listEmployeeContractsUseCase.listByEmployeeBusinessKey(any(ListEmployeeContractsCommand.class)))
                .thenReturn(List.of());
        when(listEmployeeLaborClassificationsUseCase.listByEmployeeBusinessKey(any(ListEmployeeLaborClassificationsCommand.class)))
                .thenReturn(List.of());
        when(listEmployeeWorkCentersUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of());

        assertThrows(RehireEmployeeConflictException.class, () -> service.rehire(command));
        verify(createPresenceUseCase, never()).create(any(CreatePresenceCommand.class));
    }

    @Test
    void rehiresWithOptionalCostCenterDistribution() {
        RehireEmployeeCommand command = new RehireEmployeeCommand(
                "esp", "internal", "EMP001",
                LocalDate.of(2026, 4, 15),
                "rehire", "es01", "metal", "oficial_1", "con", "sub", "madrid_01",
                new RehireEmployeeCommand.RehireEmployeeCostCenterDistributionCommand(
                        List.of(
                                new RehireEmployeeCommand.RehireEmployeeCostCenterItemCommand("CC1", new BigDecimal("60.0")),
                                new RehireEmployeeCommand.RehireEmployeeCostCenterItemCommand("CC2", new BigDecimal("40.0"))
                        )
                ),
                workingTimeCommand(new BigDecimal("80"))
        );
        when(workCenterCompanyLookupPort.findCompanyCode("ESP", "MADRID_01", LocalDate.of(2026, 4, 15)))
                .thenReturn(Optional.of("ES01"));

        Presence closedPresence = closedPresence(LocalDate.of(2026, 3, 31));
        Presence newPresence = activePresence(LocalDate.of(2026, 4, 15));
        CostCenterDistributionWindow costCenterWindow = new CostCenterDistributionWindow(
                LocalDate.of(2026, 4, 15),
                null,
                List.of(
                        new CostCenterAllocation(100L, "CC1", BigDecimal.valueOf(60), LocalDate.of(2026, 4, 15), null),
                        new CostCenterAllocation(100L, "CC2", BigDecimal.valueOf(40), LocalDate.of(2026, 4, 15), null)
                )
        );

        when(getEmployeeByBusinessKeyUseCase.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(employee("TERMINATED")));
        when(listEmployeePresencesUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(closedPresence), List.of(newPresence));
        when(listEmployeeContractsUseCase.listByEmployeeBusinessKey(any(ListEmployeeContractsCommand.class)))
                .thenReturn(List.of(new Contract(1L, "CON", "SUB", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))));
        when(listEmployeeLaborClassificationsUseCase.listByEmployeeBusinessKey(any(ListEmployeeLaborClassificationsCommand.class)))
                .thenReturn(List.of(new LaborClassification(1L, "AGR", "CAT", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))));
        when(listEmployeeWorkCentersUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(new WorkCenter(10L, 1L, 1, "WC1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), LocalDateTime.now(), LocalDateTime.now())));
        when(createPresenceUseCase.create(any(CreatePresenceCommand.class))).thenReturn(newPresence);
        when(createLaborClassificationUseCase.create(any(CreateLaborClassificationCommand.class))).thenReturn(activeLabor(LocalDate.of(2026, 4, 15)));
        when(createContractUseCase.create(any(CreateContractCommand.class))).thenReturn(activeContract(LocalDate.of(2026, 4, 15)));
        when(createWorkCenterUseCase.create(any(CreateWorkCenterCommand.class))).thenReturn(activeWorkCenter(LocalDate.of(2026, 4, 15)));
        when(createCostCenterDistributionUseCase.create(any(CreateCostCenterDistributionCommand.class))).thenReturn(costCenterWindow);
        when(createWorkingTimeUseCase.create(any(CreateWorkingTimeCommand.class)))
                .thenReturn(activeWorkingTime(2, LocalDate.of(2026, 4, 15), new BigDecimal("80")));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee("ACTIVE"));

        RehireEmployeeResult result = service.rehire(command);

        assertNotNull(result.newCostCenter());
        assertEquals(LocalDate.of(2026, 4, 15), result.newCostCenter().startDate());
        assertEquals(100.0, result.newCostCenter().totalAllocationPercentage());
        assertEquals(2, result.newCostCenter().items().size());

        ArgumentCaptor<CreateCostCenterDistributionCommand> costCenterCaptor =
                ArgumentCaptor.forClass(CreateCostCenterDistributionCommand.class);
        verify(createCostCenterDistributionUseCase).create(costCenterCaptor.capture());
        assertEquals(LocalDate.of(2026, 4, 15), costCenterCaptor.getValue().startDate());
        assertEquals(2, costCenterCaptor.getValue().items().size());
    }

    @Test
    void failsWhenCostCenterDistributionIsInvalid() {
        RehireEmployeeCommand command = new RehireEmployeeCommand(
                "esp", "internal", "EMP001",
                LocalDate.of(2026, 4, 15),
                "rehire", "es01", "metal", "oficial_1", "con", "sub", "madrid_01",
                new RehireEmployeeCommand.RehireEmployeeCostCenterDistributionCommand(
                        List.of(
                                new RehireEmployeeCommand.RehireEmployeeCostCenterItemCommand("CC1", new BigDecimal("80.0")),
                                new RehireEmployeeCommand.RehireEmployeeCostCenterItemCommand("CC2", new BigDecimal("80.0"))
                        )
                ),
                workingTimeCommand(new BigDecimal("80"))
        );
        when(workCenterCompanyLookupPort.findCompanyCode("ESP", "MADRID_01", LocalDate.of(2026, 4, 15)))
                .thenReturn(Optional.of("ES01"));

        Presence closedPresence = closedPresence(LocalDate.of(2026, 3, 31));

        when(getEmployeeByBusinessKeyUseCase.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(employee("TERMINATED")));
        when(listEmployeePresencesUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(closedPresence));
        when(listEmployeeContractsUseCase.listByEmployeeBusinessKey(any(ListEmployeeContractsCommand.class)))
                .thenReturn(List.of());
        when(listEmployeeLaborClassificationsUseCase.listByEmployeeBusinessKey(any(ListEmployeeLaborClassificationsCommand.class)))
                .thenReturn(List.of());
        when(listEmployeeWorkCentersUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of());
        when(createPresenceUseCase.create(any(CreatePresenceCommand.class))).thenReturn(activePresence(LocalDate.of(2026, 4, 15)));
        when(createLaborClassificationUseCase.create(any(CreateLaborClassificationCommand.class))).thenReturn(activeLabor(LocalDate.of(2026, 4, 15)));
        when(createContractUseCase.create(any(CreateContractCommand.class))).thenReturn(activeContract(LocalDate.of(2026, 4, 15)));
        when(createWorkCenterUseCase.create(any(CreateWorkCenterCommand.class))).thenReturn(activeWorkCenter(LocalDate.of(2026, 4, 15)));
        when(createCostCenterDistributionUseCase.create(any(CreateCostCenterDistributionCommand.class)))
                .thenThrow(new CostCenterDistributionInvalidException("total allocation percentage exceeds 100"));

        assertThrows(RehireEmployeeDistributionInvalidException.class, () -> service.rehire(command));
    }

    @Test
    void activatesEmployeeUsingDomainBehaviorRatherThanManualReconstruction() {
        Presence closedPresence = closedPresence(LocalDate.of(2026, 3, 31));
        Presence newPresence = activePresence(LocalDate.of(2026, 4, 15));
        when(workCenterCompanyLookupPort.findCompanyCode("ESP", "MADRID_01", LocalDate.of(2026, 4, 15)))
                .thenReturn(Optional.of("ES01"));

        when(getEmployeeByBusinessKeyUseCase.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(employee("TERMINATED")));
        when(listEmployeePresencesUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(closedPresence), List.of(newPresence));
        when(listEmployeeContractsUseCase.listByEmployeeBusinessKey(any(ListEmployeeContractsCommand.class)))
                .thenReturn(List.of(new Contract(1L, "CON", "SUB", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))));
        when(listEmployeeLaborClassificationsUseCase.listByEmployeeBusinessKey(any(ListEmployeeLaborClassificationsCommand.class)))
                .thenReturn(List.of(new LaborClassification(1L, "AGR", "CAT", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))));
        when(listEmployeeWorkCentersUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(new WorkCenter(10L, 1L, 1, "WC1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), LocalDateTime.now(), LocalDateTime.now())));
        when(createPresenceUseCase.create(any(CreatePresenceCommand.class))).thenReturn(newPresence);
        when(createLaborClassificationUseCase.create(any(CreateLaborClassificationCommand.class))).thenReturn(activeLabor(LocalDate.of(2026, 4, 15)));
        when(createContractUseCase.create(any(CreateContractCommand.class))).thenReturn(activeContract(LocalDate.of(2026, 4, 15)));
        when(createWorkCenterUseCase.create(any(CreateWorkCenterCommand.class))).thenReturn(activeWorkCenter(LocalDate.of(2026, 4, 15)));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RehireEmployeeResult result = service.rehire(validCommand());

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(employeeCaptor.capture());
        assertEquals("ACTIVE", employeeCaptor.getValue().getStatus());
        assertEquals(Long.valueOf(100L), employeeCaptor.getValue().getId());
        assertEquals("ESP", employeeCaptor.getValue().getRuleSystemCode());
        assertEquals("ACTIVE", result.status());
    }

    @Test
    void failsWhenCostCenterDistributionHasNullAllocationPercentage() {
        RehireEmployeeCommand command = new RehireEmployeeCommand(
                "esp", "internal", "EMP001",
                LocalDate.of(2026, 4, 15),
                "rehire", "es01", "metal", "oficial_1", "con", "sub", "madrid_01",
                new RehireEmployeeCommand.RehireEmployeeCostCenterDistributionCommand(
                        List.of(
                                new RehireEmployeeCommand.RehireEmployeeCostCenterItemCommand("CC1", null)
                        )
                ),
                workingTimeCommand(new BigDecimal("80"))
        );
        when(workCenterCompanyLookupPort.findCompanyCode("ESP", "MADRID_01", LocalDate.of(2026, 4, 15)))
                .thenReturn(Optional.of("ES01"));

        Presence closedPresence = closedPresence(LocalDate.of(2026, 3, 31));

        when(getEmployeeByBusinessKeyUseCase.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(employee("TERMINATED")));
        when(listEmployeePresencesUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(closedPresence));
        when(listEmployeeContractsUseCase.listByEmployeeBusinessKey(any(ListEmployeeContractsCommand.class)))
                .thenReturn(List.of());
        when(listEmployeeLaborClassificationsUseCase.listByEmployeeBusinessKey(any(ListEmployeeLaborClassificationsCommand.class)))
                .thenReturn(List.of());
        when(listEmployeeWorkCentersUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of());
        when(createPresenceUseCase.create(any(CreatePresenceCommand.class))).thenReturn(activePresence(LocalDate.of(2026, 4, 15)));
        when(createLaborClassificationUseCase.create(any(CreateLaborClassificationCommand.class))).thenReturn(activeLabor(LocalDate.of(2026, 4, 15)));
        when(createContractUseCase.create(any(CreateContractCommand.class))).thenReturn(activeContract(LocalDate.of(2026, 4, 15)));
        when(createWorkCenterUseCase.create(any(CreateWorkCenterCommand.class))).thenReturn(activeWorkCenter(LocalDate.of(2026, 4, 15)));

        assertThrows(RehireEmployeeDistributionInvalidException.class, () -> service.rehire(command));
        verify(createCostCenterDistributionUseCase, never()).create(any(CreateCostCenterDistributionCommand.class));
    }

    @Test
    void failsWhenCostCenterDistributionHasEmptyItems() {
        RehireEmployeeCommand command = new RehireEmployeeCommand(
                "esp", "internal", "EMP001",
                LocalDate.of(2026, 4, 15),
                "rehire", "es01", "metal", "oficial_1", "con", "sub", "madrid_01",
                new RehireEmployeeCommand.RehireEmployeeCostCenterDistributionCommand(List.of()),
                workingTimeCommand(new BigDecimal("80"))
        );
        when(workCenterCompanyLookupPort.findCompanyCode("ESP", "MADRID_01", LocalDate.of(2026, 4, 15)))
                .thenReturn(Optional.of("ES01"));

        Presence closedPresence = closedPresence(LocalDate.of(2026, 3, 31));

        when(getEmployeeByBusinessKeyUseCase.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(employee("TERMINATED")));
        when(listEmployeePresencesUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(closedPresence));
        when(listEmployeeContractsUseCase.listByEmployeeBusinessKey(any(ListEmployeeContractsCommand.class)))
                .thenReturn(List.of());
        when(listEmployeeLaborClassificationsUseCase.listByEmployeeBusinessKey(any(ListEmployeeLaborClassificationsCommand.class)))
                .thenReturn(List.of());
        when(listEmployeeWorkCentersUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of());
        when(createPresenceUseCase.create(any(CreatePresenceCommand.class))).thenReturn(activePresence(LocalDate.of(2026, 4, 15)));
        when(createLaborClassificationUseCase.create(any(CreateLaborClassificationCommand.class))).thenReturn(activeLabor(LocalDate.of(2026, 4, 15)));
        when(createContractUseCase.create(any(CreateContractCommand.class))).thenReturn(activeContract(LocalDate.of(2026, 4, 15)));
        when(createWorkCenterUseCase.create(any(CreateWorkCenterCommand.class))).thenReturn(activeWorkCenter(LocalDate.of(2026, 4, 15)));

        assertThrows(RehireEmployeeDistributionInvalidException.class, () -> service.rehire(command));
        verify(createCostCenterDistributionUseCase, never()).create(any(CreateCostCenterDistributionCommand.class));
    }

    private RehireEmployeeCommand validCommand() {
        return new RehireEmployeeCommand(
                "esp",
                "internal",
                "EMP001",
                LocalDate.of(2026, 4, 15),
                "rehire",
                "es01",
                "metal",
                "oficial_1",
                "con",
                "sub",
                "madrid_01",
                                null,
                                workingTimeCommand(new BigDecimal("80"))
        );
    }

        private RehireEmployeeCommand.RehireEmployeeWorkingTimeCommand workingTimeCommand(BigDecimal percentage) {
                return new RehireEmployeeCommand.RehireEmployeeWorkingTimeCommand(percentage);
        }

    private Employee employee(String status) {
        return new Employee(
                100L,
                "ESP",
                "INTERNAL",
                "EMP001",
                "Ana",
                "Lopez",
                null,
                "Ani",
                status,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );
    }

    private Presence closedPresence(LocalDate endDate) {
        return new Presence(
                10L, 100L, 1, "ES01", "HIRE", "VOL",
                LocalDate.of(2026, 1, 1), endDate,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    private Presence activePresence(LocalDate startDate) {
        return new Presence(
                10L, 100L, 1, "ES01", "REHIRE", null,
                startDate, null,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    private Contract activeContract(LocalDate startDate) {
        return new Contract(100L, "CON", "SUB", startDate, null);
    }

    private LaborClassification activeLabor(LocalDate startDate) {
        return new LaborClassification(100L, "METAL", "OFICIAL_1", startDate, null);
    }

    private WorkCenter activeWorkCenter(LocalDate startDate) {
        return new WorkCenter(
                20L, 100L, 1, "MADRID_01",
                startDate, null,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

        private WorkingTime closedWorkingTime(int workingTimeNumber, LocalDate startDate, LocalDate endDate, BigDecimal percentage) {
                return workingTime(workingTimeNumber, startDate, endDate, percentage);
        }

        private WorkingTime activeWorkingTime(int workingTimeNumber, LocalDate startDate, BigDecimal percentage) {
                return workingTime(workingTimeNumber, startDate, null, percentage);
        }

        private WorkingTime workingTime(int workingTimeNumber, LocalDate startDate, LocalDate endDate, BigDecimal percentage) {
                return WorkingTime.rehydrate(
                                (long) workingTimeNumber,
                                100L,
                                workingTimeNumber,
                                startDate,
                                endDate,
                                percentage,
                                deriveHours(percentage),
                                LocalDateTime.now(),
                                LocalDateTime.now()
                );
        }

        private WorkingTimeDerivedHours deriveHours(BigDecimal percentage) {
                BigDecimal factor = percentage.divide(new BigDecimal("100"));
                return new WorkingTimeDerivedHours(
                                factor.multiply(new BigDecimal("40")).setScale(2, java.math.RoundingMode.HALF_UP),
                                factor.multiply(new BigDecimal("8")).setScale(2, java.math.RoundingMode.HALF_UP),
                                factor.multiply(new BigDecimal("166.67")).setScale(2, java.math.RoundingMode.HALF_UP)
                );
        }
}

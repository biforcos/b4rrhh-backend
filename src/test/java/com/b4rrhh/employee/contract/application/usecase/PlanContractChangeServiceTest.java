package com.b4rrhh.employee.contract.application.usecase;

import com.b4rrhh.employee.contract.application.command.PlanContractChangeCommand;
import com.b4rrhh.employee.contract.application.model.ContractPlan;
import com.b4rrhh.employee.contract.application.port.ContractPresenceConsistencyPort;
import com.b4rrhh.employee.contract.application.port.EmployeeContractContext;
import com.b4rrhh.employee.contract.application.port.EmployeeContractLookupPort;
import com.b4rrhh.employee.contract.application.port.PresencePeriod;
import com.b4rrhh.employee.contract.application.service.ContractTimelineService;
import com.b4rrhh.employee.contract.domain.exception.ContractEmployeeNotFoundException;
import com.b4rrhh.employee.contract.domain.exception.ContractNotFoundException;
import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.contract.domain.model.ContractPeriod;
import com.b4rrhh.employee.contract.domain.port.ContractRepository;
import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelineRejection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The plan is asked for and nothing is written. The employee is present from
 * 2026-01-01 onwards with one open contract from that day.
 */
@ExtendWith(MockitoExtension.class)
class PlanContractChangeServiceTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String EMPLOYEE_NUMBER = "EMP001";
    private static final LocalDate PRESENCE_START = LocalDate.of(2026, 1, 1);

    @Mock
    private ContractRepository contractRepository;
    @Mock
    private EmployeeContractLookupPort employeeContractLookupPort;
    @Mock
    private ContractPresenceConsistencyPort presencePort;

    private PlanContractChangeService service;

    @BeforeEach
    void setUp() {
        service = new PlanContractChangeService(
                contractRepository,
                employeeContractLookupPort,
                new ContractTimelineService(contractRepository, presencePort)
        );
    }

    @Test
    void plansAnAddWithoutWritingAnything() {
        givenEmployeeWithSeries(contract(PRESENCE_START, null));

        ContractPlan plan = service.plan(command(TimelineOperation.ADD, null, LocalDate.of(2026, 1, 16), null));

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.ADD, plan.operation());
        assertEquals(PRESENCE_START, plan.adjustedOccurrence().before().startDate());
        assertEquals(LocalDate.of(2026, 1, 15), plan.adjustedOccurrence().after().endDate());
        assertEquals(
                List.of(
                        new ContractPeriod(PRESENCE_START, LocalDate.of(2026, 1, 15)),
                        new ContractPeriod(LocalDate.of(2026, 1, 16), null)
                ),
                plan.projected()
        );
        verify(contractRepository, never()).save(any());
        verify(contractRepository, never()).update(any(), any());
        verify(contractRepository, never()).delete(any());
    }

    @Test
    void aRejectedPlanComesBackAsAPlanNotAsAnError() {
        givenEmployeeWithSeries(contract(PRESENCE_START, LocalDate.of(2026, 1, 31)));

        ContractPlan plan = service.plan(command(TimelineOperation.ADD, null, LocalDate.of(2026, 3, 1), null));

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
        assertEquals(List.of(new ContractPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))), plan.gaps());
    }

    @Test
    void thePlanSaysAnAddOnAnExistingStartDateIsACorrectionOfThatContract() {
        givenEmployeeWithSeries(contract(PRESENCE_START, null));

        ContractPlan plan = service.plan(command(TimelineOperation.ADD, null, PRESENCE_START, LocalDate.of(2026, 1, 15)));

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.IS_A_CORRECTION, plan.rejection());
        assertEquals(TimelineOperation.CORRECT, plan.operation());
        assertEquals(new ContractPeriod(PRESENCE_START, null), plan.correctedOccurrence());
        assertEquals(List.of(new ContractPeriod(PRESENCE_START, LocalDate.of(2026, 1, 15))), plan.projected());
    }

    @Test
    void plansARemovalOfTheContractIdentifiedByItsStartDate() {
        Contract first = contract(PRESENCE_START, LocalDate.of(2026, 1, 15));
        Contract last = contract(LocalDate.of(2026, 1, 16), null);
        givenEmployeeWithSeries(first, last);
        when(contractRepository.findByEmployeeIdAndStartDate(10L, last.getStartDate())).thenReturn(Optional.of(last));

        ContractPlan plan = service.plan(command(TimelineOperation.REMOVE, LocalDate.of(2026, 1, 16), null, null));

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.REMOVE, plan.operation());
        assertEquals(PRESENCE_START, plan.adjustedOccurrence().before().startDate());
        assertEquals(List.of(new ContractPeriod(PRESENCE_START, null)), plan.projected());
        verify(contractRepository, never()).delete(any());
    }

    @Test
    void plansACorrectionOfTheContractIdentifiedByItsStartDate() {
        Contract closedTooEarly = contract(PRESENCE_START, LocalDate.of(2026, 1, 31));
        givenEmployeeWithSeries(closedTooEarly);
        when(contractRepository.findByEmployeeIdAndStartDate(10L, PRESENCE_START)).thenReturn(Optional.of(closedTooEarly));

        ContractPlan plan = service.plan(command(TimelineOperation.CORRECT, PRESENCE_START, PRESENCE_START, null));

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.CORRECT, plan.operation());
        assertEquals(List.of(new ContractPeriod(PRESENCE_START, null)), plan.projected());
        verify(contractRepository, never()).update(any(), any());
    }

    @Test
    void anAddNeedsAStartDate() {
        whenEmployeeExists();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.plan(command(TimelineOperation.ADD, null, null, null))
        );
    }

    @Test
    void aRemovalNeedsTheStartDateOfAnExistingContract() {
        whenEmployeeExists();
        when(contractRepository.findByEmployeeIdAndStartDate(10L, LocalDate.of(2026, 9, 1))).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.plan(command(TimelineOperation.REMOVE, null, null, null))
        );
        assertThrows(
                ContractNotFoundException.class,
                () -> service.plan(command(TimelineOperation.REMOVE, LocalDate.of(2026, 9, 1), null, null))
        );
    }

    @Test
    void theOperationIsRequired() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.plan(command(null, null, PRESENCE_START, null))
        );
    }

    @Test
    void rejectsWhenTheEmployeeDoesNotExist() {
        when(employeeContractLookupPort.findByBusinessKey(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.empty());

        assertThrows(
                ContractEmployeeNotFoundException.class,
                () -> service.plan(command(TimelineOperation.ADD, null, PRESENCE_START, null))
        );
    }

    private PlanContractChangeCommand command(
            TimelineOperation operation,
            LocalDate contractStartDate,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new PlanContractChangeCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER, operation, contractStartDate, startDate, endDate
        );
    }

    private void givenEmployeeWithSeries(Contract... occurrences) {
        whenEmployeeExists();
        when(contractRepository.findByEmployeeIdOrderByStartDate(10L)).thenReturn(List.of(occurrences));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L))
                .thenReturn(List.of(new PresencePeriod(PRESENCE_START, null)));
    }

    private void whenEmployeeExists() {
        when(employeeContractLookupPort.findByBusinessKey(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(new EmployeeContractContext(10L, RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER)));
    }

    private static Contract contract(LocalDate startDate, LocalDate endDate) {
        return new Contract(10L, "IND", "FT1", startDate, endDate);
    }
}

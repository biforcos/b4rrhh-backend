package com.b4rrhh.employee.lifecycle.application.service;

import com.b4rrhh.employee.contract.application.usecase.ListEmployeeContractsUseCase;
import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.labor_classification.application.usecase.ListEmployeeLaborClassificationsUseCase;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.lifecycle.application.command.TerminateEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.TerminateEmployeeResult;
import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeEmployeeNotFoundException;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeRequestInvalidException;
import com.b4rrhh.employee.presence.application.usecase.ListEmployeePresencesUseCase;
import com.b4rrhh.employee.presence.domain.model.Presence;
import com.b4rrhh.employee.workcenter.application.usecase.ListEmployeeWorkCentersUseCase;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesUseCase;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimeDerivedHours;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerminationPreConditionValidatorTest {

    @Mock private GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKey;
    @Mock private ListEmployeePresencesUseCase listPresences;
    @Mock private ListEmployeeContractsUseCase listContracts;
    @Mock private ListEmployeeLaborClassificationsUseCase listLaborClassifications;
    @Mock private ListEmployeeWorkCentersUseCase listWorkCenters;
    @Mock private ListEmployeeWorkingTimesUseCase listWorkingTimes;

    private TerminationPreConditionValidator validator;

    private static final LocalDate TERMINATION_DATE = LocalDate.of(2026, 3, 31);
    private static final LocalDate START_DATE = LocalDate.of(2026, 1, 1);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);

    @BeforeEach
    void setUp() {
        validator = new TerminationPreConditionValidator(
                getEmployeeByBusinessKey, listPresences, listContracts,
                listLaborClassifications, listWorkCenters, listWorkingTimes);
    }

    @Test
    void throwsWhenRuleSystemCodeMissing() {
        assertThrows(TerminateEmployeeRequestInvalidException.class,
                () -> validator.validateAndLookup(cmd(null, "INTERNAL", "EMP001")));
    }

    @Test
    void throwsWhenEmployeeTypeCodeMissing() {
        assertThrows(TerminateEmployeeRequestInvalidException.class,
                () -> validator.validateAndLookup(cmd("ESP", "", "EMP001")));
    }

    @Test
    void throwsWhenEmployeeNumberMissing() {
        assertThrows(TerminateEmployeeRequestInvalidException.class,
                () -> validator.validateAndLookup(cmd("ESP", "INTERNAL", "  ")));
    }

    @Test
    void throwsWhenTerminationDateNull() {
        TerminateEmployeeCommand command = new TerminateEmployeeCommand(
                "ESP", "INTERNAL", "EMP001", null, "VOL");
        assertThrows(TerminateEmployeeRequestInvalidException.class,
                () -> validator.validateAndLookup(command));
    }

    @Test
    void throwsWhenExitReasonCodeMissing() {
        TerminateEmployeeCommand command = new TerminateEmployeeCommand(
                "ESP", "INTERNAL", "EMP001", TERMINATION_DATE, null);
        assertThrows(TerminateEmployeeRequestInvalidException.class,
                () -> validator.validateAndLookup(command));
    }

    @Test
    void throwsWhenEmployeeNotFound() {
        when(getEmployeeByBusinessKey.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.empty());
        assertThrows(TerminateEmployeeEmployeeNotFoundException.class,
                () -> validator.validateAndLookup(cmd("ESP", "INTERNAL", "EMP001")));
    }

    @Test
    void returnsActiveContextWhenEmployeeIsActive() {
        Employee active = employee();
        when(getEmployeeByBusinessKey.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(active));

        TerminationContext ctx = validator.validateAndLookup(cmd("ESP", "INTERNAL", "EMP001"));

        assertFalse(ctx.isAlreadyTerminated());
        assertSame(active, ctx.employee());
        assertEquals("ESP", ctx.ruleSystemCode());
        assertEquals(TERMINATION_DATE, ctx.terminationDate());
        assertEquals("VOL", ctx.exitReasonCode());
    }

    @Test
    void normalizesInputsToUpperCase() {
        Employee active = employee();
        when(getEmployeeByBusinessKey.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(active));

        TerminationContext ctx = validator.validateAndLookup(
                new TerminateEmployeeCommand(" esp ", " internal ", " emp001 ", TERMINATION_DATE, " vol "));

        assertEquals("ESP", ctx.ruleSystemCode());
        assertEquals("INTERNAL", ctx.employeeTypeCode());
        assertEquals("EMP001", ctx.employeeNumber());
        assertEquals("VOL", ctx.exitReasonCode());
    }

    @Test
    void returnsIdempotentContextWhenEmployeeAlreadyTerminated() {
        Employee terminated = employee();
        when(getEmployeeByBusinessKey.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(terminated));

        Presence presence = closedPresence(1, TERMINATION_DATE, "VOL");
        Contract contract = new Contract(100L, "IND", "FT1", START_DATE, TERMINATION_DATE);
        LaborClassification lc = new LaborClassification(100L, "AGR", "CAT", START_DATE, TERMINATION_DATE);
        WorkCenter wc = new WorkCenter(20L, 100L, 1, "WC1", START_DATE, TERMINATION_DATE, NOW, NOW);
        WorkingTime wt = WorkingTime.rehydrate(30L, 100L, 1, START_DATE, TERMINATION_DATE,
                new BigDecimal("75"),
                new WorkingTimeDerivedHours(new BigDecimal("30"), new BigDecimal("6"), new BigDecimal("130")),
                NOW, NOW);

        when(listPresences.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(presence));
        when(listContracts.listByEmployeeBusinessKey(any())).thenReturn(List.of(contract));
        when(listLaborClassifications.listByEmployeeBusinessKey(any())).thenReturn(List.of(lc));
        when(listWorkCenters.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(wc));
        when(listWorkingTimes.listByEmployeeBusinessKey(any())).thenReturn(List.of(wt));

        TerminationContext ctx = validator.validateAndLookup(cmd("ESP", "INTERNAL", "EMP001"));

        assertTrue(ctx.isAlreadyTerminated());
        TerminateEmployeeResult result = ctx.reconstructIdempotentResult();
        assertEquals("ESP", result.ruleSystemCode());
        assertEquals("TERMINATED", result.status());
        assertEquals(1, result.closedPresenceNumber());
        assertEquals("IND", result.closedContractTypeCode());
        assertEquals("AGR", result.closedAgreementCode());
        assertEquals(1, result.closedWorkCenterAssignmentNumber());
        assertEquals(1, result.closedWorkingTimeNumber());
    }

    @Test
    void idempotentContextFiltersRecordsByTerminationDate() {
        Employee terminated = employee();
        when(getEmployeeByBusinessKey.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(terminated));

        Presence wrong = closedPresence(1, LocalDate.of(2025, 1, 1), "VOL");
        Presence right = closedPresence(2, TERMINATION_DATE, "VOL");
        when(listPresences.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(wrong, right));
        when(listContracts.listByEmployeeBusinessKey(any())).thenReturn(List.of());
        when(listLaborClassifications.listByEmployeeBusinessKey(any())).thenReturn(List.of());
        when(listWorkCenters.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of());
        when(listWorkingTimes.listByEmployeeBusinessKey(any())).thenReturn(List.of());

        TerminationContext ctx = validator.validateAndLookup(cmd("ESP", "INTERNAL", "EMP001"));

        assertEquals(2, ctx.reconstructIdempotentResult().closedPresenceNumber());
    }

    @Test
    void idempotentContextExcludesPresenceWithWrongExitReasonCode() {
        Employee terminated = employee();
        when(getEmployeeByBusinessKey.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(terminated));

        // Presence with correct terminationDate but different exitReasonCode
        Presence wrongReason = new Presence(10L, 100L, 1, "COMP", "HIRE", "WRONG_REASON",
                TERMINATION_DATE.minusMonths(3), TERMINATION_DATE, NOW, NOW);
        when(listPresences.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(wrongReason));
        when(listContracts.listByEmployeeBusinessKey(any())).thenReturn(List.of());
        when(listLaborClassifications.listByEmployeeBusinessKey(any())).thenReturn(List.of());
        when(listWorkCenters.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of());
        when(listWorkingTimes.listByEmployeeBusinessKey(any())).thenReturn(List.of());

        TerminationContext ctx = validator.validateAndLookup(cmd("ESP", "INTERNAL", "EMP001"));

        assertTrue(ctx.isAlreadyTerminated());
        assertNull(ctx.reconstructIdempotentResult().closedPresenceNumber());
    }

    // --- helpers ---

    private TerminateEmployeeCommand cmd(String rs, String et, String en) {
        return new TerminateEmployeeCommand(rs, et, en, TERMINATION_DATE, "VOL");
    }

    /** El empleado ya no lleva estado: si esta cesado lo dicen sus presencias (b4rrhh/backend#148). */
    private Employee employee() {
        return new Employee(1L, "ESP", "INTERNAL", "EMP001",
                "Ana", "Lopez", null, null,
                NOW, NOW, null);
    }

    private Presence closedPresence(int number, LocalDate endDate, String exitReason) {
        LocalDate startDate = endDate.minusMonths(3);
        return new Presence(10L, 100L, number, "COMP", "HIRE", exitReason,
                startDate, endDate, NOW, NOW);
    }
}

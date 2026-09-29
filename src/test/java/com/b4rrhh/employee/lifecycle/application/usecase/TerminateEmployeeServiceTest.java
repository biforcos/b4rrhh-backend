package com.b4rrhh.employee.lifecycle.application.usecase;

import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.presence.domain.model.EmployeeStanding;
import com.b4rrhh.employee.employee.domain.port.EmployeeRepository;
import com.b4rrhh.employee.lifecycle.application.command.TerminateEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.TerminateEmployeeResult;
import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.application.port.TerminationParticipant;
import com.b4rrhh.employee.lifecycle.application.service.TerminationPreConditionValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TerminateEmployeeServiceTest {

    @Mock private TerminationPreConditionValidator validator;
    @Mock private EmployeeRepository employeeRepository;

    private final TerminationParticipant firstParticipant = mock(TerminationParticipant.class);
    private final TerminationParticipant secondParticipant = mock(TerminationParticipant.class);

    @Test
    void callsValidatorThenRunsParticipantsInOrder() {
        TerminationContext ctx = context();
        when(validator.validateAndLookup(any())).thenReturn(ctx);
        when(ctx.isAlreadyTerminated()).thenReturn(false);
        when(ctx.terminatedEmployee()).thenReturn(mock(Employee.class));
        when(firstParticipant.order()).thenReturn(10);
        when(secondParticipant.order()).thenReturn(20);

        service(List.of(secondParticipant, firstParticipant)).terminate(command());

        InOrder order = inOrder(validator, firstParticipant, secondParticipant);
        order.verify(validator).validateAndLookup(any());
        order.verify(firstParticipant).participate(ctx);
        order.verify(secondParticipant).participate(ctx);
    }

    @Test
    void returnsIdempotentResultWhenAlreadyTerminated() {
        TerminationContext ctx = context();
        when(validator.validateAndLookup(any())).thenReturn(ctx);
        when(ctx.isAlreadyTerminated()).thenReturn(true);
        when(ctx.reconstructIdempotentResult()).thenReturn(null);

        service(List.of(firstParticipant)).terminate(command());

        verify(firstParticipant, never()).participate(any());
        verify(ctx).reconstructIdempotentResult();
    }

    @Test
    void runsPostConditionCheckAfterParticipants() {
        TerminationContext ctx = context();
        when(validator.validateAndLookup(any())).thenReturn(ctx);
        when(ctx.isAlreadyTerminated()).thenReturn(false);
        when(ctx.terminatedEmployee()).thenReturn(mock(Employee.class));
        when(firstParticipant.order()).thenReturn(10);

        service(List.of(firstParticipant)).terminate(command());

        InOrder order = inOrder(firstParticipant, ctx);
        order.verify(firstParticipant).participate(ctx);
        order.verify(ctx).assertNoActivePresence();
    }

    @Test
    void savesTerminatedEmployeeAfterParticipants() {
        TerminationContext ctx = context();
        Employee terminatedEmployee = mock(Employee.class);
        when(validator.validateAndLookup(any())).thenReturn(ctx);
        when(ctx.isAlreadyTerminated()).thenReturn(false);
        when(ctx.terminatedEmployee()).thenReturn(terminatedEmployee);

        service(List.of()).terminate(command());

        InOrder order = inOrder(ctx, employeeRepository);
        order.verify(ctx).assertNoActivePresence();
        order.verify(employeeRepository).save(terminatedEmployee);
    }

    @Test
    void returnsResultFromContext() {
        TerminationContext ctx = context();
        TerminateEmployeeResult sentinel = sentinelResult();
        when(validator.validateAndLookup(any())).thenReturn(ctx);
        when(ctx.isAlreadyTerminated()).thenReturn(false);
        when(ctx.terminatedEmployee()).thenReturn(mock(Employee.class));
        when(ctx.toResult(any())).thenReturn(sentinel);

        TerminateEmployeeResult actual = service(List.of()).terminate(command());

        assertSame(sentinel, actual);
    }

    // --- helpers ---

    /** El servicio pide el estado de hoy con el id del empleado del contexto (b4rrhh/backend#148). */
    private TerminationContext context() {
        TerminationContext ctx = mock(TerminationContext.class);
        lenient().when(ctx.employee()).thenReturn(mock(Employee.class));
        return ctx;
    }

    private TerminateEmployeeService service(List<TerminationParticipant> participants) {
        return new TerminateEmployeeService(validator, participants, employeeRepository,
                (employeeId, date) -> EmployeeStanding.on(LocalDate.of(2026, 3, 1), List.of()));
    }

    private TerminateEmployeeCommand command() {
        return new TerminateEmployeeCommand(
                "ESP", "INTERNAL", "EMP001", LocalDate.of(2026, 3, 31), "VOL");
    }

    private TerminateEmployeeResult sentinelResult() {
        return new TerminateEmployeeResult(
                "ESP", "INTERNAL", "EMP001", LocalDate.of(2026, 3, 31), "VOL", "TERMINATED",
                1, "COMP", "ENTRY", "VOL", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31),
                "CNT", "SUB", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31),
                "AGR", "CAT", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31),
                1, "WC001", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31),
                1, BigDecimal.valueOf(100), BigDecimal.valueOf(40), BigDecimal.valueOf(8), BigDecimal.valueOf(160),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
    }
}

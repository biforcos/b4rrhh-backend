package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.HireContext;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeBusinessValidationException;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeConflictException;
import com.b4rrhh.employee.working_time.application.usecase.CreateWorkingTimeCommand;
import com.b4rrhh.employee.working_time.application.usecase.CreateWorkingTimeUseCase;
import com.b4rrhh.employee.working_time.domain.exception.InvalidWorkingTimePercentageException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeEmployeeNotFoundException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeIsACorrectionException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeNumberConflictException;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimeDerivedHours;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimeOccurrence;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimePeriod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkingTimeParticipantTest {

    @Mock
    private CreateWorkingTimeUseCase createWorkingTimeUseCase;

    @InjectMocks
    private WorkingTimeParticipant participant;

    @Test
    void orderIs70() {
        assertThat(participant.order()).isEqualTo(70);
    }

    @Test
    void createsWorkingTimeFromContextAndStoresResult() {
        HireContext ctx = validContext();
        LocalDate hireDate = ctx.hireDate();
        LocalDateTime fixedNow = LocalDateTime.of(2026, 3, 23, 0, 0);
        WorkingTime workingTime = WorkingTime.rehydrate(
                30L, 100L, 1, hireDate, null, new BigDecimal("75"),
                new WorkingTimeDerivedHours(new BigDecimal("30.00"), new BigDecimal("6.00"), new BigDecimal("125.00")),
                fixedNow, fixedNow
        );
        when(createWorkingTimeUseCase.create(any(CreateWorkingTimeCommand.class))).thenReturn(workingTime);

        participant.participate(ctx);

        assertThat(ctx.workingTimeResult()).isSameAs(workingTime);

        ArgumentCaptor<CreateWorkingTimeCommand> captor = ArgumentCaptor.forClass(CreateWorkingTimeCommand.class);
        verify(createWorkingTimeUseCase).create(captor.capture());
        CreateWorkingTimeCommand cmd = captor.getValue();
        assertThat(cmd.ruleSystemCode()).isEqualTo("ESP");
        assertThat(cmd.employeeTypeCode()).isEqualTo("INTERNAL");
        assertThat(cmd.employeeNumber()).isEqualTo("EMP000001");
        assertThat(cmd.startDate()).isEqualTo(hireDate);
        assertThat(cmd.workingTimePercentage()).isEqualByComparingTo("75");
    }

    @Test
    void wrapsInvalidWorkingTimePercentageExceptionToLifecycleException() {
        HireContext ctx = validContext();
        when(createWorkingTimeUseCase.create(any(CreateWorkingTimeCommand.class)))
                .thenThrow(new InvalidWorkingTimePercentageException("workingTimePercentage must be > 0 and <= 100"));

        assertThatThrownBy(() -> participant.participate(ctx))
                .isInstanceOf(HireEmployeeBusinessValidationException.class);
    }

    /**
     * Esta excepcion no la nombra nadie en el flujo de alta: sale como error de
     * validacion solo porque es del supertipo que el catch declara (backend#59).
     * Hoy el alta no puede provocarla —la jornada empieza el dia de la
     * contratacion y no hay otra en ese inicio—, asi que se fuerza. Si en vez de
     * eso saliera cruda, el alta contestaria un 500.
     */
    @Test
    void wrapsWorkingTimeSeriesInvariantExceptionNobodyNamesToLifecycleException() {
        HireContext ctx = validContext();
        LocalDate hireDate = ctx.hireDate();
        when(createWorkingTimeUseCase.create(any(CreateWorkingTimeCommand.class)))
                .thenThrow(new WorkingTimeIsACorrectionException(
                        "ESP", "INTERNAL", "EMP000001",
                        new WorkingTimeOccurrence(1, hireDate, null),
                        new WorkingTimePeriod(hireDate, null)));

        assertThatThrownBy(() -> participant.participate(ctx))
                .isInstanceOf(HireEmployeeBusinessValidationException.class);
    }

    @Test
    void wrapsWorkingTimeNumberConflictExceptionToLifecycleConflictException() {
        HireContext ctx = validContext();
        when(createWorkingTimeUseCase.create(any(CreateWorkingTimeCommand.class)))
                .thenThrow(new WorkingTimeNumberConflictException(
                        "ESP", "INTERNAL", "EMP000001", 1, new RuntimeException("dup")));

        assertThatThrownBy(() -> participant.participate(ctx))
                .isInstanceOf(HireEmployeeConflictException.class);
    }

    private HireContext validContext() {
        HireContext ctx = new HireContext(
                "ESP", "INTERNAL", "Ana", "Lopez", null, "Ani",
                LocalDate.of(2026, 3, 23),
                "COMP", "HIRE", "WC1",
                new HireEmployeeCommand.HireEmployeeContractCommand("CON", "SUB"),
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("AGR", "CAT"),
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("75"))
        );
        ctx.setEmployeeNumber("EMP000001");
        return ctx;
    }
}

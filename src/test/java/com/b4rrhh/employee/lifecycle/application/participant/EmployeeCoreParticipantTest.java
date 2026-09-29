package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.employee.application.usecase.CreateEmployeeCommand;
import com.b4rrhh.employee.employee.application.usecase.CreateEmployeeUseCase;
import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.HireContext;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeCoreParticipantTest {

    @Mock
    private CreateEmployeeUseCase createEmployeeUseCase;

    @InjectMocks
    private EmployeeCoreParticipant participant;

    @Test
    void orderIs10() {
        assertThat(participant.order()).isEqualTo(10);
    }

    @Test
    void createsEmployeeFromContextAndStoresResult() {
        HireContext ctx = validContext();
        Employee employee = new Employee(
                100L, "ESP", "INTERNAL", "EMP000001", "Ana", "Lopez", null, "Ani",
                LocalDateTime.now(), LocalDateTime.now(), null
        );
        when(createEmployeeUseCase.create(any(CreateEmployeeCommand.class))).thenReturn(employee);

        participant.participate(ctx);

        assertThat(ctx.employee()).isSameAs(employee);

        ArgumentCaptor<CreateEmployeeCommand> captor = ArgumentCaptor.forClass(CreateEmployeeCommand.class);
        verify(createEmployeeUseCase).create(captor.capture());
        CreateEmployeeCommand cmd = captor.getValue();
        assertThat(cmd.ruleSystemCode()).isEqualTo("ESP");
        assertThat(cmd.employeeTypeCode()).isEqualTo("INTERNAL");
        assertThat(cmd.employeeNumber()).isEqualTo("EMP000001");
        assertThat(cmd.firstName()).isEqualTo("Ana");
        assertThat(cmd.lastName1()).isEqualTo("Lopez");
        assertThat(cmd.lastName2()).isNull();
        assertThat(cmd.preferredName()).isEqualTo("Ani");
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

package com.b4rrhh.employee.payroll_input.application.usecase;

import com.b4rrhh.employee.payroll_input.domain.exception.EmployeePayrollInputNotFoundException;
import com.b4rrhh.employee.payroll_input.domain.model.EmployeePayrollInput;
import com.b4rrhh.employee.payroll_input.domain.port.EmployeePayrollInputRepository;
import com.b4rrhh.employee.shared.application.port.DatedWriteNoticePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateEmployeePayrollInputServiceTest {

    @Mock
    private EmployeePayrollInputRepository repository;

    /**
     * El puerto que avisa de que la escritura ha tocado el pasado ({@code backend#130}).
     *
     * <p>Aqui es un doble sin comportamiento a proposito: lo que este test prueba es la regla de
     * la vertical, y que la marca se escriba cuando toca es del escenario del {@code #130}. Lo que
     * si vigila es el candado {@code EveryDatedWriteAnnouncesItselfThroughOnePortTest}, que exige
     * que este puerto este inyectado.
     */
    @Mock
    private DatedWriteNoticePort datedWrites;

    private UpdateEmployeePayrollInputService service;

    @BeforeEach
    void setUp() {
        service = new UpdateEmployeePayrollInputService(repository, datedWrites);
    }

    @Test
    void update_updatesQuantity_whenInputExists() {
        var existing = EmployeePayrollInput.rehydrate("ESP", "GEN", "00001", "HE_QTY", 202604, BigDecimal.valueOf(40));
        when(repository.findByBusinessKey("ESP", "GEN", "00001", "HE_QTY", 202604))
                .thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new UpdateEmployeePayrollInputCommand("ESP", "GEN", "00001", "HE_QTY", 202604, BigDecimal.valueOf(35));
        EmployeePayrollInput result = service.update(command);

        assertThat(result.getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(35));
    }

    @Test
    void update_throwsNotFound_whenInputDoesNotExist() {
        when(repository.findByBusinessKey("ESP", "GEN", "00001", "HE_QTY", 202604))
                .thenReturn(Optional.empty());

        var command = new UpdateEmployeePayrollInputCommand("ESP", "GEN", "00001", "HE_QTY", 202604, BigDecimal.valueOf(35));

        assertThatThrownBy(() -> service.update(command))
                .isInstanceOf(EmployeePayrollInputNotFoundException.class);
    }
}

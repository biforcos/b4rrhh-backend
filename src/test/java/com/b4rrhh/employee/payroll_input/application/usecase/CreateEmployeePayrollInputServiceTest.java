package com.b4rrhh.employee.payroll_input.application.usecase;

import com.b4rrhh.employee.payroll_input.application.service.EmployeePayrollInputGuard;
import com.b4rrhh.employee.payroll_input.domain.exception.EmployeePayrollInputAlreadyExistsException;
import com.b4rrhh.employee.payroll_input.domain.model.EmployeePayrollInput;
import com.b4rrhh.employee.payroll_input.domain.port.EmployeePayrollInputRepository;
import com.b4rrhh.employee.shared.application.port.DatedWriteNoticePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateEmployeePayrollInputServiceTest {

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

    /** El guardián del b4rrhh/backend#142 lo prueba su extremo a extremo; aquí no dice que no. */
    @Mock
    private EmployeePayrollInputGuard guard;

    private CreateEmployeePayrollInputService service;

    @BeforeEach
    void setUp() {
        service = new CreateEmployeePayrollInputService(repository, datedWrites, guard);
    }

    @Test
    void create_savesInput_whenBusinessKeyIsNew() {
        var command = new CreateEmployeePayrollInputCommand("ESP", "GEN", "00001", "HE_QTY", 202604, BigDecimal.valueOf(40));
        when(repository.existsByBusinessKey("ESP", "GEN", "00001", "HE_QTY", 202604)).thenReturn(false);
        var expected = EmployeePayrollInput.create("ESP", "GEN", "00001", "HE_QTY", 202604, BigDecimal.valueOf(40));
        when(repository.save(any())).thenReturn(expected);

        EmployeePayrollInput result = service.create(command);

        assertThat(result.getConceptCode()).isEqualTo("HE_QTY");
        assertThat(result.getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(40));
        verify(repository).save(any());
    }

    @Test
    void create_throwsAlreadyExists_whenDuplicateBusinessKey() {
        var command = new CreateEmployeePayrollInputCommand("ESP", "GEN", "00001", "HE_QTY", 202604, BigDecimal.valueOf(40));
        when(repository.existsByBusinessKey("ESP", "GEN", "00001", "HE_QTY", 202604)).thenReturn(true);

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(EmployeePayrollInputAlreadyExistsException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void create_normalizesRuleSystemCodeToUpperCase() {
        var command = new CreateEmployeePayrollInputCommand("esp", "gen", "00001", "he_qty", 202604, BigDecimal.valueOf(10));
        when(repository.existsByBusinessKey("ESP", "GEN", "00001", "HE_QTY", 202604)).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EmployeePayrollInput result = service.create(command);

        assertThat(result.getRuleSystemCode()).isEqualTo("ESP");
        assertThat(result.getConceptCode()).isEqualTo("HE_QTY");
    }
}

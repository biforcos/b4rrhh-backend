package com.b4rrhh.employee.payroll_input.application.usecase;

import com.b4rrhh.employee.payroll_input.domain.exception.EmployeePayrollInputNotFoundException;
import com.b4rrhh.employee.payroll_input.domain.port.EmployeePayrollInputRepository;
import com.b4rrhh.employee.shared.application.port.DatedWriteNoticePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteEmployeePayrollInputServiceTest {

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

    private DeleteEmployeePayrollInputService service;

    @BeforeEach
    void setUp() {
        service = new DeleteEmployeePayrollInputService(repository, datedWrites);
    }

    @Test
    void delete_callsRepository_whenInputExists() {
        when(repository.existsByBusinessKey("ESP", "GEN", "00001", "HE_QTY", 202604)).thenReturn(true);

        service.delete(new DeleteEmployeePayrollInputCommand("ESP", "GEN", "00001", "HE_QTY", 202604));

        verify(repository).deleteByBusinessKey("ESP", "GEN", "00001", "HE_QTY", 202604);
    }

    @Test
    void delete_throwsNotFound_whenInputDoesNotExist() {
        when(repository.existsByBusinessKey("ESP", "GEN", "00001", "HE_QTY", 202604)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(
                new DeleteEmployeePayrollInputCommand("ESP", "GEN", "00001", "HE_QTY", 202604)))
                .isInstanceOf(EmployeePayrollInputNotFoundException.class);
    }
}

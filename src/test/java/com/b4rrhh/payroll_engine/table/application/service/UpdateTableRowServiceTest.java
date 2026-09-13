package com.b4rrhh.payroll_engine.table.application.service;

import com.b4rrhh.payroll_engine.metamodel.domain.exception.ValidityWindowDoesNotCoverWholePeriodsException;
import com.b4rrhh.payroll_engine.table.application.usecase.UpdateTableRowCommand;
import com.b4rrhh.payroll_engine.table.domain.exception.TableRowNotFoundException;
import com.b4rrhh.payroll_engine.table.domain.model.PayrollTableRow;
import com.b4rrhh.payroll_engine.table.domain.port.PayrollTableRowManagementPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateTableRowServiceTest {

    @Mock
    private PayrollTableRowManagementPort port;

    @InjectMocks
    private UpdateTableRowService service;

    @Test
    void updatesOnlyProvidedFields() {
        PayrollTableRow existing = new PayrollTableRow(
                5L, "ESP", "SB_TEST", "SB-G1",
                LocalDate.of(2024, 1, 1), null,
                new BigDecimal("1800.00"), new BigDecimal("21600.00"),
                new BigDecimal("60.00"), new BigDecimal("7.50"), true
        );
        when(port.findById(5L)).thenReturn(Optional.of(existing));
        when(port.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateTableRowCommand cmd = new UpdateTableRowCommand(5L, null, null, null,
                new BigDecimal("2000.00"), null, null, null, null);
        service.update(cmd);

        ArgumentCaptor<PayrollTableRow> captor = ArgumentCaptor.forClass(PayrollTableRow.class);
        verify(port).save(captor.capture());
        assertThat(captor.getValue().getMonthlyValue()).isEqualByComparingTo("2000.00");
        assertThat(captor.getValue().getSearchCode()).isEqualTo("SB-G1");
    }

    @Test
    void throwsNotFoundWhenRowDoesNotExist() {
        when(port.findById(99L)).thenReturn(Optional.empty());
        UpdateTableRowCommand cmd = new UpdateTableRowCommand(99L, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.update(cmd))
                .isInstanceOf(TableRowNotFoundException.class);
    }

    // backend#89, y aqui la trampa de la actualizacion parcial: el PUT solo trae el endDate.
    // Lo que hay que comprobar es la ventana que queda, no la que viene.
    @Test
    void rejectsAnEndDateMidMonthEvenWhenTheStartComesFromTheExistingRow() {
        PayrollTableRow existing = new PayrollTableRow(
                5L, "ESP", "SB_TEST", "SB-G1",
                LocalDate.of(2024, 1, 1), null,
                new BigDecimal("1800.00"), new BigDecimal("21600.00"),
                new BigDecimal("60.00"), new BigDecimal("7.50"), true
        );
        when(port.findById(5L)).thenReturn(Optional.of(existing));

        UpdateTableRowCommand cmd = new UpdateTableRowCommand(
                5L, null, null, LocalDate.of(2026, 9, 12), null, null, null, null, null);

        assertThatThrownBy(() -> service.update(cmd))
                .isInstanceOf(ValidityWindowDoesNotCoverWholePeriodsException.class)
                .hasMessageContaining("endDate")
                .hasMessageContaining("2026-09-30");

        verify(port, never()).save(any());
    }
}

package com.b4rrhh.payroll_engine.table.application.service;

import com.b4rrhh.payroll_engine.metamodel.domain.exception.ValidityWindowDoesNotCoverWholePeriodsException;
import com.b4rrhh.payroll_engine.table.application.usecase.CreateTableRowCommand;
import com.b4rrhh.payroll_engine.table.domain.exception.TableRowAlreadyExistsException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTableRowServiceTest {

    @Mock
    private PayrollTableRowManagementPort port;

    @InjectMocks
    private CreateTableRowService service;

    @Test
    void createsRowWhenBusinessKeyIsNew() {
        CreateTableRowCommand cmd = new CreateTableRowCommand(
                "ESP", "SB_TEST", "SB-G1",
                LocalDate.of(2024, 1, 1), null,
                new BigDecimal("1800.00"), new BigDecimal("21600.00"),
                new BigDecimal("60.00"), new BigDecimal("7.50")
        );
        when(port.existsByBusinessKey("ESP", "SB_TEST", "SB-G1", LocalDate.of(2024, 1, 1)))
                .thenReturn(false);
        when(port.save(any())).thenAnswer(inv -> {
            PayrollTableRow r = inv.getArgument(0);
            return new PayrollTableRow(1L, r.getRuleSystemCode(), r.getTableCode(), r.getSearchCode(),
                    r.getStartDate(), r.getEndDate(), r.getMonthlyValue(), r.getAnnualValue(),
                    r.getDailyValue(), r.getHourlyValue(), r.isActive());
        });

        PayrollTableRow result = service.create(cmd);

        ArgumentCaptor<PayrollTableRow> captor = ArgumentCaptor.forClass(PayrollTableRow.class);
        verify(port).save(captor.capture());
        assertThat(captor.getValue().getSearchCode()).isEqualTo("SB-G1");
        assertThat(captor.getValue().isActive()).isTrue();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void rejectsDuplicateBusinessKey() {
        CreateTableRowCommand cmd = new CreateTableRowCommand(
                "ESP", "SB_TEST", "SB-G1",
                LocalDate.of(2024, 1, 1), null,
                new BigDecimal("1800.00"), new BigDecimal("21600.00"),
                new BigDecimal("60.00"), new BigDecimal("7.50")
        );
        when(port.existsByBusinessKey("ESP", "SB_TEST", "SB-G1", LocalDate.of(2024, 1, 1)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(cmd))
                .isInstanceOf(TableRowAlreadyExistsException.class);

        verify(port, never()).save(any());
    }

    // backend#89: la fila de tabla es el tercer camino que escribe una vigencia de la
    // reglamentacion, y se lee con la misma fecha unica de fin de periodo que el resto
    // (PayrollConceptExecutionContext.referenceDate <- command.periodEnd()).
    @Test
    void rejectsAWindowThatDoesNotCoverWholePeriodsBeforeAskingTheDatabase() {
        CreateTableRowCommand cmd = new CreateTableRowCommand(
                "ESP", "SB_TEST", "SB-G1",
                LocalDate.of(2024, 1, 1), LocalDate.of(2026, 9, 12),
                new BigDecimal("1800.00"), new BigDecimal("21600.00"),
                new BigDecimal("60.00"), new BigDecimal("7.50")
        );

        assertThatThrownBy(() -> service.create(cmd))
                .isInstanceOf(ValidityWindowDoesNotCoverWholePeriodsException.class)
                .hasMessageContaining("endDate")
                .hasMessageContaining("2026-09-12")
                .hasMessageContaining("2026-09-30");

        verify(port, never()).save(any());
    }
}

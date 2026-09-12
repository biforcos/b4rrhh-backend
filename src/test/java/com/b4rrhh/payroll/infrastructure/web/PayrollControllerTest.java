package com.b4rrhh.payroll.infrastructure.web;

import com.b4rrhh.payroll.application.usecase.BulkInvalidatePayrollResult;
import com.b4rrhh.payroll.application.usecase.BulkInvalidatePayrollUseCase;
import com.b4rrhh.payroll.application.usecase.CalculatePayrollCommand;
import com.b4rrhh.payroll.application.usecase.CalculatePayrollUseCase;
import com.b4rrhh.payroll.application.usecase.FinalizePayrollCommand;
import com.b4rrhh.payroll.application.usecase.FinalizePayrollUseCase;
import com.b4rrhh.payroll.application.usecase.GetPayrollByBusinessKeyUseCase;
import com.b4rrhh.payroll.application.usecase.InvalidatePayrollCommand;
import com.b4rrhh.payroll.application.usecase.InvalidatePayrollUseCase;
import com.b4rrhh.payroll.application.usecase.ValidatePayrollCommand;
import com.b4rrhh.payroll.application.usecase.ValidatePayrollUseCase;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollConcept;
import com.b4rrhh.payroll.domain.model.PayrollContextSnapshot;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.model.PayrollWarning;
import com.b4rrhh.payroll.infrastructure.web.assembler.PayrollResponseAssembler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.b4rrhh.payroll.infrastructure.web.dto.BulkInvalidatePayrollRequest;
import com.b4rrhh.payroll.infrastructure.web.dto.BulkInvalidatePayrollResponse;
import com.b4rrhh.payroll.infrastructure.web.dto.CalculatePayrollRequest;
import com.b4rrhh.payroll.infrastructure.web.dto.InvalidatePayrollRequest;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollConceptRequest;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollContextSnapshotRequest;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.b4rrhh.payroll.application.usecase.RecalculatePayrollCommand;
import com.b4rrhh.payroll.application.usecase.RecalculatePayrollUseCase;
import com.b4rrhh.payroll.application.usecase.SearchPayrollsQuery;
import com.b4rrhh.payroll.application.usecase.SearchPayrollsUseCase;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollSummaryResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollControllerTest {

    @Mock
    private CalculatePayrollUseCase calculatePayrollUseCase;
    @Mock
    private GetPayrollByBusinessKeyUseCase getPayrollByBusinessKeyUseCase;
    @Mock
    private InvalidatePayrollUseCase invalidatePayrollUseCase;
    @Mock
    private ValidatePayrollUseCase validatePayrollUseCase;
    @Mock
    private FinalizePayrollUseCase finalizePayrollUseCase;
    @Mock
    private BulkInvalidatePayrollUseCase bulkInvalidatePayrollUseCase;
    @Mock
    private SearchPayrollsUseCase searchPayrollsUseCase;
    @Mock
    private RecalculatePayrollUseCase recalculatePayrollUseCase;

    private PayrollController controller;

    @BeforeEach
    void setUp() {
        controller = new PayrollController(
                calculatePayrollUseCase,
                getPayrollByBusinessKeyUseCase,
                invalidatePayrollUseCase,
                validatePayrollUseCase,
                finalizePayrollUseCase,
                bulkInvalidatePayrollUseCase,
                searchPayrollsUseCase,
                recalculatePayrollUseCase,
                new PayrollResponseAssembler(new ObjectMapper())
        );
    }

    @Test
    void calculatesPayrollFromRequestBody() {
        when(calculatePayrollUseCase.calculate(any(CalculatePayrollCommand.class))).thenReturn(payroll(PayrollStatus.CALCULATED, null));

        ResponseEntity<PayrollResponse> response = controller.calculateTemporaryStub(new CalculatePayrollRequest(
                "ESP",
                "INTERNAL",
                "EMP001",
                "202501",
                "NORMAL",
                1,
                PayrollStatus.CALCULATED,
                null,
                LocalDateTime.of(2026, 1, 31, 10, 15),
                "ENGINE",
                "1.0",
                List.of(new PayrollConceptRequest(1, "BASE", "Base salary", new BigDecimal("1000.00"), null, null, "EARNING", "202501", 1)),
                List.of(new PayrollContextSnapshotRequest("PRESENCE", "EMPLOYEE", "{\"presenceNumber\":1}", "{\"companyCode\":\"ES01\"}"))
        ));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ESP", response.getBody().ruleSystemCode());

        ArgumentCaptor<CalculatePayrollCommand> captor = ArgumentCaptor.forClass(CalculatePayrollCommand.class);
        verify(calculatePayrollUseCase).calculate(captor.capture());
        assertEquals("NORMAL", captor.getValue().payrollTypeCode());
    }

    @Test
    void getsPayrollByBusinessKey() {
        when(getPayrollByBusinessKeyUseCase.getByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
                .thenReturn(Optional.of(payroll(PayrollStatus.CALCULATED, null)));

        ResponseEntity<PayrollResponse> response = controller.getByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("202501", response.getBody().payrollPeriodCode());
        assertEquals(41L, response.getBody().runId());
        assertEquals(0, response.getBody().warnings().size());
        }

        @Test
        void getsPayrollByBusinessKeyIncludingWarningsWhenPresent() {
        when(getPayrollByBusinessKeyUseCase.getByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1))
            .thenReturn(Optional.of(payroll(
                PayrollStatus.NOT_VALID,
                "ENGINE_INVALID",
                List.of(new PayrollWarning(
                    9L,
                    7L,
                    "MISSING_RULE_INPUT",
                    "WARNING",
                    "Missing optional input value",
                    "{\"field\":\"baseSalary\"}"
                ))
            )));

        ResponseEntity<PayrollResponse> response = controller.getByBusinessKey("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().warnings().size());
        assertEquals("MISSING_RULE_INPUT", response.getBody().warnings().getFirst().warningCode());
        assertEquals("WARNING", response.getBody().warnings().getFirst().severityCode());
    }

    @Test
    void invalidatesPayrollByBusinessKey() {
        when(invalidatePayrollUseCase.invalidate(any(InvalidatePayrollCommand.class)))
                .thenReturn(payroll(PayrollStatus.NOT_VALID, "USER_INVALIDATED"));

        ResponseEntity<PayrollResponse> response = controller.invalidate(
                "ESP",
                "INTERNAL",
                "EMP001",
                "202501",
                "NORMAL",
                1,
                new InvalidatePayrollRequest("USER_INVALIDATED")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(PayrollStatus.NOT_VALID, response.getBody().status());
        verify(invalidatePayrollUseCase).invalidate(any(InvalidatePayrollCommand.class));
    }

    @Test
    void validatesPayrollByBusinessKey() {
        when(validatePayrollUseCase.validate(any(ValidatePayrollCommand.class)))
                .thenReturn(payroll(PayrollStatus.EXPLICIT_VALIDATED, null));

        ResponseEntity<PayrollResponse> response = controller.validate("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(PayrollStatus.EXPLICIT_VALIDATED, response.getBody().status());
        verify(validatePayrollUseCase).validate(any(ValidatePayrollCommand.class));
    }

    @Test
    void finalizesPayrollByBusinessKey() {
        when(finalizePayrollUseCase.finalizePayroll(any(FinalizePayrollCommand.class)))
                .thenReturn(payroll(PayrollStatus.DEFINITIVE, null));

        ResponseEntity<PayrollResponse> response = controller.finalizePayroll("ESP", "INTERNAL", "EMP001", "202501", "NORMAL", 1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(PayrollStatus.DEFINITIVE, response.getBody().status());
        verify(finalizePayrollUseCase).finalizePayroll(any(FinalizePayrollCommand.class));
    }

    @Test
    void bulkInvalidatesPayrolls() {
        BulkInvalidatePayrollResult result = new BulkInvalidatePayrollResult(
                "ESP", "202501", "NORMAL", 3, 3, 2, 1, 0, 0, "BULK_RESET"
        );
        when(bulkInvalidatePayrollUseCase.invalidateBulk(any())).thenReturn(result);

        ResponseEntity<BulkInvalidatePayrollResponse> response = controller.invalidateBulk(
                new BulkInvalidatePayrollRequest(
                        "ESP",
                        "202501",
                        "NORMAL",
                        "BULK_RESET",
                        new com.b4rrhh.payroll.infrastructure.web.dto.PayrollLaunchTargetSelectionRequest(
                                com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                                new com.b4rrhh.payroll.infrastructure.web.dto.PayrollLaunchEmployeeTargetRequest("INTERNAL", "EMP001"),
                                null
                        )
                )
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ESP", response.getBody().ruleSystemCode());
        assertEquals(3, response.getBody().totalCandidates());
        assertEquals(2, response.getBody().totalInvalidated());
        assertEquals(1, response.getBody().totalSkippedAlreadyNotValid());
        assertEquals(0, response.getBody().totalSkippedProtected());
        assertEquals("BULK_RESET", response.getBody().statusReasonCode());
        verify(bulkInvalidatePayrollUseCase).invalidateBulk(any());
    }


    @Test
    void searchesPayrollsByFilters() {
        Payroll payroll = payroll(PayrollStatus.CALCULATED, null);
        when(searchPayrollsUseCase.search(any(SearchPayrollsQuery.class))).thenReturn(List.of(payroll));

        ResponseEntity<List<PayrollSummaryResponse>> response = controller.search(null, "202604", "MAS000001", "CALCULATED");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        ArgumentCaptor<SearchPayrollsQuery> queryCaptor = ArgumentCaptor.forClass(SearchPayrollsQuery.class);
        verify(searchPayrollsUseCase).search(queryCaptor.capture());
        assertEquals("202604", queryCaptor.getValue().payrollPeriodCode());
        assertEquals(PayrollStatus.CALCULATED, queryCaptor.getValue().status());

        PayrollSummaryResponse first = response.getBody().getFirst();
        assertEquals("CALCULATED", first.status());
        assertEquals("EMP001", first.employeeNumber());
    }

    @Test
    void recalculatesPayroll() {
        Payroll recalculated = payroll(PayrollStatus.CALCULATED, null);
        when(recalculatePayrollUseCase.recalculate(any(RecalculatePayrollCommand.class))).thenReturn(recalculated);

        ResponseEntity<PayrollResponse> response = controller.recalculate(
                "MAS", "EMP", "MAS000001", "202604", "NORMAL", 1
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("CALCULATED", response.getBody().status().name());
    }

    private Payroll payroll(PayrollStatus status, String statusReasonCode) {
        return payroll(status, statusReasonCode, List.of());
    }

    private Payroll payroll(PayrollStatus status, String statusReasonCode, List<PayrollWarning> warnings) {
        return Payroll.rehydrate(
                7L,
                "ESP",
                "INTERNAL",
                "EMP001",
                "202501",
                "NORMAL",
                1,
                status,
                statusReasonCode,
                LocalDateTime.of(2026, 1, 31, 10, 15),
                "ENGINE",
                "1.0",
                41L,
                warnings,
                List.of(new PayrollConcept(1, "BASE", "Base salary", new BigDecimal("1000.00"), null, null, "EARNING", "202501", 1)),
                List.of(new PayrollContextSnapshot("PRESENCE", "EMPLOYEE", "{\"presenceNumber\":1}", "{\"companyCode\":\"ES01\"}")),
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}

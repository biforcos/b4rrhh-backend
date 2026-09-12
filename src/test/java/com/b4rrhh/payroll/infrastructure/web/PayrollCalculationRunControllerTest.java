package com.b4rrhh.payroll.infrastructure.web;

import com.b4rrhh.payroll.application.usecase.GetPayrollCalculationRunUseCase;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationCommand;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationUseCase;
import com.b4rrhh.payroll.application.usecase.ListPayrollCalculationRunMessagesUseCase;
import com.b4rrhh.payroll.domain.model.CalculationRun;
import com.b4rrhh.payroll.domain.model.CalculationRunMessage;
import com.b4rrhh.payroll.infrastructure.web.assembler.PayrollCalculationRunMessageResponseAssembler;
import com.b4rrhh.payroll.infrastructure.web.assembler.PayrollCalculationRunResponseAssembler;
import com.b4rrhh.payroll.infrastructure.web.dto.LaunchPayrollCalculationRequest;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollCalculationRunMessagesResponse;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollCalculationRunResponse;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollLaunchEmployeeTargetRequest;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollLaunchTargetSelectionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollCalculationRunControllerTest {

    @Mock
    private LaunchPayrollCalculationUseCase launchPayrollCalculationUseCase;
    @Mock
    private GetPayrollCalculationRunUseCase getPayrollCalculationRunUseCase;
    @Mock
    private ListPayrollCalculationRunMessagesUseCase listPayrollCalculationRunMessagesUseCase;

    private PayrollCalculationRunController controller;

    @BeforeEach
    void setUp() {
        controller = new PayrollCalculationRunController(
                launchPayrollCalculationUseCase,
                getPayrollCalculationRunUseCase,
            listPayrollCalculationRunMessagesUseCase,
            new PayrollCalculationRunResponseAssembler(),
            new PayrollCalculationRunMessageResponseAssembler()
        );
    }

    @Test
    void launchesPayrollCalculationRun() {
        when(launchPayrollCalculationUseCase.requestLaunch(any(LaunchPayrollCalculationCommand.class))).thenReturn(run("REQUESTED"));

        ResponseEntity<PayrollCalculationRunResponse> response = controller.launch(new LaunchPayrollCalculationRequest(
                "ESP",
                "202501",
                "NORMAL",
                "ENGINE",
                "1.0",
                new PayrollLaunchTargetSelectionRequest(
                        com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTargetRequest("INTERNAL", "EMP001"),
                        null
                )
        ), new TestingAuthenticationToken("hr.manager@b4rrhh", "n/a"));

        // 202: la ejecucion esta aceptada y en marcha, no terminada (#75).
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().runId());
        assertEquals("REQUESTED", response.getBody().status());

        ArgumentCaptor<LaunchPayrollCalculationCommand> captor = ArgumentCaptor.forClass(LaunchPayrollCalculationCommand.class);
        verify(launchPayrollCalculationUseCase).requestLaunch(captor.capture());
        assertEquals("NORMAL", captor.getValue().payrollTypeCode());
        assertEquals("INTERNAL", captor.getValue().targetSelection().employee().employeeTypeCode());
        assertEquals("hr.manager@b4rrhh", captor.getValue().requestedBy());
    }

        @Test
        void launchesPayrollCalculationRunForAllEmployeesWithPresenceInPeriod() {
        when(launchPayrollCalculationUseCase.requestLaunch(any(LaunchPayrollCalculationCommand.class))).thenReturn(run("REQUESTED"));

        ResponseEntity<PayrollCalculationRunResponse> response = controller.launch(new LaunchPayrollCalculationRequest(
            "ESP",
            "202501",
            "NORMAL",
            "ENGINE",
            "1.0",
            new PayrollLaunchTargetSelectionRequest(
                com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelectionType.ALL_EMPLOYEES_WITH_PRESENCE_IN_PERIOD,
                null,
                null
            )
        ), null);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());

        ArgumentCaptor<LaunchPayrollCalculationCommand> captor = ArgumentCaptor.forClass(LaunchPayrollCalculationCommand.class);
        verify(launchPayrollCalculationUseCase).requestLaunch(captor.capture());
        assertEquals(
            com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelectionType.ALL_EMPLOYEES_WITH_PRESENCE_IN_PERIOD,
            captor.getValue().targetSelection().selectionType()
        );
        assertNull(captor.getValue().targetSelection().employee());
        assertNull(captor.getValue().targetSelection().employees());
        assertNull(captor.getValue().requestedBy());
        }

    @Test
    void getsPayrollCalculationRunById() {
        when(getPayrollCalculationRunUseCase.getById(1L)).thenReturn(Optional.of(run("RUNNING")));

        ResponseEntity<PayrollCalculationRunResponse> response = controller.getById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("RUNNING", response.getBody().status());
        assertEquals(1, response.getBody().totalCalculated());
        assertEquals("hr.manager@b4rrhh", response.getBody().requestedBy());
    }

    @Test
    void returnsNotFoundWhenRunDoesNotExist() {
        when(getPayrollCalculationRunUseCase.getById(999L)).thenReturn(Optional.empty());

        ResponseEntity<PayrollCalculationRunResponse> response = controller.getById(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void listsRunMessagesWhenRunExists() {
        when(getPayrollCalculationRunUseCase.getById(1L)).thenReturn(Optional.of(run("COMPLETED")));
        when(listPayrollCalculationRunMessagesUseCase.listByRunId(1L)).thenReturn(List.of(
                new CalculationRunMessage(
                        10L,
                        1L,
                        "PAYROLL_LAUNCH_INFO",
                        "INFO",
                        "Launch started",
                        "{}",
                        "ESP",
                        "INTERNAL",
                        "EMP001",
                        "202501",
                        "NORMAL",
                        1,
                        LocalDateTime.of(2026, 4, 11, 10, 1)
                )
        ));

        ResponseEntity<PayrollCalculationRunMessagesResponse> response = controller.listRunMessages(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().runId());
        assertEquals(1, response.getBody().items().size());
        assertEquals("PAYROLL_LAUNCH_INFO", response.getBody().items().getFirst().messageCode());
    }

    @Test
    void returnsOkWithEmptyItemsWhenRunExistsAndHasNoMessages() {
        when(getPayrollCalculationRunUseCase.getById(1L)).thenReturn(Optional.of(run("COMPLETED")));
        when(listPayrollCalculationRunMessagesUseCase.listByRunId(1L)).thenReturn(List.of());

        ResponseEntity<PayrollCalculationRunMessagesResponse> response = controller.listRunMessages(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().runId());
        assertEquals(0, response.getBody().items().size());
    }

    @Test
    void returnsNotFoundWhenListingMessagesForMissingRun() {
        when(getPayrollCalculationRunUseCase.getById(999L)).thenReturn(Optional.empty());

        ResponseEntity<PayrollCalculationRunMessagesResponse> response = controller.listRunMessages(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    private CalculationRun run(String status) {
        return new CalculationRun(
                1L,
                "ESP",
                "202501",
                "NORMAL",
                "ENGINE",
                "1.0",
                LocalDateTime.of(2026, 4, 11, 10, 0),
                "hr.manager@b4rrhh",
                status,
                "{\"selectionType\":\"SINGLE_EMPLOYEE\"}",
                1,
                1,
                1,
                0,
                0,
                1,
                0,
                0,
                LocalDateTime.of(2026, 4, 11, 10, 1),
                LocalDateTime.of(2026, 4, 11, 10, 2),
                "{}",
                LocalDateTime.of(2026, 4, 11, 10, 0),
                LocalDateTime.of(2026, 4, 11, 10, 2)
        );
    }
}
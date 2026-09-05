package com.b4rrhh.employee.contract.application.usecase;

import com.b4rrhh.employee.contract.application.command.CloseContractCommand;
import com.b4rrhh.employee.contract.application.port.ContractPresenceConsistencyPort;
import com.b4rrhh.employee.contract.application.port.EmployeeContractContext;
import com.b4rrhh.employee.contract.application.port.EmployeeContractLookupPort;
import com.b4rrhh.employee.contract.application.port.PresencePeriod;
import com.b4rrhh.employee.contract.application.service.ContractTimelineService;
import com.b4rrhh.employee.contract.domain.exception.ContractAlreadyClosedException;
import com.b4rrhh.employee.contract.domain.exception.ContractCoverageIncompleteException;
import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.contract.domain.model.ContractPeriod;
import com.b4rrhh.employee.contract.domain.port.ContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The deprecated {@code close} as an adapter over the temporal component
 * (ADR-057): closing is a correction of the end date and the resulting
 * series decides. The timeline service is real; the repository and the
 * presence port are mocked.
 */
@ExtendWith(MockitoExtension.class)
class CloseContractServiceTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String EMPLOYEE_NUMBER = "EMP001";

    @Mock
    private ContractRepository contractRepository;
    @Mock
    private EmployeeContractLookupPort employeeContractLookupPort;
    @Mock
    private ContractPresenceConsistencyPort presencePort;

    private CloseContractService service;

    @BeforeEach
    void setUp() {
        service = new CloseContractService(
                contractRepository,
                employeeContractLookupPort,
                new ContractTimelineService(contractRepository, presencePort)
        );
    }

    // The termination flow closes the presence first (order 5), so closing the
    // contract on the same day leaves nothing uncovered.
    @Test
    void closesWhenValid() {
        CloseContractCommand command = new CloseContractCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        );

        Contract existing = new Contract(
                10L,
                "IND",
                "FT1",
                LocalDate.of(2026, 1, 1),
                null
        );

        givenEmployeePresent(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), existing);
        when(contractRepository.findByEmployeeIdAndStartDate(10L, LocalDate.of(2026, 1, 1)))
                .thenReturn(Optional.of(existing));

        Contract closed = service.close(command);

        assertEquals(LocalDate.of(2026, 1, 31), closed.getEndDate());

        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractRepository).update(captor.capture(), any(LocalDate.class));
        assertEquals(LocalDate.of(2026, 1, 31), captor.getValue().getEndDate());
    }

    @Test
    void rejectsCloseWhenAlreadyClosed() {
        CloseContractCommand command = new CloseContractCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 1)
        );

        Contract closed = new Contract(
                10L,
                "IND",
                "FT1",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        );

        whenEmployeeExists();
        when(contractRepository.findByEmployeeIdAndStartDate(10L, LocalDate.of(2026, 1, 1)))
                .thenReturn(Optional.of(closed));

        assertThrows(ContractAlreadyClosedException.class, () -> service.close(command));
        verify(contractRepository, never()).update(any(Contract.class), any(LocalDate.class));
    }

    @Test
    void rejectsCloseWhenCoverageWouldHaveGap() {
        CloseContractCommand command = new CloseContractCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 15)
        );

        Contract existing = new Contract(
                10L,
                "IND",
                "FT1",
                LocalDate.of(2026, 1, 1),
                null
        );

        givenEmployeePresent(LocalDate.of(2026, 1, 1), null, existing);
        when(contractRepository.findByEmployeeIdAndStartDate(10L, LocalDate.of(2026, 1, 1)))
                .thenReturn(Optional.of(existing));

        ContractCoverageIncompleteException ex = assertThrows(
                ContractCoverageIncompleteException.class,
                () -> service.close(command)
        );

        assertEquals(List.of(new ContractPeriod(LocalDate.of(2026, 1, 16), null)), ex.gaps());
        verify(contractRepository, never()).update(any(Contract.class), any(LocalDate.class));
    }

    private void givenEmployeePresent(LocalDate presenceStart, LocalDate presenceEnd, Contract... occurrences) {
        whenEmployeeExists();
        when(contractRepository.findByEmployeeIdOrderByStartDate(10L)).thenReturn(List.of(occurrences));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L))
                .thenReturn(List.of(new PresencePeriod(presenceStart, presenceEnd)));
    }

    private void whenEmployeeExists() {
        when(employeeContractLookupPort.findByBusinessKeyForUpdate(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER
        )).thenReturn(Optional.of(new EmployeeContractContext(
                10L,
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER
        )));
    }
}

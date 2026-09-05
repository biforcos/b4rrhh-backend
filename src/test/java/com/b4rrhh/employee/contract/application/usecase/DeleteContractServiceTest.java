package com.b4rrhh.employee.contract.application.usecase;

import com.b4rrhh.employee.contract.application.command.DeleteContractCommand;
import com.b4rrhh.employee.contract.application.port.ContractPresenceConsistencyPort;
import com.b4rrhh.employee.contract.application.port.EmployeeContractContext;
import com.b4rrhh.employee.contract.application.port.EmployeeContractLookupPort;
import com.b4rrhh.employee.contract.application.port.PresencePeriod;
import com.b4rrhh.employee.contract.application.service.ContractTimelineService;
import com.b4rrhh.employee.contract.domain.exception.ContractCoverageIncompleteException;
import com.b4rrhh.employee.contract.domain.exception.ContractNotFoundException;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Removing a contract (ADR-057, decision 3). The timeline service is real
 * and the repository and presence port are mocked: the employee is present
 * from 2026-01-01 onwards.
 */
@ExtendWith(MockitoExtension.class)
class DeleteContractServiceTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String EMPLOYEE_NUMBER = "EMP001";
    private static final LocalDate PRESENCE_START = LocalDate.of(2026, 1, 1);

    @Mock
    private ContractRepository contractRepository;
    @Mock
    private EmployeeContractLookupPort employeeContractLookupPort;
    @Mock
    private ContractPresenceConsistencyPort presencePort;

    private DeleteContractService service;

    @BeforeEach
    void setUp() {
        service = new DeleteContractService(
                contractRepository,
                employeeContractLookupPort,
                new ContractTimelineService(contractRepository, presencePort)
        );
    }

    @Test
    void deletingTheLastOneReopensThePreviousOne() {
        Contract first = contract(PRESENCE_START, LocalDate.of(2026, 1, 15));
        Contract last = contract(LocalDate.of(2026, 1, 16), null);
        givenEmployeeWithSeries(first, last);
        when(contractRepository.findByEmployeeIdAndStartDate(10L, last.getStartDate())).thenReturn(Optional.of(last));
        when(contractRepository.findByEmployeeIdAndStartDate(10L, first.getStartDate())).thenReturn(Optional.of(first));

        service.delete(command(LocalDate.of(2026, 1, 16)));

        ArgumentCaptor<Contract> reopened = ArgumentCaptor.forClass(Contract.class);
        verify(contractRepository).update(reopened.capture(), eq(PRESENCE_START));
        assertEquals(PRESENCE_START, reopened.getValue().getStartDate());
        assertNull(reopened.getValue().getEndDate());
        verify(contractRepository).delete(last);
    }

    @Test
    void deletingTheOnlyOneIsRejectedBecauseThePresenceWouldBeUncovered() {
        Contract only = contract(PRESENCE_START, null);
        givenEmployeeWithSeries(only);
        when(contractRepository.findByEmployeeIdAndStartDate(10L, PRESENCE_START)).thenReturn(Optional.of(only));

        ContractCoverageIncompleteException ex = assertThrows(
                ContractCoverageIncompleteException.class,
                () -> service.delete(command(PRESENCE_START))
        );

        assertEquals(List.of(new ContractPeriod(PRESENCE_START, null)), ex.gaps());
        verify(contractRepository, never()).delete(any());
        verify(contractRepository, never()).update(any(), any());
    }

    @Test
    void deletingOneInTheMiddleIsRejectedNamingTheNeighboursToStretch() {
        Contract first = contract(PRESENCE_START, LocalDate.of(2026, 1, 15));
        Contract middle = contract(LocalDate.of(2026, 1, 16), LocalDate.of(2026, 1, 31));
        Contract last = contract(LocalDate.of(2026, 2, 1), null);
        givenEmployeeWithSeries(first, middle, last);
        when(contractRepository.findByEmployeeIdAndStartDate(10L, middle.getStartDate())).thenReturn(Optional.of(middle));

        ContractCoverageIncompleteException ex = assertThrows(
                ContractCoverageIncompleteException.class,
                () -> service.delete(command(LocalDate.of(2026, 1, 16)))
        );

        assertEquals(
                List.of(new ContractPeriod(LocalDate.of(2026, 1, 16), LocalDate.of(2026, 1, 31))),
                ex.gaps()
        );
        assertEquals(
                List.of(
                        new ContractPeriod(PRESENCE_START, LocalDate.of(2026, 1, 15)),
                        new ContractPeriod(LocalDate.of(2026, 2, 1), null)
                ),
                ex.stretchCandidates()
        );
        verify(contractRepository, never()).delete(any());
        verify(contractRepository, never()).update(any(), any());
    }

    @Test
    void rejectsWhenTheContractDoesNotExist() {
        whenEmployeeExists();
        when(contractRepository.findByEmployeeIdAndStartDate(10L, LocalDate.of(2026, 7, 1))).thenReturn(Optional.empty());

        assertThrows(ContractNotFoundException.class, () -> service.delete(command(LocalDate.of(2026, 7, 1))));
        verify(contractRepository, never()).delete(any());
    }

    private DeleteContractCommand command(LocalDate startDate) {
        return new DeleteContractCommand(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER, startDate);
    }

    private void givenEmployeeWithSeries(Contract... occurrences) {
        whenEmployeeExists();
        when(contractRepository.findByEmployeeIdOrderByStartDate(10L)).thenReturn(List.of(occurrences));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L))
                .thenReturn(List.of(new PresencePeriod(PRESENCE_START, null)));
    }

    private void whenEmployeeExists() {
        when(employeeContractLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(new EmployeeContractContext(10L, RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER)));
    }

    private static Contract contract(LocalDate startDate, LocalDate endDate) {
        return new Contract(10L, "IND", "FT1", startDate, endDate);
    }
}

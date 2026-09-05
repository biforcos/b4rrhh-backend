package com.b4rrhh.employee.contract.application.usecase;

import com.b4rrhh.employee.contract.application.command.UpdateContractCommand;
import com.b4rrhh.employee.contract.application.port.ContractPresenceConsistencyPort;
import com.b4rrhh.employee.contract.application.port.EmployeeContractContext;
import com.b4rrhh.employee.contract.application.port.EmployeeContractLookupPort;
import com.b4rrhh.employee.contract.application.port.PresencePeriod;
import com.b4rrhh.employee.contract.application.service.ContractSubtypeRelationValidator;
import com.b4rrhh.employee.contract.application.service.ContractCatalogValidator;
import com.b4rrhh.employee.contract.application.service.ContractTimelineService;
import com.b4rrhh.employee.contract.domain.exception.ContractCoverageIncompleteException;
import com.b4rrhh.employee.contract.domain.exception.ContractNotFoundException;
import com.b4rrhh.employee.contract.domain.exception.ContractOutsidePresencePeriodException;
import com.b4rrhh.employee.contract.domain.exception.ContractOverlapException;
import com.b4rrhh.employee.contract.domain.exception.ContractSubtypeRelationInvalidException;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Correcting a contract moves only the corrected one (ADR-057, decision 3).
 * The timeline service is real and the repository and presence port are
 * mocked; each test says from when the employee is present.
 */
@ExtendWith(MockitoExtension.class)
class UpdateContractServiceTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String EMPLOYEE_NUMBER = "EMP001";

    @Mock
    private ContractRepository contractRepository;
    @Mock
    private EmployeeContractLookupPort employeeContractLookupPort;
    @Mock
    private ContractPresenceConsistencyPort presencePort;

    private TestContractCatalogValidator contractCatalogValidator;
    private TestContractSubtypeRelationValidator contractSubtypeRelationValidator;
    private UpdateContractService service;

    @BeforeEach
    void setUp() {
        contractCatalogValidator = new TestContractCatalogValidator();
        contractSubtypeRelationValidator = new TestContractSubtypeRelationValidator();

        service = new UpdateContractService(
                contractRepository,
                employeeContractLookupPort,
                contractCatalogValidator,
                contractSubtypeRelationValidator,
                new ContractTimelineService(contractRepository, presencePort)
        );
    }

    @Test
    void correctsTheCodesKeepingTheDates() {
        Contract existing = contract("IND", "FT1", LocalDate.of(2026, 1, 1), null);
        givenEmployeePresentFrom(LocalDate.of(2026, 1, 1), existing);
        whenContractExists(existing);

        Contract updated = service.update(command(LocalDate.of(2026, 1, 1), null, null, "TMP", "PT1"));

        assertEquals("TMP", updated.getContractCode());
        assertEquals("PT1", updated.getContractSubtypeCode());
        assertEquals(LocalDate.of(2026, 1, 1), updated.getStartDate());
        assertNull(updated.getEndDate());

        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractRepository).update(captor.capture(), eq(LocalDate.of(2026, 1, 1)));
        assertEquals("TMP", captor.getValue().getContractCode());
        assertEquals("PT1", captor.getValue().getContractSubtypeCode());
    }

    // Before ADR-057 a closed contract could not be corrected. What identifies
    // it is the day it starts, and a wrong type on a past contract is still
    // wrong: the resulting series decides, not whether it is open.
    @Test
    void correctsAClosedContractStretchingItOverTheGapItLeft() {
        Contract closedTooEarly = contract("IND", "FT1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        givenEmployeePresentFrom(LocalDate.of(2026, 1, 1), closedTooEarly);
        whenContractExists(closedTooEarly);

        Contract updated = service.update(command(LocalDate.of(2026, 1, 1), null, null, "TMP", "PT1"));

        assertNull(updated.getEndDate());
        assertEquals("TMP", updated.getContractCode());
        verify(contractRepository).update(any(Contract.class), eq(LocalDate.of(2026, 1, 1)));
    }

    @Test
    void rejectsUpdateWhenContractSubtypeRelationIsInvalid() {
        Contract existing = contract("IND", "FT1", LocalDate.of(2026, 1, 1), null);
        contractSubtypeRelationValidator.setInvalidRelation(true);
        whenEmployeeExists();
        whenContractExists(existing);

        assertThrows(
                ContractSubtypeRelationInvalidException.class,
                () -> service.update(command(LocalDate.of(2026, 1, 1), null, null, "TMP", "PT1"))
        );
        verify(contractRepository, never()).update(any(Contract.class), any(LocalDate.class));
    }

    // The old cascade (the predecessor followed the new start on its own) is
    // what ADR-057 decision 3 retires: stretching a neighbour is the user's
    // act. The plan rejects the gap and names the predecessor to stretch.
    @Test
    void movingTheStartLaterDoesNotStretchThePredecessorButNamesIt() {
        Contract predecessor = contract("IND", "FT1", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        Contract current = contract("IND", "FT1", LocalDate.of(2025, 1, 1), null);
        givenEmployeePresentFrom(LocalDate.of(2024, 1, 1), predecessor, current);
        whenContractExists(current);

        ContractCoverageIncompleteException ex = assertThrows(
                ContractCoverageIncompleteException.class,
                () -> service.update(command(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1), null, "IND", "FT1"))
        );

        assertEquals(List.of(new ContractPeriod(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))), ex.gaps());
        assertTrue(ex.stretchCandidates().contains(new ContractPeriod(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))));
        verify(contractRepository, never()).update(any(Contract.class), any(LocalDate.class));
    }

    @Test
    void movingTheStartEarlierOverAnUncoveredStretchUpdatesOnlyTheCorrectedOne() {
        Contract current = contract("IND", "FT1", LocalDate.of(2025, 1, 1), null);
        givenEmployeePresentFrom(LocalDate.of(2024, 12, 1), current);
        whenContractExists(current);

        service.update(command(LocalDate.of(2025, 1, 1), LocalDate.of(2024, 12, 1), null, "IND", "FT1"));

        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractRepository).update(captor.capture(), eq(LocalDate.of(2025, 1, 1)));
        assertEquals(LocalDate.of(2024, 12, 1), captor.getValue().getStartDate());
    }

    @Test
    void movingTheStartEarlierIntoThePredecessorIsRejectedAsAnOverlap() {
        Contract predecessor = contract("CTR", "ORD", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        Contract current = contract("CTR", "ORD", LocalDate.of(2025, 1, 1), null);
        givenEmployeePresentFrom(LocalDate.of(2024, 1, 1), predecessor, current);
        whenContractExists(current);

        ContractOverlapException ex = assertThrows(
                ContractOverlapException.class,
                () -> service.update(command(LocalDate.of(2025, 1, 1), LocalDate.of(2024, 12, 15), null, "CTR", "ORD"))
        );

        assertEquals(List.of(new ContractPeriod(LocalDate.of(2024, 12, 15), LocalDate.of(2024, 12, 31))), ex.overlaps());
        verify(contractRepository, never()).update(any(Contract.class), any(LocalDate.class));
    }

    @Test
    void movingBeforeThePresenceIsRejected() {
        Contract current = contract("IND", "FT1", LocalDate.of(2025, 1, 1), null);
        givenEmployeePresentFrom(LocalDate.of(2025, 1, 1), current);
        whenContractExists(current);

        assertThrows(
                ContractOutsidePresencePeriodException.class,
                () -> service.update(command(LocalDate.of(2025, 1, 1), LocalDate.of(2024, 12, 1), null, "IND", "FT1"))
        );
        verify(contractRepository, never()).update(any(Contract.class), any(LocalDate.class));
    }

    @Test
    void rejectsWhenTheContractDoesNotExist() {
        whenEmployeeExists();
        when(contractRepository.findByEmployeeIdAndStartDate(10L, LocalDate.of(2026, 1, 1))).thenReturn(Optional.empty());

        assertThrows(
                ContractNotFoundException.class,
                () -> service.update(command(LocalDate.of(2026, 1, 1), null, null, "TMP", "PT1"))
        );
    }

    private UpdateContractCommand command(
            LocalDate startDate,
            LocalDate newStartDate,
            LocalDate endDate,
            String contractCode,
            String contractSubtypeCode
    ) {
        return new UpdateContractCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                startDate,
                newStartDate,
                endDate,
                contractCode,
                contractSubtypeCode
        );
    }

    private void givenEmployeePresentFrom(LocalDate presenceStart, Contract... occurrences) {
        whenEmployeeExists();
        when(contractRepository.findByEmployeeIdOrderByStartDate(10L)).thenReturn(List.of(occurrences));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L))
                .thenReturn(List.of(new PresencePeriod(presenceStart, null)));
    }

    private void whenContractExists(Contract existing) {
        when(contractRepository.findByEmployeeIdAndStartDate(10L, existing.getStartDate()))
                .thenReturn(Optional.of(existing));
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

    private static Contract contract(String code, String subtype, LocalDate startDate, LocalDate endDate) {
        return Contract.rehydrate(10L, code, subtype, startDate, endDate);
    }

    private static final class TestContractCatalogValidator extends ContractCatalogValidator {

        private TestContractCatalogValidator() {
            super(null);
        }

        @Override
        public String normalizeRequiredCode(String fieldName, String value) {
            return value == null ? null : value.trim().toUpperCase();
        }

        @Override
        public void validateContractCode(String ruleSystemCode, String contractCode, LocalDate referenceDate) {
            // Always valid in these tests.
        }

        @Override
        public void validateContractSubtypeCode(
                String ruleSystemCode,
                String contractSubtypeCode,
                LocalDate referenceDate
        ) {
            // Always valid in these tests.
        }
    }

    private static final class TestContractSubtypeRelationValidator extends ContractSubtypeRelationValidator {

        private boolean invalidRelation;

        private TestContractSubtypeRelationValidator() {
            super(null);
        }

        void setInvalidRelation(boolean invalidRelation) {
            this.invalidRelation = invalidRelation;
        }

        @Override
        public void validateContractSubtypeRelation(
                String ruleSystemCode,
                String contractCode,
                String contractSubtypeCode,
                LocalDate referenceDate
        ) {
            if (invalidRelation) {
                throw new ContractSubtypeRelationInvalidException(
                        ruleSystemCode,
                        contractCode,
                        contractSubtypeCode,
                        referenceDate
                );
            }
        }
    }
}

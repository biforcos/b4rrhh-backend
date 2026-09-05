package com.b4rrhh.employee.contract.application.usecase;

import com.b4rrhh.employee.contract.application.command.ReplaceContractFromDateCommand;
import com.b4rrhh.employee.contract.application.port.ContractPresenceConsistencyPort;
import com.b4rrhh.employee.contract.application.port.EmployeeContractContext;
import com.b4rrhh.employee.contract.application.port.EmployeeContractLookupPort;
import com.b4rrhh.employee.contract.application.port.PresencePeriod;
import com.b4rrhh.employee.contract.application.service.ContractSubtypeRelationValidator;
import com.b4rrhh.employee.contract.application.service.ContractCatalogValidator;
import com.b4rrhh.employee.contract.application.service.ContractTimelineService;
import com.b4rrhh.employee.contract.domain.exception.ContractSubtypeRelationInvalidException;
import com.b4rrhh.employee.contract.domain.exception.ContractCoverageIncompleteException;
import com.b4rrhh.employee.contract.domain.exception.ContractEmployeeNotFoundException;
import com.b4rrhh.employee.contract.domain.exception.ContractIsACorrectionException;
import com.b4rrhh.employee.contract.domain.exception.ContractOutsidePresencePeriodException;
import com.b4rrhh.employee.contract.domain.exception.ContractOverlapException;
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
 * The deprecated {@code replace-from-date} as an adapter over the temporal
 * component (ADR-057). The behaviour the old {@code ReplaceMode} gave the
 * screen is kept where the ADR keeps it: SPLIT inside an open or a closed
 * contract, insertion when nothing covers. EXACT_START is the one that
 * changes: it is no longer a silent replacement but a rejected plan that
 * names the contract to correct (backend#52).
 */
@ExtendWith(MockitoExtension.class)
class ReplaceContractFromDateServiceTest {

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
    private ReplaceContractFromDateService service;

    @BeforeEach
    void setUp() {
        contractCatalogValidator = new TestContractCatalogValidator();
        contractSubtypeRelationValidator = new TestContractSubtypeRelationValidator();

        service = new ReplaceContractFromDateService(
                contractRepository,
                employeeContractLookupPort,
                contractCatalogValidator,
                contractSubtypeRelationValidator,
                new ContractTimelineService(contractRepository, presencePort)
        );
    }

    @Test
    void replaceInsideOpenPeriodSplitsTimelineSafely() {
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

        Contract replaced = service.replaceFromDate(command(
                LocalDate.of(2026, 3, 1),
                "tmp",
                "pt1"
        ));

        assertEquals(LocalDate.of(2026, 3, 1), replaced.getStartDate());
        assertEquals(null, replaced.getEndDate());
        assertEquals("TMP", replaced.getContractCode());
        assertEquals("PT1", replaced.getContractSubtypeCode());

        ArgumentCaptor<Contract> updatedCaptor = ArgumentCaptor.forClass(Contract.class);
        ArgumentCaptor<Contract> savedCaptor = ArgumentCaptor.forClass(Contract.class);
        verify(contractRepository).update(updatedCaptor.capture(), any(LocalDate.class));
        verify(contractRepository).save(savedCaptor.capture());

        assertEquals("IND", updatedCaptor.getValue().getContractCode());
        assertEquals("FT1", updatedCaptor.getValue().getContractSubtypeCode());
        assertEquals(LocalDate.of(2026, 1, 1), updatedCaptor.getValue().getStartDate());
        assertEquals(LocalDate.of(2026, 2, 28), updatedCaptor.getValue().getEndDate());

        assertEquals("TMP", savedCaptor.getValue().getContractCode());
        assertEquals("PT1", savedCaptor.getValue().getContractSubtypeCode());
        assertEquals(LocalDate.of(2026, 3, 1), savedCaptor.getValue().getStartDate());
        assertEquals(null, savedCaptor.getValue().getEndDate());
    }

    @Test
    void replaceInsideClosedPeriodPreservesOriginalEndDate() {
        Contract existing = new Contract(
                10L,
                "IND",
                "FT1",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31)
        );

        // The employee left on the same day the contract ends: no gap after it.
        givenEmployeePresent(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), existing);
        when(contractRepository.findByEmployeeIdAndStartDate(10L, LocalDate.of(2026, 1, 1)))
                .thenReturn(Optional.of(existing));

        Contract replaced = service.replaceFromDate(command(
                LocalDate.of(2026, 3, 1),
                "TMP",
                "PT1"
        ));

        assertEquals(LocalDate.of(2026, 3, 1), replaced.getStartDate());
        assertEquals(LocalDate.of(2026, 3, 31), replaced.getEndDate());

        ArgumentCaptor<Contract> updatedCaptor = ArgumentCaptor.forClass(Contract.class);
        ArgumentCaptor<Contract> savedCaptor = ArgumentCaptor.forClass(Contract.class);
        verify(contractRepository).update(updatedCaptor.capture(), any(LocalDate.class));
        verify(contractRepository).save(savedCaptor.capture());

        assertEquals(LocalDate.of(2026, 2, 28), updatedCaptor.getValue().getEndDate());
        assertEquals(LocalDate.of(2026, 3, 31), savedCaptor.getValue().getEndDate());
    }

    // Was replaceAtExactStartDateUpdatesWithoutDuplicateIdentityRow: the old
    // EXACT_START replaced the codes silently. ADR-057 §6 keeps what the user
    // wanted (no second row on the same start) but says it out loud: the plan
    // is a correction, it is rejected as such, and nothing is written.
    @Test
    void replaceAtExactStartDateIsRejectedAsACorrectionAndWritesNoDuplicateIdentityRow() {
        Contract existing = new Contract(
                10L,
                "IND",
                "FT1",
                LocalDate.of(2026, 3, 1),
                null
        );

        givenEmployeePresent(LocalDate.of(2026, 3, 1), null, existing);

        ContractIsACorrectionException ex = assertThrows(
                ContractIsACorrectionException.class,
                () -> service.replaceFromDate(command(LocalDate.of(2026, 3, 1), "TMP", "PT1"))
        );

        assertEquals(new ContractPeriod(LocalDate.of(2026, 3, 1), null), ex.correctedOccurrence());
        verify(contractRepository, never()).update(any(Contract.class), any(LocalDate.class));
        verify(contractRepository, never()).save(any(Contract.class));
    }

    @Test
    void rejectsWhenContractSubtypeRelationIsInvalid() {
        contractSubtypeRelationValidator.setInvalidRelation(true);
        whenEmployeeExists();

        assertThrows(
                ContractSubtypeRelationInvalidException.class,
                () -> service.replaceFromDate(command(LocalDate.of(2026, 3, 1), "TMP", "PT1"))
        );
        verify(contractRepository, never()).save(any(Contract.class));
        verify(contractRepository, never()).update(any(Contract.class), any(LocalDate.class));
    }

    @Test
    void rejectsWhenEmployeeDoesNotExist() {
        when(employeeContractLookupPort.findByBusinessKeyForUpdate(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER
        )).thenReturn(Optional.empty());

        assertThrows(
                ContractEmployeeNotFoundException.class,
                () -> service.replaceFromDate(command(LocalDate.of(2026, 3, 1), "TMP", "PT1"))
        );

        verify(contractRepository, never()).save(any(Contract.class));
        verify(contractRepository, never()).update(any(Contract.class), any(LocalDate.class));
    }

    @Test
    void rejectsWhenReplacementBreaksPresenceCoverage() {
        Contract closedInJanuary = new Contract(
                10L,
                "IND",
                "FT1",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        );

        givenEmployeePresent(LocalDate.of(2026, 1, 1), null, closedInJanuary);

        ContractCoverageIncompleteException ex = assertThrows(
                ContractCoverageIncompleteException.class,
                () -> service.replaceFromDate(command(LocalDate.of(2026, 3, 1), "TMP", "PT1"))
        );

        assertEquals(List.of(new ContractPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))), ex.gaps());
        verify(contractRepository, never()).save(any(Contract.class));
        verify(contractRepository, never()).update(any(Contract.class), any(LocalDate.class));
    }

    @Test
    void rejectsWhenReplacementIsOutsidePresence() {
        Contract existing = new Contract(
                10L,
                "IND",
                "FT1",
                LocalDate.of(2026, 1, 1),
                null
        );

        givenEmployeePresent(LocalDate.of(2026, 1, 1), null, existing);

        assertThrows(
                ContractOutsidePresencePeriodException.class,
                () -> service.replaceFromDate(command(LocalDate.of(2025, 12, 1), "TMP", "PT1"))
        );

        verify(contractRepository, never()).save(any(Contract.class));
        verify(contractRepository, never()).update(any(Contract.class), any(LocalDate.class));
    }

    @Test
    void rejectsWhenNoCoveringPeriodAndProjectedTimelineOverlaps() {
        Contract futureOpen = new Contract(
                10L,
                "IND",
                "FT1",
                LocalDate.of(2026, 4, 1),
                null
        );

        givenEmployeePresent(LocalDate.of(2026, 1, 1), null, futureOpen);

        assertThrows(
                ContractOverlapException.class,
                () -> service.replaceFromDate(command(LocalDate.of(2026, 3, 1), "TMP", "PT1"))
        );

        verify(contractRepository, never()).save(any(Contract.class));
        verify(contractRepository, never()).update(any(Contract.class), any(LocalDate.class));
    }

    @Test
    void createsNewPeriodWhenNoCoveringAndProjectedTimelineIsValid() {
        givenEmployeePresent(LocalDate.of(2026, 3, 1), null);

        Contract replaced = service.replaceFromDate(command(
                LocalDate.of(2026, 3, 1),
                "TMP",
                "PT1"
        ));

        assertEquals(LocalDate.of(2026, 3, 1), replaced.getStartDate());
        assertEquals(null, replaced.getEndDate());

        verify(contractRepository, never()).update(any(Contract.class), any(LocalDate.class));
        verify(contractRepository).save(any(Contract.class));
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

    private ReplaceContractFromDateCommand command(
            LocalDate effectiveDate,
            String contractCode,
            String contractSubtypeCode
    ) {
        return new ReplaceContractFromDateCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                effectiveDate,
                contractCode,
                contractSubtypeCode
        );
    }

    private static final class TestContractCatalogValidator extends ContractCatalogValidator {

        private TestContractCatalogValidator() {
            super(null);
        }

        @Override
        public String normalizeRequiredCode(String fieldName, String value) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException(fieldName + " is required");
            }

            return value.trim().toUpperCase();
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

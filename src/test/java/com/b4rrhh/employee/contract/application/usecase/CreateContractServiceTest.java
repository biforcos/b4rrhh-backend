package com.b4rrhh.employee.contract.application.usecase;

import com.b4rrhh.employee.contract.application.command.CreateContractCommand;
import com.b4rrhh.employee.contract.application.port.ContractPresenceConsistencyPort;
import com.b4rrhh.employee.contract.application.port.EmployeeContractContext;
import com.b4rrhh.employee.contract.application.port.EmployeeContractLookupPort;
import com.b4rrhh.employee.contract.application.port.PresencePeriod;
import com.b4rrhh.employee.contract.application.service.ContractSubtypeRelationValidator;
import com.b4rrhh.employee.contract.application.service.ContractCatalogValidator;
import com.b4rrhh.employee.contract.application.service.ContractTimelineService;
import com.b4rrhh.employee.contract.domain.exception.InvalidContractDateRangeException;
import com.b4rrhh.employee.contract.domain.exception.ContractSubtypeRelationInvalidException;
import com.b4rrhh.employee.contract.domain.exception.ContractInvalidException;
import com.b4rrhh.employee.contract.domain.exception.ContractIsACorrectionException;
import com.b4rrhh.employee.contract.domain.exception.ContractSubtypeInvalidException;
import com.b4rrhh.employee.contract.domain.exception.ContractCoverageIncompleteException;
import com.b4rrhh.employee.contract.domain.exception.ContractEmployeeNotFoundException;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Adding a contract is planned against the invariants of the series
 * (ADR-057). The timeline service is real and the repository and presence
 * port are mocked: the employee is present from 2026-01-01 onwards.
 */
@ExtendWith(MockitoExtension.class)
class CreateContractServiceTest {

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

    private TestContractCatalogValidator contractCatalogValidator;
    private TestContractSubtypeRelationValidator contractSubtypeRelationValidator;
    private CreateContractService service;

    @BeforeEach
    void setUp() {
        contractCatalogValidator = new TestContractCatalogValidator();
        contractSubtypeRelationValidator = new TestContractSubtypeRelationValidator();

        service = new CreateContractService(
                contractRepository,
                employeeContractLookupPort,
                contractCatalogValidator,
                contractSubtypeRelationValidator,
                new ContractTimelineService(contractRepository, presencePort)
        );
    }

    @Test
    void rejectsWhenEmployeeDoesNotExist() {
        CreateContractCommand command = command(
                "IND",
                "FT1",
                LocalDate.of(2026, 1, 1),
                null
        );

        when(employeeContractLookupPort.findByBusinessKeyForUpdate(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER
        )).thenReturn(Optional.empty());

        assertThrows(ContractEmployeeNotFoundException.class, () -> service.create(command));
    }

    @Test
    void rejectsInvalidContractCode() {
        CreateContractCommand command = command(
            "BAD",
                "FT1",
                LocalDate.of(2026, 1, 1),
                null
        );

        contractCatalogValidator.markContractInvalid("BAD");
        whenEmployeeExists();

        assertThrows(ContractInvalidException.class, () -> service.create(command));
    }

    @Test
    void rejectsInvalidContractSubtypeCode() {
        CreateContractCommand command = command(
                "IND",
            "BCT",
                LocalDate.of(2026, 1, 1),
                null
        );

        contractCatalogValidator.markSubtypeInvalid("BCT");
        whenEmployeeExists();

        assertThrows(ContractSubtypeInvalidException.class, () -> service.create(command));
    }

    @Test
    void rejectsInvalidContractSubtypeRelation() {
        CreateContractCommand command = command(
                "IND",
                "FT1",
                LocalDate.of(2026, 1, 1),
                null
        );

        contractSubtypeRelationValidator.setInvalidRelation(true);
        whenEmployeeExists();

        assertThrows(ContractSubtypeRelationInvalidException.class, () -> service.create(command));
    }

    @Test
    void rejectsInvalidDateRange() {
        CreateContractCommand command = command(
                "IND",
                "FT1",
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 1, 1)
        );

        whenEmployeeExists();

        assertThrows(InvalidContractDateRangeException.class, () -> service.create(command));
    }

    @Test
    void rejectsOverlapOnCreateNamingTheSharedDates() {
        Contract first = contract(PRESENCE_START, LocalDate.of(2026, 1, 31));
        Contract second = contract(LocalDate.of(2026, 2, 1), null);
        givenEmployeeWithSeries(first, second);

        ContractOverlapException ex = assertThrows(
                ContractOverlapException.class,
                () -> service.create(command("IND", "FT1", LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 10)))
        );

        assertEquals(List.of(new ContractPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 10))), ex.overlaps());
        verify(contractRepository, never()).save(any(Contract.class));
        verify(contractRepository, never()).update(any(Contract.class), any(LocalDate.class));
    }

    @Test
    void rejectsOutsidePresenceOnCreate() {
        givenEmployeeWithSeries();

        assertThrows(
                ContractOutsidePresencePeriodException.class,
                () -> service.create(command("IND", "FT1", LocalDate.of(2025, 12, 1), null))
        );
        verify(contractRepository, never()).save(any(Contract.class));
    }

    @Test
    void rejectsIncompleteCoverageOnCreateSayingWhichGapAndWhatToStretch() {
        givenEmployeeWithSeries();

        ContractCoverageIncompleteException ex = assertThrows(
                ContractCoverageIncompleteException.class,
                () -> service.create(command("IND", "FT1", LocalDate.of(2026, 3, 1), null))
        );

        assertEquals(List.of(new ContractPeriod(PRESENCE_START, LocalDate.of(2026, 2, 28))), ex.gaps());
        assertEquals(List.of(new ContractPeriod(LocalDate.of(2026, 3, 1), null)), ex.stretchCandidates());
        verify(contractRepository, never()).save(any(Contract.class));
    }

    @Test
    void createsWhenValidAndFullCoverage() {
        CreateContractCommand command = command(
                "ind",
                "ft1",
                LocalDate.of(2026, 1, 1),
                null
        );

        givenEmployeeWithSeries();

        Contract created = service.create(command);

        assertEquals("IND", created.getContractCode());
        assertEquals("FT1", created.getContractSubtypeCode());

        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractRepository).save(captor.capture());
        assertEquals("IND", captor.getValue().getContractCode());
        assertEquals("FT1", captor.getValue().getContractSubtypeCode());
        verify(contractRepository, never()).update(any(Contract.class), any(LocalDate.class));
    }

    @Test
    void addingFromTheSixteenthClosesTheOpenOneOnTheFifteenthInsteadOfRejectingIt() {
        Contract open = contract(PRESENCE_START, null);
        givenEmployeeWithSeries(open);
        when(contractRepository.findByEmployeeIdAndStartDate(10L, PRESENCE_START)).thenReturn(Optional.of(open));

        Contract created = service.create(command("TMP", "PT1", LocalDate.of(2026, 1, 16), null));

        assertEquals(LocalDate.of(2026, 1, 16), created.getStartDate());
        assertNull(created.getEndDate());

        ArgumentCaptor<Contract> closedCaptor = ArgumentCaptor.forClass(Contract.class);
        verify(contractRepository).update(closedCaptor.capture(), any(LocalDate.class));
        assertEquals(PRESENCE_START, closedCaptor.getValue().getStartDate());
        assertEquals(LocalDate.of(2026, 1, 15), closedCaptor.getValue().getEndDate());
        assertEquals("IND", closedCaptor.getValue().getContractCode());

        ArgumentCaptor<Contract> savedCaptor = ArgumentCaptor.forClass(Contract.class);
        verify(contractRepository).save(savedCaptor.capture());
        assertEquals("TMP", savedCaptor.getValue().getContractCode());
        assertEquals(LocalDate.of(2026, 1, 16), savedCaptor.getValue().getStartDate());
    }

    @Test
    void rejectsAContractStartingOnTheSameDayAsAnExistingOneAsACorrectionNotAnAdd() {
        Contract open = contract(PRESENCE_START, null);
        givenEmployeeWithSeries(open);

        ContractIsACorrectionException ex = assertThrows(
                ContractIsACorrectionException.class,
                () -> service.create(command("TMP", "PT1", PRESENCE_START, null))
        );

        assertEquals(new ContractPeriod(PRESENCE_START, null), ex.correctedOccurrence());
        verify(contractRepository, never()).save(any(Contract.class));
        verify(contractRepository, never()).update(any(Contract.class), any(LocalDate.class));
    }

    private void givenEmployeeWithSeries(Contract... occurrences) {
        whenEmployeeExists();
        when(contractRepository.findByEmployeeIdOrderByStartDate(10L)).thenReturn(List.of(occurrences));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L))
                .thenReturn(List.of(new PresencePeriod(PRESENCE_START, null)));
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

    private static Contract contract(LocalDate startDate, LocalDate endDate) {
        return new Contract(10L, "IND", "FT1", startDate, endDate);
    }

    private CreateContractCommand command(
            String contractCode,
            String contractSubtypeCode,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new CreateContractCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                contractCode,
                contractSubtypeCode,
                startDate,
                endDate
        );
    }

    private static final class TestContractCatalogValidator extends ContractCatalogValidator {

        private final Set<String> invalidContractCodes = new HashSet<>();
        private final Set<String> invalidSubtypeCodes = new HashSet<>();

        private TestContractCatalogValidator() {
            super(null);
        }

        void markContractInvalid(String code) {
            invalidContractCodes.add(code);
        }

        void markSubtypeInvalid(String code) {
            invalidSubtypeCodes.add(code);
        }

        @Override
        public String normalizeRequiredCode(String fieldName, String value) {
            if (value == null || value.trim().isEmpty()) {
                if ("contractCode".equals(fieldName)) {
                    throw new ContractInvalidException(String.valueOf(value));
                }
                throw new ContractSubtypeInvalidException(String.valueOf(value));
            }

            return value.trim().toUpperCase();
        }

        @Override
        public void validateContractCode(String ruleSystemCode, String contractCode, LocalDate referenceDate) {
            if (invalidContractCodes.contains(contractCode)) {
                throw new ContractInvalidException(contractCode);
            }
        }

        @Override
        public void validateContractSubtypeCode(
                String ruleSystemCode,
                String contractSubtypeCode,
                LocalDate referenceDate
        ) {
            if (invalidSubtypeCodes.contains(contractSubtypeCode)) {
                throw new ContractSubtypeInvalidException(contractSubtypeCode);
            }
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

package com.b4rrhh.employee.labor_classification.application.usecase;

import com.b4rrhh.employee.labor_classification.application.command.CreateLaborClassificationCommand;
import com.b4rrhh.employee.labor_classification.application.port.EmployeeLaborClassificationContext;
import com.b4rrhh.employee.labor_classification.application.port.EmployeeLaborClassificationLookupPort;
import com.b4rrhh.employee.labor_classification.application.port.LaborClassificationPresenceConsistencyPort;
import com.b4rrhh.employee.labor_classification.application.port.PresencePeriod;
import com.b4rrhh.employee.labor_classification.application.service.AgreementCategoryRelationValidator;
import com.b4rrhh.employee.labor_classification.application.service.LaborClassificationCatalogValidator;
import com.b4rrhh.employee.labor_classification.application.service.LaborClassificationTimelineService;
import com.b4rrhh.employee.labor_classification.domain.exception.InvalidLaborClassificationDateRangeException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationAgreementCategoryRelationInvalidException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationAgreementInvalidException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationCategoryInvalidException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationCoverageIncompleteException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationEmployeeNotFoundException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationIsACorrectionException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationOutsidePresencePeriodException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationOverlapException;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassificationPeriod;
import com.b4rrhh.employee.labor_classification.domain.port.LaborClassificationRepository;
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
 * Adding a labor classification is planned against the invariants of the
 * series (ADR-057). The timeline service is real and the repository and
 * presence port are mocked: the employee is present from 2026-01-01 onwards.
 */
@ExtendWith(MockitoExtension.class)
class CreateLaborClassificationServiceTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String EMPLOYEE_NUMBER = "EMP001";
    private static final LocalDate PRESENCE_START = LocalDate.of(2026, 1, 1);

    @Mock
    private LaborClassificationRepository laborClassificationRepository;
    @Mock
    private EmployeeLaborClassificationLookupPort employeeLaborClassificationLookupPort;
    @Mock
    private LaborClassificationPresenceConsistencyPort presencePort;

    private TestLaborClassificationCatalogValidator laborClassificationCatalogValidator;
    private TestAgreementCategoryRelationValidator agreementCategoryRelationValidator;
    private CreateLaborClassificationService service;

    @BeforeEach
    void setUp() {
        laborClassificationCatalogValidator = new TestLaborClassificationCatalogValidator();
        agreementCategoryRelationValidator = new TestAgreementCategoryRelationValidator();

        service = new CreateLaborClassificationService(
                laborClassificationRepository,
                employeeLaborClassificationLookupPort,
                laborClassificationCatalogValidator,
                agreementCategoryRelationValidator,
                new LaborClassificationTimelineService(laborClassificationRepository, presencePort)
        );
    }

    @Test
    void rejectsWhenEmployeeDoesNotExist() {
        CreateLaborClassificationCommand command = command(
                "AGR_OFFICE",
                "CAT_ADMIN",
                LocalDate.of(2026, 1, 1),
                null
        );

        when(employeeLaborClassificationLookupPort.findByBusinessKeyForUpdate(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER
        )).thenReturn(Optional.empty());

        assertThrows(LaborClassificationEmployeeNotFoundException.class, () -> service.create(command));
    }

    @Test
    void rejectsInvalidAgreementCode() {
        CreateLaborClassificationCommand command = command(
                "BAD_AGR",
                "CAT_ADMIN",
                LocalDate.of(2026, 1, 1),
                null
        );

        laborClassificationCatalogValidator.markAgreementInvalid("BAD_AGR");
        whenEmployeeExists();

        assertThrows(LaborClassificationAgreementInvalidException.class, () -> service.create(command));
    }

    @Test
    void rejectsInvalidAgreementCategoryCode() {
        CreateLaborClassificationCommand command = command(
                "AGR_OFFICE",
                "BAD_CAT",
                LocalDate.of(2026, 1, 1),
                null
        );

        laborClassificationCatalogValidator.markCategoryInvalid("BAD_CAT");
        whenEmployeeExists();

        assertThrows(LaborClassificationCategoryInvalidException.class, () -> service.create(command));
    }

    @Test
    void rejectsInvalidAgreementCategoryRelation() {
        CreateLaborClassificationCommand command = command(
                "AGR_OFFICE",
                "CAT_ADMIN",
                LocalDate.of(2026, 1, 1),
                null
        );

        agreementCategoryRelationValidator.setInvalidRelation(true);
        whenEmployeeExists();

        assertThrows(LaborClassificationAgreementCategoryRelationInvalidException.class, () -> service.create(command));
    }

    @Test
    void rejectsInvalidDateRange() {
        CreateLaborClassificationCommand command = command(
                "AGR_OFFICE",
                "CAT_ADMIN",
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 1, 1)
        );

        whenEmployeeExists();

        assertThrows(InvalidLaborClassificationDateRangeException.class, () -> service.create(command));
    }

    @Test
    void rejectsOverlapOnCreateNamingTheSharedDates() {
        LaborClassification first = occurrence(PRESENCE_START, LocalDate.of(2026, 1, 31));
        LaborClassification second = occurrence(LocalDate.of(2026, 2, 1), null);
        givenEmployeeWithSeries(first, second);

        LaborClassificationOverlapException ex = assertThrows(
                LaborClassificationOverlapException.class,
                () -> service.create(command("AGR_OFFICE", "CAT_ADMIN", LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 10)))
        );

        assertEquals(
                List.of(new LaborClassificationPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 10))),
                ex.overlaps()
        );
        verify(laborClassificationRepository, never()).save(any(LaborClassification.class));
        verify(laborClassificationRepository, never()).update(any(LaborClassification.class), any(LocalDate.class));
    }

    @Test
    void rejectsOutsidePresenceOnCreate() {
        givenEmployeeWithSeries();

        assertThrows(
                LaborClassificationOutsidePresencePeriodException.class,
                () -> service.create(command("AGR_OFFICE", "CAT_ADMIN", LocalDate.of(2025, 12, 1), null))
        );
        verify(laborClassificationRepository, never()).save(any(LaborClassification.class));
    }

    @Test
    void rejectsIncompleteCoverageOnCreateSayingWhichGapAndWhatToStretch() {
        givenEmployeeWithSeries();

        LaborClassificationCoverageIncompleteException ex = assertThrows(
                LaborClassificationCoverageIncompleteException.class,
                () -> service.create(command("AGR_OFFICE", "CAT_ADMIN", LocalDate.of(2026, 3, 1), null))
        );

        assertEquals(List.of(new LaborClassificationPeriod(PRESENCE_START, LocalDate.of(2026, 2, 28))), ex.gaps());
        assertEquals(List.of(new LaborClassificationPeriod(LocalDate.of(2026, 3, 1), null)), ex.stretchCandidates());
        verify(laborClassificationRepository, never()).save(any(LaborClassification.class));
    }

    @Test
    void createsWhenValidAndFullCoverage() {
        CreateLaborClassificationCommand command = command(
                "agr_office",
                "cat_admin",
                LocalDate.of(2026, 1, 1),
                null
        );

        givenEmployeeWithSeries();

        LaborClassification created = service.create(command);

        assertEquals("AGR_OFFICE", created.getAgreementCode());
        assertEquals("CAT_ADMIN", created.getAgreementCategoryCode());

        ArgumentCaptor<LaborClassification> captor = ArgumentCaptor.forClass(LaborClassification.class);
        verify(laborClassificationRepository).save(captor.capture());
        assertEquals("AGR_OFFICE", captor.getValue().getAgreementCode());
        assertEquals("CAT_ADMIN", captor.getValue().getAgreementCategoryCode());
        verify(laborClassificationRepository, never()).update(any(LaborClassification.class), any(LocalDate.class));
    }

    @Test
    void addingFromTheSixteenthClosesTheOpenOneOnTheFifteenthInsteadOfRejectingIt() {
        LaborClassification open = occurrence(PRESENCE_START, null);
        givenEmployeeWithSeries(open);
        when(laborClassificationRepository.findByEmployeeIdAndStartDate(10L, PRESENCE_START)).thenReturn(Optional.of(open));

        LaborClassification created = service.create(command("AGR_TECH", "CAT_TECH_1", LocalDate.of(2026, 1, 16), null));

        assertEquals(LocalDate.of(2026, 1, 16), created.getStartDate());
        assertNull(created.getEndDate());

        ArgumentCaptor<LaborClassification> closedCaptor = ArgumentCaptor.forClass(LaborClassification.class);
        verify(laborClassificationRepository).update(closedCaptor.capture(), any(LocalDate.class));
        assertEquals(PRESENCE_START, closedCaptor.getValue().getStartDate());
        assertEquals(LocalDate.of(2026, 1, 15), closedCaptor.getValue().getEndDate());
        assertEquals("AGR_OFFICE", closedCaptor.getValue().getAgreementCode());

        ArgumentCaptor<LaborClassification> savedCaptor = ArgumentCaptor.forClass(LaborClassification.class);
        verify(laborClassificationRepository).save(savedCaptor.capture());
        assertEquals("AGR_TECH", savedCaptor.getValue().getAgreementCode());
        assertEquals(LocalDate.of(2026, 1, 16), savedCaptor.getValue().getStartDate());
    }

    @Test
    void rejectsAnOccurrenceStartingOnTheSameDayAsAnExistingOneAsACorrectionNotAnAdd() {
        LaborClassification open = occurrence(PRESENCE_START, null);
        givenEmployeeWithSeries(open);

        LaborClassificationIsACorrectionException ex = assertThrows(
                LaborClassificationIsACorrectionException.class,
                () -> service.create(command("AGR_TECH", "CAT_TECH_1", PRESENCE_START, null))
        );

        assertEquals(new LaborClassificationPeriod(PRESENCE_START, null), ex.correctedOccurrence());
        verify(laborClassificationRepository, never()).save(any(LaborClassification.class));
        verify(laborClassificationRepository, never()).update(any(LaborClassification.class), any(LocalDate.class));
    }

    private void givenEmployeeWithSeries(LaborClassification... occurrences) {
        whenEmployeeExists();
        when(laborClassificationRepository.findByEmployeeIdOrderByStartDate(10L)).thenReturn(List.of(occurrences));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L))
                .thenReturn(List.of(new PresencePeriod(PRESENCE_START, null)));
    }

    private void whenEmployeeExists() {
        when(employeeLaborClassificationLookupPort.findByBusinessKeyForUpdate(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER
        )).thenReturn(Optional.of(new EmployeeLaborClassificationContext(
                10L,
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER
        )));
    }

    private static LaborClassification occurrence(LocalDate startDate, LocalDate endDate) {
        return new LaborClassification(10L, "AGR_OFFICE", "CAT_ADMIN", startDate, endDate);
    }

    private CreateLaborClassificationCommand command(
            String agreementCode,
            String agreementCategoryCode,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new CreateLaborClassificationCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                agreementCode,
                agreementCategoryCode,
                startDate,
                endDate
        );
    }

    private static final class TestLaborClassificationCatalogValidator extends LaborClassificationCatalogValidator {

        private final Set<String> invalidAgreementCodes = new HashSet<>();
        private final Set<String> invalidCategoryCodes = new HashSet<>();

        private TestLaborClassificationCatalogValidator() {
            super(null);
        }

        void markAgreementInvalid(String code) {
            invalidAgreementCodes.add(code);
        }

        void markCategoryInvalid(String code) {
            invalidCategoryCodes.add(code);
        }

        @Override
        public String normalizeRequiredCode(String fieldName, String value) {
            if (value == null || value.trim().isEmpty()) {
                if ("agreementCode".equals(fieldName)) {
                    throw new LaborClassificationAgreementInvalidException(String.valueOf(value));
                }
                throw new LaborClassificationCategoryInvalidException(String.valueOf(value));
            }

            return value.trim().toUpperCase();
        }

        @Override
        public void validateAgreementCode(String ruleSystemCode, String agreementCode, LocalDate referenceDate) {
            if (invalidAgreementCodes.contains(agreementCode)) {
                throw new LaborClassificationAgreementInvalidException(agreementCode);
            }
        }

        @Override
        public void validateAgreementCategoryCode(
                String ruleSystemCode,
                String agreementCategoryCode,
                LocalDate referenceDate
        ) {
            if (invalidCategoryCodes.contains(agreementCategoryCode)) {
                throw new LaborClassificationCategoryInvalidException(agreementCategoryCode);
            }
        }
    }

    private static final class TestAgreementCategoryRelationValidator extends AgreementCategoryRelationValidator {

        private boolean invalidRelation;

        private TestAgreementCategoryRelationValidator() {
            super(null);
        }

        void setInvalidRelation(boolean invalidRelation) {
            this.invalidRelation = invalidRelation;
        }

        @Override
        public void validateAgreementCategoryRelation(
                String ruleSystemCode,
                String agreementCode,
                String agreementCategoryCode,
                LocalDate referenceDate
        ) {
            if (invalidRelation) {
                throw new LaborClassificationAgreementCategoryRelationInvalidException(
                        ruleSystemCode,
                        agreementCode,
                        agreementCategoryCode,
                        referenceDate
                );
            }
        }
    }
}

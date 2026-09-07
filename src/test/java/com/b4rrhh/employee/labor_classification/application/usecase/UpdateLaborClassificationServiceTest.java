package com.b4rrhh.employee.labor_classification.application.usecase;

import com.b4rrhh.employee.labor_classification.application.command.UpdateLaborClassificationCommand;
import com.b4rrhh.employee.labor_classification.application.port.EmployeeLaborClassificationContext;
import com.b4rrhh.employee.labor_classification.application.port.EmployeeLaborClassificationLookupPort;
import com.b4rrhh.employee.labor_classification.application.port.LaborClassificationPresenceConsistencyPort;
import com.b4rrhh.employee.labor_classification.application.port.PresencePeriod;
import com.b4rrhh.employee.labor_classification.application.service.AgreementCategoryRelationValidator;
import com.b4rrhh.employee.labor_classification.application.service.LaborClassificationCatalogValidator;
import com.b4rrhh.employee.labor_classification.application.service.LaborClassificationTimelineService;
import com.b4rrhh.employee.labor_classification.domain.exception.InvalidLaborClassificationDateRangeException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationAgreementCategoryRelationInvalidException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationCoverageIncompleteException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationNotFoundException;
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
 * Correcting a labor classification moves only the corrected one (ADR-057,
 * decision 3). The timeline service is real and the repository and presence
 * port are mocked; each test says from when the employee is present.
 */
@ExtendWith(MockitoExtension.class)
class UpdateLaborClassificationServiceTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String EMPLOYEE_NUMBER = "EMP001";

    @Mock
    private LaborClassificationRepository laborClassificationRepository;
    @Mock
    private EmployeeLaborClassificationLookupPort employeeLaborClassificationLookupPort;
    @Mock
    private LaborClassificationPresenceConsistencyPort presencePort;

    private TestLaborClassificationCatalogValidator laborClassificationCatalogValidator;
    private TestAgreementCategoryRelationValidator agreementCategoryRelationValidator;
    private UpdateLaborClassificationService service;

    @BeforeEach
    void setUp() {
        laborClassificationCatalogValidator = new TestLaborClassificationCatalogValidator();
        agreementCategoryRelationValidator = new TestAgreementCategoryRelationValidator();

        service = new UpdateLaborClassificationService(
                laborClassificationRepository,
                employeeLaborClassificationLookupPort,
                laborClassificationCatalogValidator,
                agreementCategoryRelationValidator,
                new LaborClassificationTimelineService(laborClassificationRepository, presencePort)
        );
    }

    // Keeping the dates is said by sending them, not by leaving them out
    // (backend#69).
    @Test
    void correctsTheCodesKeepingTheDates() {
        LaborClassification existing = occurrence("AGR_OFFICE", "CAT_ADMIN", LocalDate.of(2026, 1, 1), null);
        givenEmployeePresentFrom(LocalDate.of(2026, 1, 1), existing);
        whenOccurrenceExists(existing);

        LaborClassification updated = service.update(
                command(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), null, "AGR_TECH", "CAT_TECH_1"));

        assertEquals("AGR_TECH", updated.getAgreementCode());
        assertEquals("CAT_TECH_1", updated.getAgreementCategoryCode());
        assertEquals(LocalDate.of(2026, 1, 1), updated.getStartDate());
        assertNull(updated.getEndDate());

        ArgumentCaptor<LaborClassification> captor = ArgumentCaptor.forClass(LaborClassification.class);
        verify(laborClassificationRepository).update(captor.capture(), eq(LocalDate.of(2026, 1, 1)));
        assertEquals("AGR_TECH", captor.getValue().getAgreementCode());
        assertEquals("CAT_TECH_1", captor.getValue().getAgreementCategoryCode());
    }

    // Before ADR-057 a closed occurrence could not be corrected. What identifies
    // it is the day it starts, and a wrong category on a past one is still
    // wrong: the resulting series decides, not whether it is open.
    @Test
    void correctsAClosedOccurrenceStretchingItOverTheGapItLeft() {
        LaborClassification closedTooEarly = occurrence(
                "AGR_OFFICE", "CAT_ADMIN", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        givenEmployeePresentFrom(LocalDate.of(2026, 1, 1), closedTooEarly);
        whenOccurrenceExists(closedTooEarly);

        LaborClassification updated = service.update(
                command(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), null, "AGR_TECH", "CAT_TECH_1"));

        assertNull(updated.getEndDate());
        assertEquals("AGR_TECH", updated.getAgreementCode());
        verify(laborClassificationRepository).update(any(LaborClassification.class), eq(LocalDate.of(2026, 1, 1)));
    }

    @Test
    void rejectsUpdateWhenAgreementCategoryRelationIsInvalid() {
        LaborClassification existing = occurrence("AGR_OFFICE", "CAT_ADMIN", LocalDate.of(2026, 1, 1), null);
        agreementCategoryRelationValidator.setInvalidRelation(true);
        whenEmployeeExists();
        whenOccurrenceExists(existing);

        assertThrows(
                LaborClassificationAgreementCategoryRelationInvalidException.class,
                () -> service.update(
                        command(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), null, "AGR_TECH", "CAT_TECH_1"))
        );
        verify(laborClassificationRepository, never()).update(any(LaborClassification.class), any(LocalDate.class));
    }

    // The old cascade (the predecessor followed the new start on its own) is
    // what ADR-057 decision 3 retires: stretching a neighbour is the user's
    // act. The plan rejects the gap and names the predecessor to stretch.
    @Test
    void movingTheStartLaterDoesNotStretchThePredecessorButNamesIt() {
        LaborClassification predecessor = occurrence(
                "AGR_TECH", "CAT_TECH_1", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        LaborClassification current = occurrence("AGR_TECH", "CAT_TECH_1", LocalDate.of(2025, 1, 1), null);
        givenEmployeePresentFrom(LocalDate.of(2024, 1, 1), predecessor, current);
        whenOccurrenceExists(current);

        LaborClassificationCoverageIncompleteException ex = assertThrows(
                LaborClassificationCoverageIncompleteException.class,
                () -> service.update(command(
                        LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1), null, "AGR_TECH", "CAT_TECH_1"))
        );

        assertEquals(
                List.of(new LaborClassificationPeriod(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))),
                ex.gaps()
        );
        assertTrue(ex.stretchCandidates().contains(
                new LaborClassificationPeriod(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))));
        verify(laborClassificationRepository, never()).update(any(LaborClassification.class), any(LocalDate.class));
    }

    @Test
    void movingTheStartEarlierOverAnUncoveredStretchUpdatesOnlyTheCorrectedOne() {
        LaborClassification current = occurrence("AGR_TECH", "CAT_TECH_1", LocalDate.of(2025, 1, 1), null);
        givenEmployeePresentFrom(LocalDate.of(2024, 12, 1), current);
        whenOccurrenceExists(current);

        service.update(command(LocalDate.of(2025, 1, 1), LocalDate.of(2024, 12, 1), null, "AGR_TECH", "CAT_TECH_1"));

        ArgumentCaptor<LaborClassification> captor = ArgumentCaptor.forClass(LaborClassification.class);
        verify(laborClassificationRepository).update(captor.capture(), eq(LocalDate.of(2025, 1, 1)));
        assertEquals(LocalDate.of(2024, 12, 1), captor.getValue().getStartDate());
    }

    @Test
    void movingTheStartEarlierIntoThePredecessorIsRejectedAsAnOverlap() {
        LaborClassification predecessor = occurrence(
                "AGR_TECH", "CAT_TECH_1", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        LaborClassification current = occurrence("AGR_TECH", "CAT_TECH_1", LocalDate.of(2025, 1, 1), null);
        givenEmployeePresentFrom(LocalDate.of(2024, 1, 1), predecessor, current);
        whenOccurrenceExists(current);

        LaborClassificationOverlapException ex = assertThrows(
                LaborClassificationOverlapException.class,
                () -> service.update(command(
                        LocalDate.of(2025, 1, 1), LocalDate.of(2024, 12, 15), null, "AGR_TECH", "CAT_TECH_1"))
        );

        assertEquals(
                List.of(new LaborClassificationPeriod(LocalDate.of(2024, 12, 15), LocalDate.of(2024, 12, 31))),
                ex.overlaps()
        );
        verify(laborClassificationRepository, never()).update(any(LaborClassification.class), any(LocalDate.class));
    }

    @Test
    void movingBeforeThePresenceIsRejected() {
        LaborClassification current = occurrence("AGR_TECH", "CAT_TECH_1", LocalDate.of(2025, 1, 1), null);
        givenEmployeePresentFrom(LocalDate.of(2025, 1, 1), current);
        whenOccurrenceExists(current);

        assertThrows(
                LaborClassificationOutsidePresencePeriodException.class,
                () -> service.update(command(
                        LocalDate.of(2025, 1, 1), LocalDate.of(2024, 12, 1), null, "AGR_TECH", "CAT_TECH_1"))
        );
        verify(laborClassificationRepository, never()).update(any(LaborClassification.class), any(LocalDate.class));
    }

    // A body without a start date used to mean "leave it where it is", which
    // is what a client that forgot to send it looks like too. The two arrived
    // identical and both got a 200, so a screen could stop moving the start
    // and nobody would hear about it — three of them did (backend#69). Now
    // the silence is a rejection, and nothing is written.
    @Test
    void rejectsACorrectionThatDoesNotSayWhereTheOccurrenceStarts() {
        LaborClassification existing = occurrence("AGR_OFFICE", "CAT_ADMIN", LocalDate.of(2026, 1, 1), null);
        whenEmployeeExists();
        whenOccurrenceExists(existing);

        InvalidLaborClassificationDateRangeException rejected = assertThrows(
                InvalidLaborClassificationDateRangeException.class,
                () -> service.update(command(LocalDate.of(2026, 1, 1), null, null, "AGR_TECH", "CAT_TECH_1"))
        );

        assertTrue(rejected.getMessage().contains("startDate"));
        verify(laborClassificationRepository, never())
                .update(any(LaborClassification.class), any(LocalDate.class));
    }

    // The body is not even looked at: an occurrence that is not there is a
    // 404, whatever the body says or leaves out.
    @Test
    void rejectsWhenTheOccurrenceDoesNotExist() {
        whenEmployeeExists();
        when(laborClassificationRepository.findByEmployeeIdAndStartDate(10L, LocalDate.of(2026, 1, 1)))
                .thenReturn(Optional.empty());

        assertThrows(
                LaborClassificationNotFoundException.class,
                () -> service.update(command(LocalDate.of(2026, 1, 1), null, null, "AGR_TECH", "CAT_TECH_1"))
        );
    }

    private UpdateLaborClassificationCommand command(
            LocalDate startDate,
            LocalDate newStartDate,
            LocalDate endDate,
            String agreementCode,
            String agreementCategoryCode
    ) {
        return new UpdateLaborClassificationCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                startDate,
                newStartDate,
                endDate,
                agreementCode,
                agreementCategoryCode
        );
    }

    private void givenEmployeePresentFrom(LocalDate presenceStart, LaborClassification... occurrences) {
        whenEmployeeExists();
        when(laborClassificationRepository.findByEmployeeIdOrderByStartDate(10L)).thenReturn(List.of(occurrences));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L))
                .thenReturn(List.of(new PresencePeriod(presenceStart, null)));
    }

    private void whenOccurrenceExists(LaborClassification existing) {
        when(laborClassificationRepository.findByEmployeeIdAndStartDate(10L, existing.getStartDate()))
                .thenReturn(Optional.of(existing));
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

    private static LaborClassification occurrence(String agreement, String category, LocalDate startDate, LocalDate endDate) {
        return new LaborClassification(10L, agreement, category, startDate, endDate);
    }

    private static final class TestLaborClassificationCatalogValidator extends LaborClassificationCatalogValidator {

        private TestLaborClassificationCatalogValidator() {
            super(null);
        }

        @Override
        public String normalizeRequiredCode(String fieldName, String value) {
            return value == null ? null : value.trim().toUpperCase();
        }

        @Override
        public void validateAgreementCode(String ruleSystemCode, String agreementCode, LocalDate referenceDate) {
            // Always valid in these tests.
        }

        @Override
        public void validateAgreementCategoryCode(
                String ruleSystemCode,
                String agreementCategoryCode,
                LocalDate referenceDate
        ) {
            // Always valid in these tests.
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

package com.b4rrhh.employee.labor_classification.application.usecase;

import com.b4rrhh.employee.labor_classification.application.command.ReplaceLaborClassificationFromDateCommand;
import com.b4rrhh.employee.labor_classification.application.port.EmployeeLaborClassificationContext;
import com.b4rrhh.employee.labor_classification.application.port.EmployeeLaborClassificationLookupPort;
import com.b4rrhh.employee.labor_classification.application.port.LaborClassificationPresenceConsistencyPort;
import com.b4rrhh.employee.labor_classification.application.port.PresencePeriod;
import com.b4rrhh.employee.labor_classification.application.service.AgreementCategoryRelationValidator;
import com.b4rrhh.employee.labor_classification.application.service.LaborClassificationCatalogValidator;
import com.b4rrhh.employee.labor_classification.application.service.LaborClassificationTimelineService;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationAgreementCategoryRelationInvalidException;
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
 * occurrence, insertion when nothing covers. EXACT_START is the one that
 * changes: it is no longer a silent replacement but a rejected plan that
 * names the occurrence to correct (backend#52).
 */
@ExtendWith(MockitoExtension.class)
class ReplaceLaborClassificationFromDateServiceTest {

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
    private ReplaceLaborClassificationFromDateService service;

    @BeforeEach
    void setUp() {
        laborClassificationCatalogValidator = new TestLaborClassificationCatalogValidator();
        agreementCategoryRelationValidator = new TestAgreementCategoryRelationValidator();

        service = new ReplaceLaborClassificationFromDateService(
                laborClassificationRepository,
                employeeLaborClassificationLookupPort,
                laborClassificationCatalogValidator,
                agreementCategoryRelationValidator,
                new LaborClassificationTimelineService(laborClassificationRepository, presencePort)
        );
    }

    @Test
    void replaceInsideOpenPeriodSplitsTimelineSafely() {
        LaborClassification existing = new LaborClassification(
                10L,
                "AGR_OFFICE",
                "CAT_ADMIN",
                LocalDate.of(2026, 1, 1),
                null
        );

        givenEmployeePresent(LocalDate.of(2026, 1, 1), null, existing);
        when(laborClassificationRepository.findByEmployeeIdAndStartDate(10L, LocalDate.of(2026, 1, 1)))
                .thenReturn(Optional.of(existing));

        LaborClassification replaced = service.replaceFromDate(command(
                LocalDate.of(2026, 3, 1),
                "agr_tech",
                "cat_tech_1"
        ));

        assertEquals(LocalDate.of(2026, 3, 1), replaced.getStartDate());
        assertEquals(null, replaced.getEndDate());
        assertEquals("AGR_TECH", replaced.getAgreementCode());
        assertEquals("CAT_TECH_1", replaced.getAgreementCategoryCode());

        ArgumentCaptor<LaborClassification> updatedCaptor = ArgumentCaptor.forClass(LaborClassification.class);
        ArgumentCaptor<LaborClassification> savedCaptor = ArgumentCaptor.forClass(LaborClassification.class);
        verify(laborClassificationRepository).update(updatedCaptor.capture(), any(LocalDate.class));
        verify(laborClassificationRepository).save(savedCaptor.capture());

        assertEquals("AGR_OFFICE", updatedCaptor.getValue().getAgreementCode());
        assertEquals("CAT_ADMIN", updatedCaptor.getValue().getAgreementCategoryCode());
        assertEquals(LocalDate.of(2026, 1, 1), updatedCaptor.getValue().getStartDate());
        assertEquals(LocalDate.of(2026, 2, 28), updatedCaptor.getValue().getEndDate());

        assertEquals("AGR_TECH", savedCaptor.getValue().getAgreementCode());
        assertEquals("CAT_TECH_1", savedCaptor.getValue().getAgreementCategoryCode());
        assertEquals(LocalDate.of(2026, 3, 1), savedCaptor.getValue().getStartDate());
        assertEquals(null, savedCaptor.getValue().getEndDate());
    }

    @Test
    void replaceInsideClosedPeriodPreservesOriginalEndDate() {
        LaborClassification existing = new LaborClassification(
                10L,
                "AGR_OFFICE",
                "CAT_ADMIN",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31)
        );

        // The employee left on the same day the occurrence ends: no gap after it.
        givenEmployeePresent(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), existing);
        when(laborClassificationRepository.findByEmployeeIdAndStartDate(10L, LocalDate.of(2026, 1, 1)))
                .thenReturn(Optional.of(existing));

        LaborClassification replaced = service.replaceFromDate(command(
                LocalDate.of(2026, 3, 1),
                "AGR_TECH",
                "CAT_TECH_1"
        ));

        assertEquals(LocalDate.of(2026, 3, 1), replaced.getStartDate());
        assertEquals(LocalDate.of(2026, 3, 31), replaced.getEndDate());

        ArgumentCaptor<LaborClassification> updatedCaptor = ArgumentCaptor.forClass(LaborClassification.class);
        ArgumentCaptor<LaborClassification> savedCaptor = ArgumentCaptor.forClass(LaborClassification.class);
        verify(laborClassificationRepository).update(updatedCaptor.capture(), any(LocalDate.class));
        verify(laborClassificationRepository).save(savedCaptor.capture());

        assertEquals(LocalDate.of(2026, 2, 28), updatedCaptor.getValue().getEndDate());
        assertEquals(LocalDate.of(2026, 3, 31), savedCaptor.getValue().getEndDate());
    }

    // Was replaceAtExactStartDateUpdatesWithoutDuplicateIdentityRow: the old
    // EXACT_START replaced the codes silently. ADR-057 §6 keeps what the user
    // wanted (no second row on the same start) but says it out loud: the plan
    // is a correction, it is rejected as such, and nothing is written.
    @Test
    void replaceAtExactStartDateIsRejectedAsACorrectionAndWritesNoDuplicateIdentityRow() {
        LaborClassification existing = new LaborClassification(
                10L,
                "AGR_OFFICE",
                "CAT_ADMIN",
                LocalDate.of(2026, 3, 1),
                null
        );

        givenEmployeePresent(LocalDate.of(2026, 3, 1), null, existing);

        LaborClassificationIsACorrectionException ex = assertThrows(
                LaborClassificationIsACorrectionException.class,
                () -> service.replaceFromDate(command(LocalDate.of(2026, 3, 1), "AGR_TECH", "CAT_TECH_1"))
        );

        assertEquals(new LaborClassificationPeriod(LocalDate.of(2026, 3, 1), null), ex.correctedOccurrence());
        verify(laborClassificationRepository, never()).update(any(LaborClassification.class), any(LocalDate.class));
        verify(laborClassificationRepository, never()).save(any(LaborClassification.class));
    }

    @Test
    void rejectsWhenAgreementCategoryRelationIsInvalid() {
        agreementCategoryRelationValidator.setInvalidRelation(true);
        whenEmployeeExists();

        assertThrows(
                LaborClassificationAgreementCategoryRelationInvalidException.class,
                () -> service.replaceFromDate(command(LocalDate.of(2026, 3, 1), "AGR_TECH", "CAT_TECH_1"))
        );
        verify(laborClassificationRepository, never()).save(any(LaborClassification.class));
        verify(laborClassificationRepository, never()).update(any(LaborClassification.class), any(LocalDate.class));
    }

    @Test
    void rejectsWhenEmployeeDoesNotExist() {
        when(employeeLaborClassificationLookupPort.findByBusinessKeyForUpdate(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER
        )).thenReturn(Optional.empty());

        assertThrows(
                LaborClassificationEmployeeNotFoundException.class,
                () -> service.replaceFromDate(command(LocalDate.of(2026, 3, 1), "AGR_TECH", "CAT_TECH_1"))
        );

        verify(laborClassificationRepository, never()).save(any(LaborClassification.class));
        verify(laborClassificationRepository, never()).update(any(LaborClassification.class), any(LocalDate.class));
    }

    @Test
    void rejectsWhenReplacementBreaksPresenceCoverage() {
        LaborClassification closedInJanuary = new LaborClassification(
                10L,
                "AGR_OFFICE",
                "CAT_ADMIN",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        );

        givenEmployeePresent(LocalDate.of(2026, 1, 1), null, closedInJanuary);

        LaborClassificationCoverageIncompleteException ex = assertThrows(
                LaborClassificationCoverageIncompleteException.class,
                () -> service.replaceFromDate(command(LocalDate.of(2026, 3, 1), "AGR_TECH", "CAT_TECH_1"))
        );

        assertEquals(
                List.of(new LaborClassificationPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))),
                ex.gaps()
        );
        verify(laborClassificationRepository, never()).save(any(LaborClassification.class));
        verify(laborClassificationRepository, never()).update(any(LaborClassification.class), any(LocalDate.class));
    }

    @Test
    void rejectsWhenReplacementIsOutsidePresence() {
        LaborClassification existing = new LaborClassification(
                10L,
                "AGR_OFFICE",
                "CAT_ADMIN",
                LocalDate.of(2026, 1, 1),
                null
        );

        givenEmployeePresent(LocalDate.of(2026, 1, 1), null, existing);

        assertThrows(
                LaborClassificationOutsidePresencePeriodException.class,
                () -> service.replaceFromDate(command(LocalDate.of(2025, 12, 1), "AGR_TECH", "CAT_TECH_1"))
        );

        verify(laborClassificationRepository, never()).save(any(LaborClassification.class));
        verify(laborClassificationRepository, never()).update(any(LaborClassification.class), any(LocalDate.class));
    }

    @Test
    void rejectsWhenNoCoveringPeriodAndProjectedTimelineOverlaps() {
        LaborClassification futureOpen = new LaborClassification(
                10L,
                "AGR_OFFICE",
                "CAT_ADMIN",
                LocalDate.of(2026, 4, 1),
                null
        );

        givenEmployeePresent(LocalDate.of(2026, 1, 1), null, futureOpen);

        assertThrows(
                LaborClassificationOverlapException.class,
                () -> service.replaceFromDate(command(LocalDate.of(2026, 3, 1), "AGR_TECH", "CAT_TECH_1"))
        );

        verify(laborClassificationRepository, never()).save(any(LaborClassification.class));
        verify(laborClassificationRepository, never()).update(any(LaborClassification.class), any(LocalDate.class));
    }

    @Test
    void createsNewPeriodWhenNoCoveringAndProjectedTimelineIsValid() {
        givenEmployeePresent(LocalDate.of(2026, 3, 1), null);

        LaborClassification replaced = service.replaceFromDate(command(
                LocalDate.of(2026, 3, 1),
                "AGR_TECH",
                "CAT_TECH_1"
        ));

        assertEquals(LocalDate.of(2026, 3, 1), replaced.getStartDate());
        assertEquals(null, replaced.getEndDate());

        verify(laborClassificationRepository, never()).update(any(LaborClassification.class), any(LocalDate.class));
        verify(laborClassificationRepository).save(any(LaborClassification.class));
    }

    private void givenEmployeePresent(LocalDate presenceStart, LocalDate presenceEnd, LaborClassification... occurrences) {
        whenEmployeeExists();
        when(laborClassificationRepository.findByEmployeeIdOrderByStartDate(10L)).thenReturn(List.of(occurrences));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L))
                .thenReturn(List.of(new PresencePeriod(presenceStart, presenceEnd)));
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

    private ReplaceLaborClassificationFromDateCommand command(
            LocalDate effectiveDate,
            String agreementCode,
            String agreementCategoryCode
    ) {
        return new ReplaceLaborClassificationFromDateCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                effectiveDate,
                agreementCode,
                agreementCategoryCode
        );
    }

    private static final class TestLaborClassificationCatalogValidator extends LaborClassificationCatalogValidator {

        private TestLaborClassificationCatalogValidator() {
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

package com.b4rrhh.employee.temporal.support;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The intention a plan was asked with is part of the plan, and a plan that
 * is not the operation it was asked for cannot even be built as accepted
 * (backend#58). No vertical has to remember to check it: the record does.
 */
class TimelinePlanTest {

    private static final DateRange EXISTING = new DateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30));
    private static final DateRange CORRECTED = new DateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 5, 31));

    @Test
    void aCorrectionAskedForAsAnAddCannotBeBuiltAccepted() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> plan(TimelineOperation.ADD, TimelineOperation.CORRECT, EXISTING, null)
        );

        assertEquals("a plan that is not the operation asked for is rejected as IS_A_CORRECTION", ex.getMessage());
    }

    @Test
    void aCorrectionAskedForAsAnAddIsBuiltRejectedAsACorrection() {
        TimelinePlan plan = plan(TimelineOperation.ADD, TimelineOperation.CORRECT, EXISTING, TimelineRejection.IS_A_CORRECTION);

        assertFalse(plan.isAccepted());
        assertEquals(TimelineOperation.ADD, plan.intent());
        assertEquals(TimelineOperation.CORRECT, plan.operation());
        assertEquals(EXISTING, plan.correctedOccurrence());
    }

    @Test
    void aPlanThatIsTheOperationAskedForCannotClaimToBeACorrectionInstead() {
        assertThrows(
                IllegalArgumentException.class,
                () -> plan(TimelineOperation.CORRECT, TimelineOperation.CORRECT, EXISTING, TimelineRejection.IS_A_CORRECTION)
        );
    }

    @Test
    void theIntentIsRequired() {
        assertThrows(
                IllegalArgumentException.class,
                () -> plan(null, TimelineOperation.CORRECT, EXISTING, null)
        );
    }

    private static TimelinePlan plan(
            TimelineOperation intent,
            TimelineOperation operation,
            DateRange correctedOccurrence,
            TimelineRejection rejection
    ) {
        return new TimelinePlan(
                intent,
                operation,
                CORRECTED,
                correctedOccurrence,
                rejection,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(CORRECTED)
        );
    }
}

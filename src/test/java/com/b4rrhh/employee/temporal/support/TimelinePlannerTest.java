package com.b4rrhh.employee.temporal.support;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The series under test has three occurrences inside an open presence period:
 *
 * <pre>
 *   FIRST   2026-01-01 .. 2026-03-31
 *   SECOND  2026-04-01 .. 2026-06-30
 *   LAST    2026-07-01 .. (open)
 * </pre>
 */
class TimelinePlannerTest {

    private static final DateRange PRESENCE = range(LocalDate.of(2026, 1, 1), null);
    private static final DateRange FIRST = range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
    private static final DateRange SECOND = range(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30));
    private static final DateRange LAST = range(LocalDate.of(2026, 7, 1), null);

    private final TimelinePlanner planner = new TimelinePlanner();

    @Nested
    class AddingAnOccurrence {

        @Test
        void afterTheLastOneClosesItTheDayBefore() {
            DateRange added = range(LocalDate.of(2026, 10, 1), null);

            TimelinePlan plan = planner.planAdd(mandatory(FIRST, SECOND, LAST), added);

            assertTrue(plan.isAccepted());
            assertEquals(TimelineOperation.ADD, plan.intent());
            assertEquals(TimelineOperation.ADD, plan.operation());
            assertEquals(added, plan.occurrence());
            assertNull(plan.correctedOccurrence());
            assertEquals(
                    new OccurrenceAdjustment(LAST, range(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30))),
                    plan.adjustedOccurrence()
            );
            assertTrue(plan.gaps().isEmpty());
            assertEquals(
                    List.of(FIRST, SECOND, range(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30)), added),
                    plan.projected()
            );
        }

        @Test
        void afterAClosedLastOneClosesNothing() {
            DateRange closedLast = range(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30));
            DateRange added = range(LocalDate.of(2026, 10, 1), null);

            TimelinePlan plan = planner.planAdd(mandatory(FIRST, SECOND, closedLast), added);

            assertTrue(plan.isAccepted());
            assertNull(plan.adjustedOccurrence());
            assertEquals(List.of(FIRST, SECOND, closedLast, added), plan.projected());
        }

        @Test
        void inTheMiddleSplitsTheCoveringOccurrence() {
            DateRange added = range(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 30));

            TimelinePlan plan = planner.planAdd(mandatory(FIRST, SECOND, LAST), added);

            assertTrue(plan.isAccepted());
            assertEquals(
                    new OccurrenceAdjustment(SECOND, range(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30))),
                    plan.adjustedOccurrence()
            );
            assertTrue(plan.gaps().isEmpty());
            assertEquals(
                    List.of(FIRST, range(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)), added, LAST),
                    plan.projected()
            );
        }

        @Test
        void inTheMiddleLeavingAGapIsRejectedWhenCoverageIsMandatory() {
            DateRange added = range(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 15));

            TimelinePlan plan = planner.planAdd(mandatory(FIRST, SECOND, LAST), added);

            assertFalse(plan.isAccepted());
            assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
            assertEquals(List.of(range(LocalDate.of(2026, 5, 16), LocalDate.of(2026, 6, 30))), plan.gaps());
            assertEquals(List.of(added, LAST), plan.stretchCandidates());
        }

        @Test
        void inTheMiddleLeavingAGapIsAcceptedWhenCoverageIsOptional() {
            DateRange added = range(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 15));

            TimelinePlan plan = planner.planAdd(optional(FIRST, SECOND, LAST), added);

            assertTrue(plan.isAccepted());
            assertEquals(List.of(range(LocalDate.of(2026, 5, 16), LocalDate.of(2026, 6, 30))), plan.gaps());
            assertEquals(
                    new OccurrenceAdjustment(SECOND, range(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30))),
                    plan.adjustedOccurrence()
            );
        }

        @Test
        void outsideThePresenceIsRejected() {
            DateRange added = range(LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 31));

            TimelinePlan plan = planner.planAdd(mandatory(FIRST, SECOND, LAST), added);

            assertFalse(plan.isAccepted());
            assertEquals(TimelineRejection.OUTSIDE_PRESENCE, plan.rejection());
            assertNull(plan.adjustedOccurrence());
        }

        @Test
        void reachingBeyondTheNextOccurrenceIsAnOverlapAndNothingElseMoves() {
            DateRange added = range(LocalDate.of(2026, 5, 1), null);

            TimelinePlan plan = planner.planAdd(mandatory(FIRST, SECOND, LAST), added);

            assertFalse(plan.isAccepted());
            assertEquals(TimelineRejection.OVERLAP, plan.rejection());
            assertEquals(List.of(LAST), plan.overlaps());
        }

        @Test
        void beforeTheFirstOneWithoutTouchingItIsAccepted() {
            DateRange earlyPresence = range(LocalDate.of(2025, 1, 1), null);
            DateRange added = range(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 12, 31));

            TimelinePlan plan = planner.planAdd(
                    new Timeline(TimelineCoverage.OPTIONAL, List.of(earlyPresence), List.of(FIRST, SECOND, LAST)),
                    added
            );

            assertTrue(plan.isAccepted());
            assertNull(plan.adjustedOccurrence());
            assertEquals(List.of(added, FIRST, SECOND, LAST), plan.projected());
        }

        @Test
        void intoAnEmptySeriesCreatesTheFirstOccurrence() {
            DateRange added = range(LocalDate.of(2026, 1, 1), null);

            TimelinePlan plan = planner.planAdd(mandatory(), added);

            assertTrue(plan.isAccepted());
            assertEquals(List.of(added), plan.projected());
        }
    }

    /**
     * An occurrence is identified by the day it starts. Adding one that starts
     * on that very day is not an add: it is the correction of the existing
     * one. The plan says so and names it, and because it is not the operation
     * that was asked for it comes back rejected: nobody applies a correction
     * they asked for as an add (backend#58).
     */
    @Nested
    class AddingOnTheStartDateOfAnExistingOccurrence {

        @Test
        void withADifferentEndIsRejectedAsTheCorrectionOfThatOccurrenceNotAnAdd() {
            DateRange added = range(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 31));

            TimelinePlan plan = planner.planAdd(optional(FIRST, SECOND, LAST), added);

            assertFalse(plan.isAccepted());
            assertEquals(TimelineRejection.IS_A_CORRECTION, plan.rejection());
            assertEquals(TimelineOperation.ADD, plan.intent());
            assertEquals(TimelineOperation.CORRECT, plan.operation());
            assertEquals(SECOND, plan.correctedOccurrence());
            assertEquals(added, plan.occurrence());
            assertNull(plan.adjustedOccurrence());
            assertEquals(List.of(range(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30))), plan.gaps());
            assertEquals(List.of(FIRST, added, LAST), plan.projected());
        }

        @Test
        void showsWhatTheCorrectionWouldRunIntoEvenThoughBeingACorrectionIsWhatRejectsIt() {
            Timeline timeline = mandatory(FIRST, SECOND, LAST);
            DateRange added = range(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 31));

            TimelinePlan plan = planner.planAdd(timeline, added);
            TimelinePlan correction = planner.planCorrect(timeline, SECOND, added);

            assertEquals(TimelineRejection.IS_A_CORRECTION, plan.rejection());
            assertEquals(TimelineRejection.GAP_NOT_ALLOWED, correction.rejection());
            assertEquals(correction.gaps(), plan.gaps());
            assertEquals(correction.stretchCandidates(), plan.stretchCandidates());
            assertEquals(correction.projected(), plan.projected());
            assertEquals(correction.correctedOccurrence(), plan.correctedOccurrence());
        }

        @Test
        void ofTheOpenLastOneIsRejectedAsItsCorrection() {
            DateRange closedPresence = range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
            DateRange added = range(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31));

            TimelinePlan plan = planner.planAdd(
                    new Timeline(TimelineCoverage.MANDATORY, List.of(closedPresence), List.of(FIRST, SECOND, LAST)),
                    added
            );

            assertFalse(plan.isAccepted());
            assertEquals(TimelineRejection.IS_A_CORRECTION, plan.rejection());
            assertEquals(TimelineOperation.CORRECT, plan.operation());
            assertEquals(LAST, plan.correctedOccurrence());
            assertNull(plan.adjustedOccurrence());
            assertTrue(plan.gaps().isEmpty());
            assertEquals(List.of(FIRST, SECOND, added), plan.projected());
        }

        @Test
        void askedForAsACorrectionTheSameDatesAreAccepted() {
            DateRange corrected = range(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 31));

            TimelinePlan plan = planner.planCorrect(optional(FIRST, SECOND, LAST), SECOND, corrected);

            assertTrue(plan.isAccepted());
            assertEquals(TimelineOperation.CORRECT, plan.intent());
            assertEquals(TimelineOperation.CORRECT, plan.operation());
            assertEquals(SECOND, plan.correctedOccurrence());
        }

        @Test
        void oneDayLaterIsStillAnAddThatClosesTheDayBefore() {
            DateRange added = range(LocalDate.of(2026, 7, 2), null);

            TimelinePlan plan = planner.planAdd(mandatory(FIRST, SECOND, LAST), added);

            assertTrue(plan.isAccepted());
            assertEquals(TimelineOperation.ADD, plan.intent());
            assertEquals(TimelineOperation.ADD, plan.operation());
            assertNull(plan.correctedOccurrence());
            assertEquals(
                    new OccurrenceAdjustment(LAST, range(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1))),
                    plan.adjustedOccurrence()
            );
            assertEquals(
                    List.of(FIRST, SECOND, range(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1)), added),
                    plan.projected()
            );
        }

        @Test
        void oneDayEarlierIsStillAnAddJudgedAsSuch() {
            DateRange added = range(LocalDate.of(2026, 6, 30), null);

            TimelinePlan plan = planner.planAdd(mandatory(FIRST, SECOND, LAST), added);

            assertFalse(plan.isAccepted());
            assertEquals(TimelineRejection.OVERLAP, plan.rejection());
            assertEquals(TimelineOperation.ADD, plan.operation());
            assertNull(plan.correctedOccurrence());
            assertEquals(
                    new OccurrenceAdjustment(SECOND, range(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 29))),
                    plan.adjustedOccurrence()
            );
            assertEquals(List.of(LAST), plan.overlaps());
        }
    }

    @Nested
    class RemovingAnOccurrence {

        @Test
        void theLastOneReopensThePreviousOne() {
            TimelinePlan plan = planner.planRemove(mandatory(FIRST, SECOND, LAST), LAST);

            assertTrue(plan.isAccepted());
            assertEquals(TimelineOperation.REMOVE, plan.intent());
            assertEquals(TimelineOperation.REMOVE, plan.operation());
            assertEquals(LAST, plan.occurrence());
            assertNull(plan.correctedOccurrence());
            assertEquals(
                    new OccurrenceAdjustment(SECOND, range(LocalDate.of(2026, 4, 1), null)),
                    plan.adjustedOccurrence()
            );
            assertTrue(plan.gaps().isEmpty());
            assertEquals(List.of(FIRST, range(LocalDate.of(2026, 4, 1), null)), plan.projected());
        }

        @Test
        void aClosedLastOneHandsItsEndDateToThePreviousOne() {
            DateRange closedPresence = range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
            DateRange closedLast = range(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31));

            TimelinePlan plan = planner.planRemove(
                    new Timeline(TimelineCoverage.MANDATORY, List.of(closedPresence), List.of(FIRST, SECOND, closedLast)),
                    closedLast
            );

            assertTrue(plan.isAccepted());
            assertEquals(
                    new OccurrenceAdjustment(SECOND, range(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 12, 31))),
                    plan.adjustedOccurrence()
            );
        }

        @Test
        void theLastOneDoesNotStretchAPreviousOneThatWasNotAdjacent() {
            DateRange detachedLast = range(LocalDate.of(2026, 9, 1), null);

            TimelinePlan plan = planner.planRemove(optional(FIRST, SECOND, detachedLast), detachedLast);

            assertTrue(plan.isAccepted());
            assertNull(plan.adjustedOccurrence());
            assertEquals(List.of(range(LocalDate.of(2026, 7, 1), null)), plan.gaps());
            assertEquals(List.of(FIRST, SECOND), plan.projected());
        }

        @Test
        void oneInTheMiddleIsRejectedWhenCoverageIsMandatoryAndNamesTheNeighboursToStretch() {
            TimelinePlan plan = planner.planRemove(mandatory(FIRST, SECOND, LAST), SECOND);

            assertFalse(plan.isAccepted());
            assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
            assertNull(plan.adjustedOccurrence());
            assertEquals(List.of(SECOND), plan.gaps());
            assertEquals(List.of(FIRST, LAST), plan.stretchCandidates());
            assertEquals(List.of(FIRST, LAST), plan.projected());
        }

        @Test
        void oneInTheMiddleLeavesAGapWhenCoverageIsOptional() {
            TimelinePlan plan = planner.planRemove(optional(FIRST, SECOND, LAST), SECOND);

            assertTrue(plan.isAccepted());
            assertNull(plan.adjustedOccurrence());
            assertEquals(List.of(SECOND), plan.gaps());
            assertEquals(List.of(FIRST, LAST), plan.stretchCandidates());
            assertEquals(List.of(FIRST, LAST), plan.projected());
        }

        @Test
        void theOnlyOneIsRejectedWhenCoverageIsMandatory() {
            DateRange only = range(LocalDate.of(2026, 1, 1), null);

            TimelinePlan plan = planner.planRemove(mandatory(only), only);

            assertFalse(plan.isAccepted());
            assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
            assertEquals(List.of(PRESENCE), plan.gaps());
            assertTrue(plan.stretchCandidates().isEmpty());
        }

        @Test
        void anOccurrenceThatIsNotInTheSeriesIsAProgrammingError() {
            DateRange stranger = range(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> planner.planRemove(mandatory(FIRST, SECOND, LAST), stranger)
            );
        }
    }

    @Nested
    class CorrectingAnOccurrence {

        @Test
        void stretchingOneOverAGapCoversItAndMovesNothingElse() {
            DateRange shortSecond = range(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 31));
            DateRange corrected = range(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30));

            TimelinePlan plan = planner.planCorrect(mandatory(FIRST, shortSecond, LAST), shortSecond, corrected);

            assertTrue(plan.isAccepted());
            assertEquals(TimelineOperation.CORRECT, plan.intent());
            assertEquals(TimelineOperation.CORRECT, plan.operation());
            assertEquals(corrected, plan.occurrence());
            assertEquals(shortSecond, plan.correctedOccurrence());
            assertNull(plan.adjustedOccurrence());
            assertTrue(plan.gaps().isEmpty());
            assertEquals(List.of(FIRST, corrected, LAST), plan.projected());
        }

        @Test
        void movingTheStartLaterLeavesAGapAndNamesThePreviousOneToStretch() {
            DateRange corrected = range(LocalDate.of(2026, 4, 15), LocalDate.of(2026, 6, 30));

            TimelinePlan plan = planner.planCorrect(mandatory(FIRST, SECOND, LAST), SECOND, corrected);

            assertFalse(plan.isAccepted());
            assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
            assertEquals(List.of(range(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 14))), plan.gaps());
            assertEquals(List.of(FIRST, corrected), plan.stretchCandidates());
        }

        @Test
        void movingTheStartLaterIsAcceptedWhenCoverageIsOptional() {
            DateRange corrected = range(LocalDate.of(2026, 4, 15), LocalDate.of(2026, 6, 30));

            TimelinePlan plan = planner.planCorrect(optional(FIRST, SECOND, LAST), SECOND, corrected);

            assertTrue(plan.isAccepted());
            assertEquals(List.of(range(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 14))), plan.gaps());
        }

        @Test
        void reachingIntoTheNextOneIsAnOverlapAndNothingElseMoves() {
            DateRange corrected = range(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 7, 15));

            TimelinePlan plan = planner.planCorrect(mandatory(FIRST, SECOND, LAST), SECOND, corrected);

            assertFalse(plan.isAccepted());
            assertEquals(TimelineRejection.OVERLAP, plan.rejection());
            assertEquals(List.of(range(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 15))), plan.overlaps());
            assertNull(plan.adjustedOccurrence());
            assertEquals(List.of(FIRST, corrected, LAST), plan.projected());
        }

        @Test
        void reopeningTheLastOneIsAccepted() {
            DateRange closedLast = range(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30));

            TimelinePlan plan = planner.planCorrect(mandatory(FIRST, SECOND, closedLast), closedLast, LAST);

            assertTrue(plan.isAccepted());
            assertEquals(List.of(FIRST, SECOND, LAST), plan.projected());
        }

        @Test
        void outsideThePresenceIsRejected() {
            DateRange corrected = range(LocalDate.of(2025, 12, 1), LocalDate.of(2026, 3, 31));

            TimelinePlan plan = planner.planCorrect(mandatory(FIRST, SECOND, LAST), FIRST, corrected);

            assertFalse(plan.isAccepted());
            assertEquals(TimelineRejection.OUTSIDE_PRESENCE, plan.rejection());
        }

        @Test
        void leavingTheDatesAsTheyAreIsAccepted() {
            TimelinePlan plan = planner.planCorrect(mandatory(FIRST, SECOND, LAST), SECOND, SECOND);

            assertTrue(plan.isAccepted());
            assertEquals(List.of(FIRST, SECOND, LAST), plan.projected());
        }

        @Test
        void anOccurrenceThatIsNotInTheSeriesIsAProgrammingError() {
            DateRange stranger = range(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> planner.planCorrect(mandatory(FIRST, SECOND, LAST), stranger, stranger)
            );
        }
    }

    /**
     * A series of facts of the person, not of the employment (backend#53): an
     * address does not expire because the employee leaves. It declares that it
     * may outlive the presence, so being outside it is never a fault, while a
     * mandatory coverage still demands that the presence itself be covered.
     */
    @Nested
    class ASeriesThatMayOutliveThePresence {

        private static final DateRange CLOSED_PRESENCE = range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));

        @Test
        void addingBeforeThePresenceStartsIsAccepted() {
            DateRange added = range(LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 31));

            TimelinePlan plan = planner.planAdd(mandatoryOutliving(FIRST, SECOND, LAST), added);

            assertTrue(plan.isAccepted());
            assertNull(plan.rejection());
            assertEquals(List.of(added, FIRST, SECOND, LAST), plan.projected());
        }

        @Test
        void addingAfterTheEmployeeLeftClosesTheOpenOneAndIsAccepted() {
            DateRange added = range(LocalDate.of(2026, 9, 1), null);

            TimelinePlan plan = planner.planAdd(
                    new Timeline(
                            TimelineCoverage.MANDATORY,
                            TimelineContainment.MAY_OUTLIVE_PRESENCE,
                            List.of(CLOSED_PRESENCE),
                            List.of(FIRST, SECOND, LAST)
                    ),
                    added
            );

            assertTrue(plan.isAccepted());
            assertEquals(
                    new OccurrenceAdjustment(LAST, range(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31))),
                    plan.adjustedOccurrence()
            );
        }

        @Test
        void correctingAnOccurrenceThatOutlivesTheTerminationIsAccepted() {
            DateRange stretched = range(LocalDate.of(2026, 7, 1), LocalDate.of(2027, 12, 31));

            TimelinePlan plan = planner.planCorrect(
                    new Timeline(
                            TimelineCoverage.MANDATORY,
                            TimelineContainment.MAY_OUTLIVE_PRESENCE,
                            List.of(CLOSED_PRESENCE),
                            List.of(FIRST, SECOND, LAST)
                    ),
                    LAST,
                    stretched
            );

            assertTrue(plan.isAccepted());
            assertEquals(List.of(FIRST, SECOND, stretched), plan.projected());
        }

        @Test
        void aGapInsideThePresenceIsStillRejectedWhenCoverageIsMandatory() {
            DateRange added = range(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 15));

            TimelinePlan plan = planner.planAdd(mandatoryOutliving(FIRST, SECOND, LAST), added);

            assertFalse(plan.isAccepted());
            assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
            assertEquals(List.of(range(LocalDate.of(2026, 5, 16), LocalDate.of(2026, 6, 30))), plan.gaps());
        }

        @Test
        void anOverlapIsStillRejected() {
            DateRange added = range(LocalDate.of(2025, 12, 1), LocalDate.of(2026, 1, 15));

            TimelinePlan plan = planner.planAdd(mandatoryOutliving(FIRST, SECOND, LAST), added);

            assertFalse(plan.isAccepted());
            assertEquals(TimelineRejection.OVERLAP, plan.rejection());
        }

        @Test
        void theThreeArgumentTimelineStaysWithinThePresence() {
            assertEquals(TimelineContainment.WITHIN_PRESENCE, mandatory(FIRST).containment());
        }

        private static Timeline mandatoryOutliving(DateRange... occurrences) {
            return new Timeline(
                    TimelineCoverage.MANDATORY,
                    TimelineContainment.MAY_OUTLIVE_PRESENCE,
                    List.of(PRESENCE),
                    List.of(occurrences)
            );
        }
    }

    @Nested
    class PlanningWithoutApplying {

        @Test
        void planningLeavesTheTimelineUntouchedAndIsRepeatable() {
            List<DateRange> occurrences = new ArrayList<>(List.of(FIRST, SECOND, LAST));
            Timeline timeline = new Timeline(TimelineCoverage.MANDATORY, List.of(PRESENCE), occurrences);
            DateRange added = range(LocalDate.of(2026, 10, 1), null);

            TimelinePlan firstPlan = planner.planAdd(timeline, added);
            TimelinePlan secondPlan = planner.planAdd(timeline, added);

            assertEquals(List.of(FIRST, SECOND, LAST), timeline.occurrences());
            assertEquals(List.of(FIRST, SECOND, LAST), occurrences);
            assertEquals(firstPlan, secondPlan);
            assertEquals(4, firstPlan.projected().size());
        }

        @Test
        void aRemovalPlanCanBeInspectedBeforeAnythingIsRemoved() {
            Timeline timeline = mandatory(FIRST, SECOND, LAST);

            TimelinePlan plan = planner.planRemove(timeline, LAST);

            assertEquals(List.of(FIRST, SECOND, LAST), timeline.occurrences());
            assertEquals(List.of(FIRST, range(LocalDate.of(2026, 4, 1), null)), plan.projected());
        }
    }

    @Nested
    class OrderIsDerivedFromTheStartDate {

        @Test
        void theTimelineSortsWhateverOrderItIsGiven() {
            Timeline timeline = new Timeline(
                    TimelineCoverage.MANDATORY,
                    List.of(PRESENCE),
                    List.of(LAST, FIRST, SECOND)
            );

            assertEquals(List.of(FIRST, SECOND, LAST), timeline.occurrences());
        }

        @Test
        void theLastOccurrenceIsTheOneWithTheLatestStartDateNotTheLastGiven() {
            Timeline timeline = new Timeline(
                    TimelineCoverage.MANDATORY,
                    List.of(PRESENCE),
                    List.of(LAST, FIRST, SECOND)
            );

            TimelinePlan plan = planner.planAdd(timeline, range(LocalDate.of(2026, 10, 1), null));

            assertEquals(LAST, plan.adjustedOccurrence().before());
        }
    }

    private static Timeline mandatory(DateRange... occurrences) {
        return new Timeline(TimelineCoverage.MANDATORY, List.of(PRESENCE), List.of(occurrences));
    }

    private static Timeline optional(DateRange... occurrences) {
        return new Timeline(TimelineCoverage.OPTIONAL, List.of(PRESENCE), List.of(occurrences));
    }

    private static DateRange range(LocalDate startDate, LocalDate endDate) {
        return new DateRange(startDate, endDate);
    }
}

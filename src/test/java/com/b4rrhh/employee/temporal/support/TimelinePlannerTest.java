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

    /**
     * The move (backend#70). An employee is hired on 2026-01-01 and moves
     * house on 2026-05-31: the domicile is two addresses that are the two
     * halves of one move, and the API writes one occurrence at a time.
     *
     * <pre>
     *   HEAD  2026-01-01 .. 2026-05-31
     *   TAIL  2026-06-01 .. (open)
     * </pre>
     *
     * <p>Neither half covers the presence on its own: the head leaves its
     * tail uncovered and the tail leaves its head. Judging each write as if
     * it had to be the last one rejected both, in both orders, so the only
     * reachable state was the one the invariant forbids — no address at all.
     * That is what the loader's run measured: 220 rejected addresses over the
     * 110 employees who moved, 81 of them left with nothing.
     *
     * <p>Written in the order the move happened, both go in now. The other
     * order does not, and that asymmetry is the decision, not a leftover: see
     * {@link #areRejectedTheOtherWayRoundNamingTheStretchFromTheHire}.
     */
    @Nested
    class TheTwoHalvesOfAMove {

        private static final DateRange HIRED = range(LocalDate.of(2026, 1, 1), null);
        private static final DateRange HEAD = range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 31));
        private static final DateRange TAIL = range(LocalDate.of(2026, 6, 1), null);

        @Test
        void goInChronologicallyOneAtATime() {
            TimelinePlan first = planner.planAdd(domicileOf(), HEAD);
            assertTrue(first.isAccepted(), "the head of the move is turned away: " + first.rejection());

            TimelinePlan second = planner.planAdd(domicileOf(HEAD), TAIL);
            assertTrue(second.isAccepted(), "the tail of the move is turned away: " + second.rejection());
            assertEquals(List.of(HEAD, TAIL), second.projected());
            assertTrue(second.gaps().isEmpty());
        }

        /**
         * The other way round is still rejected, and on purpose. The tail
         * alone says the employee had no address between being hired and
         * moving, and that is a claim about a past that is already over, not
         * a question still open. So one order works and the other is told
         * why: the error names the stretch from the hire and the occurrence
         * to stretch back.
         */
        @Test
        void areRejectedTheOtherWayRoundNamingTheStretchFromTheHire() {
            TimelinePlan plan = planner.planAdd(domicileOf(), TAIL);

            assertFalse(plan.isAccepted());
            assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
            assertEquals(List.of(range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 31))), plan.gaps());
            assertEquals(List.of(TAIL), plan.stretchCandidates());
        }

        /**
         * The head is accepted and the plan still says what is missing. The
         * screen needs it to ask for the other half; hiding it would trade
         * one silence for another.
         */
        @Test
        void leaveTheHeadAcceptedWhileStillNamingWhatIsLeftUncovered() {
            TimelinePlan head = planner.planAdd(domicileOf(), HEAD);

            assertTrue(head.isAccepted());
            assertEquals(List.of(range(LocalDate.of(2026, 6, 1), null)), head.gaps());
            assertEquals(List.of(HEAD), head.stretchCandidates());
        }

        /**
         * The other side of the rule, and the reason it is not just «gaps are
         * fine now»: what a change opens is still rejected. Closing the only
         * address of a present employee uncovers a stretch that was covered a
         * moment ago.
         *
         * <p>The plan reports the head gap too, the one it found and did not
         * cause. It is the rejection that is narrow, not the report: the
         * screen shows the whole timeline and the user sees both.
         */
        @Test
        void doNotMakeItLegalToCloseTheOnlyAddressOfAPresentEmployee() {
            TimelinePlan plan = planner.planCorrect(domicileOf(TAIL), TAIL, range(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31)));

            assertFalse(plan.isAccepted());
            assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
            assertEquals(
                    List.of(
                            range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 31)),
                            range(LocalDate.of(2026, 9, 1), null)
                    ),
                    plan.gaps()
            );
        }

        /** And on a series that was whole, the same close is rejected on its own. */
        @Test
        void doNotMakeItLegalToCloseTheLastAddressOfAWholeSeriesEither() {
            TimelinePlan plan = planner.planCorrect(
                    domicileOf(HEAD, TAIL),
                    TAIL,
                    range(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31))
            );

            assertFalse(plan.isAccepted());
            assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
            assertEquals(List.of(range(LocalDate.of(2026, 9, 1), null)), plan.gaps());
        }

        /** Nor to take away the half that was already there. */
        @Test
        void doNotMakeItLegalToRemoveOneHalfOnceBothAreIn() {
            TimelinePlan plan = planner.planRemove(domicileOf(HEAD, TAIL), HEAD);

            assertFalse(plan.isAccepted());
            assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
        }

        /**
         * And the line the leeway does not cross: a hole between two
         * occurrences is rejected whoever left it there, even on a series
         * that was already behind. The edges of the presence are where
         * «nobody has told me yet» is a real state; between two occurrences
         * the series is making a claim about the past.
         */
        @Test
        void doNotMakeItLegalToLeaveAHoleBetweenTwoAddresses() {
            DateRange early = range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
            DateRange late = range(LocalDate.of(2026, 3, 1), null);

            TimelinePlan plan = planner.planAdd(domicileOf(early), late);

            assertFalse(plan.isAccepted());
            assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
            assertEquals(List.of(range(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))), plan.gaps());
            assertEquals(List.of(early, late), plan.stretchCandidates());
        }

        /**
         * A change that swallows an occurrence between two stretches it found
         * uncovered opens a gap, however it looks: what separated them was
         * covered.
         */
        @Test
        void doNotMakeItLegalToJoinTwoGapsTheChangeDidNotOpen() {
            DateRange middle = range(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

            TimelinePlan plan = planner.planRemove(domicile(HIRED, middle), middle);

            assertFalse(plan.isAccepted());
            assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
        }

        /** The domicile: mandatory coverage, and it may outlive the presence (backend#53). */
        private static Timeline domicileOf(DateRange... occurrences) {
            return domicile(HIRED, occurrences);
        }

        private static Timeline domicile(DateRange presence, DateRange... occurrences) {
            return new Timeline(
                    TimelineCoverage.MANDATORY,
                    TimelineContainment.MAY_OUTLIVE_PRESENCE,
                    List.of(presence),
                    List.of(occurrences)
            );
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

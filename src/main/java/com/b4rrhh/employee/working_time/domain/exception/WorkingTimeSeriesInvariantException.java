package com.b4rrhh.employee.working_time.domain.exception;

/**
 * A violation of an invariant of the working time series (ADR-057): the state
 * the request would leave behind is not a valid state of the series. Nothing
 * is applied.
 *
 * <p>It exists so that the family can be named once. The lifecycle flows that
 * create a working time translate these into their own validation error, and
 * before this supertype they did it by listing the members one by one — a list
 * that has to be remembered, and that was already out of date the day
 * {@link WorkingTimeIsACorrectionException} appeared (backend#58, backend#59).
 * Catching this instead means a new invariant needs no edit anywhere else.
 *
 * <p>It is {@code sealed} on purpose: the family is closed, the roster is
 * visible in one place, and a switch over it is exhaustive with the compiler's
 * help.
 *
 * <p>What is <em>not</em> here belongs to another family and must stay out:
 * not found, already closed and number conflict are not statements about the
 * shape of the series.
 */
public abstract sealed class WorkingTimeSeriesInvariantException extends RuntimeException
        permits WorkingTimeCoverageGapException,
                WorkingTimeIsACorrectionException,
                WorkingTimeOutsidePresencePeriodException,
                WorkingTimeOverlapException {

    protected WorkingTimeSeriesInvariantException(String message) {
        super(message);
    }
}

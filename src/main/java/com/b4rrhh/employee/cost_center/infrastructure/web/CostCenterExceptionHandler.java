package com.b4rrhh.employee.cost_center.infrastructure.web;

import com.b4rrhh.employee.cost_center.domain.exception.CostCenterCatalogValueInvalidException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionConflictException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionCoverageGapException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionInvalidException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionIsACorrectionException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionNotFoundException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionOverlapException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionPercentageExceededException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionStartDateMismatchException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterEmployeeNotFoundException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterOutsidePresencePeriodException;
import com.b4rrhh.employee.cost_center.domain.exception.InvalidAllocationPercentageException;
import com.b4rrhh.employee.cost_center.domain.exception.InvalidCostCenterDateRangeException;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionPeriod;
import com.b4rrhh.employee.cost_center.infrastructure.web.dto.CostCenterDistributionPeriodResponse;
import com.b4rrhh.employee.cost_center.infrastructure.web.dto.CostCenterErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

/**
 * Every error carries a {@code code} the screen maps and, when a plan
 * rejection has something to name (ADR-057), the {@code details} it names.
 * The statuses are the ones this API always answered with.
 */
@RestControllerAdvice(assignableTypes = CostCenterBusinessKeyController.class)
public class CostCenterExceptionHandler {

    @ExceptionHandler({
            CostCenterEmployeeNotFoundException.class,
            CostCenterDistributionNotFoundException.class
    })
    public ResponseEntity<CostCenterErrorResponse> handleNotFound(RuntimeException ex) {
        String code = ex instanceof CostCenterDistributionNotFoundException
                ? "COST_CENTER_DISTRIBUTION_NOT_FOUND"
                : "COST_CENTER_EMPLOYEE_NOT_FOUND";

        return respond(HttpStatus.NOT_FOUND, code, ex, null);
    }

    @ExceptionHandler({
            CostCenterCatalogValueInvalidException.class,
            InvalidCostCenterDateRangeException.class,
            InvalidAllocationPercentageException.class,
            CostCenterDistributionInvalidException.class,
            CostCenterDistributionPercentageExceededException.class,
            CostCenterDistributionStartDateMismatchException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<CostCenterErrorResponse> handleBadRequest(RuntimeException ex) {
        if (ex instanceof CostCenterCatalogValueInvalidException) {
            return respond(HttpStatus.BAD_REQUEST, "COST_CENTER_CATALOG_NOT_FOUND", ex, Map.of("field", "costCenterCode"));
        }

        return respond(HttpStatus.BAD_REQUEST, "COST_CENTER_INVALID_WINDOW", ex, null);
    }

    /**
     * A plan rejection says what it ran into: the shared dates, the gap and
     * the neighbours to stretch, or the window an add would correct instead
     * of adding a second one.
     */
    @ExceptionHandler({
            CostCenterDistributionConflictException.class,
            CostCenterDistributionIsACorrectionException.class,
            CostCenterDistributionOverlapException.class,
            CostCenterOutsidePresencePeriodException.class,
            CostCenterDistributionCoverageGapException.class
    })
    public ResponseEntity<CostCenterErrorResponse> handleConflict(RuntimeException ex) {
        if (ex instanceof CostCenterDistributionIsACorrectionException correction) {
            return respond(
                    HttpStatus.CONFLICT,
                    "COST_CENTER_IS_A_CORRECTION",
                    ex,
                    Map.of("correctedOccurrence", period(correction.correctedOccurrence()))
            );
        }
        if (ex instanceof CostCenterDistributionOverlapException overlap) {
            return respond(
                    HttpStatus.CONFLICT,
                    "COST_CENTER_OVERLAP",
                    ex,
                    overlap.overlaps().isEmpty() ? null : Map.of("overlaps", periods(overlap.overlaps()))
            );
        }
        if (ex instanceof CostCenterDistributionCoverageGapException gap) {
            return respond(
                    HttpStatus.CONFLICT,
                    "COST_CENTER_COVERAGE_GAP",
                    ex,
                    Map.of("gaps", periods(gap.gaps()), "stretchCandidates", periods(gap.stretchCandidates()))
            );
        }
        if (ex instanceof CostCenterOutsidePresencePeriodException) {
            return respond(HttpStatus.CONFLICT, "COST_CENTER_OUTSIDE_PRESENCE", ex, null);
        }

        return respond(HttpStatus.CONFLICT, "COST_CENTER_DISTRIBUTION_CONFLICT", ex, null);
    }

    private static List<CostCenterDistributionPeriodResponse> periods(List<CostCenterDistributionPeriod> periods) {
        return periods.stream().map(CostCenterExceptionHandler::period).toList();
    }

    private static CostCenterDistributionPeriodResponse period(CostCenterDistributionPeriod period) {
        return new CostCenterDistributionPeriodResponse(period.startDate(), period.endDate());
    }

    private static ResponseEntity<CostCenterErrorResponse> respond(
            HttpStatus status,
            String code,
            RuntimeException ex,
            Map<String, Object> details
    ) {
        return ResponseEntity.status(status).body(new CostCenterErrorResponse(code, ex.getMessage(), details));
    }
}

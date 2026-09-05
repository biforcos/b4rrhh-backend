package com.b4rrhh.employee.contract.infrastructure.rest;

import com.b4rrhh.employee.contract.domain.exception.ContractAlreadyClosedException;
import com.b4rrhh.employee.contract.domain.exception.ContractCoverageIncompleteException;
import com.b4rrhh.employee.contract.domain.exception.ContractEmployeeNotFoundException;
import com.b4rrhh.employee.contract.domain.exception.ContractInvalidException;
import com.b4rrhh.employee.contract.domain.exception.ContractIsACorrectionException;
import com.b4rrhh.employee.contract.domain.exception.ContractNotFoundException;
import com.b4rrhh.employee.contract.domain.exception.ContractOutsidePresencePeriodException;
import com.b4rrhh.employee.contract.domain.exception.ContractOverlapException;
import com.b4rrhh.employee.contract.domain.exception.ContractSubtypeInvalidException;
import com.b4rrhh.employee.contract.domain.exception.ContractSubtypeRelationInvalidException;
import com.b4rrhh.employee.contract.domain.exception.InvalidContractDateRangeException;
import com.b4rrhh.employee.contract.domain.model.ContractPeriod;
import com.b4rrhh.employee.contract.infrastructure.rest.dto.ContractErrorResponse;
import com.b4rrhh.employee.contract.infrastructure.rest.dto.ContractPeriodResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice(assignableTypes = ContractController.class)
public class ContractExceptionHandler {

    @ExceptionHandler({
            ContractEmployeeNotFoundException.class,
            ContractNotFoundException.class
    })
    public ResponseEntity<ContractErrorResponse> handleNotFound(RuntimeException ex) {
        String code = ex instanceof ContractNotFoundException
                ? "CONTRACT_NOT_FOUND"
                : "CONTRACT_EMPLOYEE_NOT_FOUND";

        return respond(HttpStatus.NOT_FOUND, code, ex.getMessage(), null);
    }

    @ExceptionHandler({
            ContractInvalidException.class,
            ContractSubtypeInvalidException.class,
            ContractSubtypeRelationInvalidException.class,
            InvalidContractDateRangeException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ContractErrorResponse> handleBadRequest(RuntimeException ex) {
        return respond(HttpStatus.BAD_REQUEST, "CONTRACT_INVALID_REQUEST", ex.getMessage(), null);
    }

    /**
     * A plan rejection says what it ran into (ADR-057): the shared dates, the
     * gap and the neighbours to stretch, or the contract an add would correct
     * instead of adding a second one.
     */
    @ExceptionHandler({
            ContractOverlapException.class,
            ContractOutsidePresencePeriodException.class,
            ContractCoverageIncompleteException.class,
            ContractIsACorrectionException.class,
            ContractAlreadyClosedException.class
    })
    public ResponseEntity<ContractErrorResponse> handleConflict(RuntimeException ex) {
        if (ex instanceof ContractIsACorrectionException correction) {
            return respond(
                    HttpStatus.CONFLICT,
                    "CONTRACT_IS_A_CORRECTION",
                    ex.getMessage(),
                    Map.of("correctedOccurrence", toPeriod(correction.correctedOccurrence()))
            );
        }
        if (ex instanceof ContractOverlapException overlap) {
            return respond(
                    HttpStatus.CONFLICT,
                    "CONTRACT_OVERLAP",
                    ex.getMessage(),
                    overlap.overlaps().isEmpty() ? null : Map.of("overlaps", toPeriods(overlap.overlaps()))
            );
        }
        if (ex instanceof ContractCoverageIncompleteException gap) {
            return respond(
                    HttpStatus.CONFLICT,
                    "CONTRACT_COVERAGE_GAP",
                    ex.getMessage(),
                    Map.of(
                            "gaps", toPeriods(gap.gaps()),
                            "stretchCandidates", toPeriods(gap.stretchCandidates())
                    )
            );
        }
        if (ex instanceof ContractOutsidePresencePeriodException) {
            return respond(HttpStatus.CONFLICT, "CONTRACT_OUTSIDE_PRESENCE", ex.getMessage(), null);
        }

        return respond(HttpStatus.CONFLICT, "CONTRACT_ALREADY_CLOSED", ex.getMessage(), null);
    }

    private static ContractPeriodResponse toPeriod(ContractPeriod period) {
        return new ContractPeriodResponse(period.startDate(), period.endDate());
    }

    private static List<ContractPeriodResponse> toPeriods(List<ContractPeriod> periods) {
        return periods.stream().map(ContractExceptionHandler::toPeriod).toList();
    }

    private static ResponseEntity<ContractErrorResponse> respond(
            HttpStatus status,
            String code,
            String message,
            Map<String, Object> details
    ) {
        return ResponseEntity.status(status).body(new ContractErrorResponse(code, message, details));
    }
}

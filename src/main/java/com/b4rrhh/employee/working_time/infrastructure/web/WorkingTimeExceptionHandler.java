package com.b4rrhh.employee.working_time.infrastructure.web;

import com.b4rrhh.employee.working_time.domain.exception.InvalidWorkingTimeDateRangeException;
import com.b4rrhh.employee.working_time.domain.exception.InvalidWorkingTimePercentageException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeAlreadyClosedException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeCoverageGapException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeEmployeeNotFoundException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeNotFoundException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeNumberConflictException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeOutsidePresencePeriodException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeOverlapException;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimeOccurrence;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimePeriod;
import com.b4rrhh.employee.working_time.infrastructure.web.dto.WorkingTimeErrorResponse;
import com.b4rrhh.employee.working_time.infrastructure.web.dto.WorkingTimeOccurrenceResponse;
import com.b4rrhh.employee.working_time.infrastructure.web.dto.WorkingTimePeriodResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice(assignableTypes = WorkingTimeController.class)
public class WorkingTimeExceptionHandler {

    @ExceptionHandler({
            WorkingTimeEmployeeNotFoundException.class,
            WorkingTimeNotFoundException.class
    })
    public ResponseEntity<WorkingTimeErrorResponse> handleNotFound(RuntimeException ex) {
        if (ex instanceof WorkingTimeNotFoundException) {
            return notFound(
                    "WORKING_TIME_NOT_FOUND",
                    "No existe la jornada indicada para el empleado.",
                    null
            );
        }

        return notFound(
                "WORKING_TIME_NOT_FOUND",
                "No se ha encontrado el empleado solicitado para jornada.",
                null
        );
    }

    @ExceptionHandler({
            InvalidWorkingTimePercentageException.class,
            InvalidWorkingTimeDateRangeException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<WorkingTimeErrorResponse> handleBadRequest(RuntimeException ex) {
        if (ex instanceof InvalidWorkingTimePercentageException) {
            return badRequest(
                    "WORKING_TIME_INVALID_PERCENTAGE",
                    ex.getMessage(),
                    Map.of("field", "workingTimePercentage")
            );
        }

        return badRequest(
                "WORKING_TIME_INVALID_PERIOD",
                ex.getMessage(),
                null
        );
    }

    @ExceptionHandler({
            WorkingTimeOverlapException.class,
            WorkingTimeCoverageGapException.class,
            WorkingTimeOutsidePresencePeriodException.class,
            WorkingTimeAlreadyClosedException.class,
            WorkingTimeNumberConflictException.class
    })
    public ResponseEntity<WorkingTimeErrorResponse> handleConflict(RuntimeException ex) {
        if (ex instanceof WorkingTimeOverlapException overlap) {
            return conflict(
                    "WORKING_TIME_OVERLAP",
                    "El periodo informado se solapa con otra jornada del empleado.",
                    overlap.overlaps().isEmpty() ? null : Map.of("overlaps", periods(overlap.overlaps()))
            );
        }
        if (ex instanceof WorkingTimeCoverageGapException gap) {
            return conflict(
                    "WORKING_TIME_COVERAGE_GAP",
                    "La jornada dejaria sin cubrir un tramo de la presencia del empleado.",
                    Map.of(
                            "gaps", periods(gap.gaps()),
                            "stretchCandidates", occurrences(gap.stretchCandidates())
                    )
            );
        }
        if (ex instanceof WorkingTimeOutsidePresencePeriodException) {
            return conflict(
                    "WORKING_TIME_OUTSIDE_PRESENCE",
                    "El periodo informado queda fuera de cualquier presence válida del empleado.",
                    null
            );
        }
        if (ex instanceof WorkingTimeNumberConflictException) {
            return conflict(
                    "WORKING_TIME_NUMBER_CONFLICT",
                    "Se ha producido un conflicto de numeración funcional para la jornada del empleado.",
                    null
            );
        }

        return conflict(
                "WORKING_TIME_ALREADY_CLOSED",
                "La jornada ya estaba cerrada y no puede cerrarse nuevamente.",
                null
        );
    }

    private static List<WorkingTimePeriodResponse> periods(List<WorkingTimePeriod> periods) {
        return periods.stream()
                .map(period -> new WorkingTimePeriodResponse(period.startDate(), period.endDate()))
                .toList();
    }

    private static List<WorkingTimeOccurrenceResponse> occurrences(List<WorkingTimeOccurrence> occurrences) {
        return occurrences.stream()
                .map(occurrence -> new WorkingTimeOccurrenceResponse(
                        occurrence.workingTimeNumber(),
                        occurrence.startDate(),
                        occurrence.endDate()
                ))
                .toList();
    }

    private ResponseEntity<WorkingTimeErrorResponse> notFound(
            String code,
            String message,
            Map<String, Object> details
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new WorkingTimeErrorResponse(code, message, details));
    }

    private ResponseEntity<WorkingTimeErrorResponse> badRequest(
            String code,
            String message,
            Map<String, Object> details
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new WorkingTimeErrorResponse(code, message, details));
    }

    private ResponseEntity<WorkingTimeErrorResponse> conflict(
            String code,
            String message,
            Map<String, Object> details
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new WorkingTimeErrorResponse(code, message, details));
    }
}
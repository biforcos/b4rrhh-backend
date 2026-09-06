package com.b4rrhh.employee.workcenter.infrastructure.web;

import com.b4rrhh.employee.workcenter.domain.exception.InvalidWorkCenterDateRangeException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterAlreadyClosedException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterCatalogValueInvalidException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterCompanyMismatchException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterEmployeeNotFoundException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterIsACorrectionException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterNotFoundException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterOutsidePresencePeriodException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterOverlapException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterPresenceCoverageGapException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterRuleSystemNotFoundException;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenterOccurrence;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenterPeriod;
import com.b4rrhh.employee.workcenter.infrastructure.web.dto.WorkCenterErrorResponse;
import com.b4rrhh.employee.workcenter.infrastructure.web.dto.WorkCenterOccurrenceResponse;
import com.b4rrhh.employee.workcenter.infrastructure.web.dto.WorkCenterPeriodResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice(name = "employeeWorkCenterExceptionHandler", assignableTypes = WorkCenterController.class)
public class WorkCenterExceptionHandler {

    @ExceptionHandler({
            WorkCenterEmployeeNotFoundException.class,
            WorkCenterNotFoundException.class,
            WorkCenterRuleSystemNotFoundException.class
    })
    public ResponseEntity<WorkCenterErrorResponse> handleNotFound(RuntimeException ex) {
        if (ex instanceof WorkCenterNotFoundException) {
            return notFound(
                    "WORK_CENTER_NOT_FOUND",
                    "No existe la asignacion de centro de trabajo indicada para el empleado.",
                    null
            );
        }

        return notFound(
                "WORK_CENTER_NOT_FOUND",
                "No se ha encontrado el empleado o el sistema de reglas solicitado.",
                null
        );
    }

    @ExceptionHandler({
            WorkCenterCatalogValueInvalidException.class,
            InvalidWorkCenterDateRangeException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<WorkCenterErrorResponse> handleBadRequest(RuntimeException ex) {
        if (ex instanceof WorkCenterCatalogValueInvalidException) {
            return notFound(
                    "WORK_CENTER_CATALOG_NOT_FOUND",
                    "El centro de trabajo indicado no existe o no esta activo en catalogo para la fecha informada.",
                    Map.of("field", "workCenterCode")
            );
        }

        return conflict(
                "WORK_CENTER_INVALID_PERIOD",
                "La correccion de centro de trabajo es invalida por fechas o datos inconsistentes.",
                null
        );
    }

    /**
     * A plan rejection says what it ran into (ADR-057): the shared dates, the
     * gap and the neighbours to stretch, or the assignment an add would
     * correct instead of adding a second one.
     */
    @ExceptionHandler({
            WorkCenterAlreadyClosedException.class,
            WorkCenterCompanyMismatchException.class,
            WorkCenterIsACorrectionException.class,
            WorkCenterOverlapException.class,
            WorkCenterOutsidePresencePeriodException.class,
            WorkCenterPresenceCoverageGapException.class
    })
    public ResponseEntity<WorkCenterErrorResponse> handleConflict(RuntimeException ex) {
        if (ex instanceof WorkCenterCompanyMismatchException) {
            return conflict(
                    "WORK_CENTER_COMPANY_MISMATCH",
                    ex.getMessage(),
                    null
            );
        }
        if (ex instanceof WorkCenterIsACorrectionException correction) {
            WorkCenterOccurrence corrected = correction.correctedOccurrence();
            return conflict(
                    "WORK_CENTER_IS_A_CORRECTION",
                    "La fecha de inicio coincide con la de la asignacion n.º " + corrected.workCenterAssignmentNumber()
                            + ": esto no añade una asignacion de centro de trabajo, corrige esa. Confírmalo como correccion.",
                    Map.of("correctedOccurrence", occurrences(List.of(corrected)).get(0))
            );
        }
        if (ex instanceof WorkCenterOverlapException overlap) {
            return conflict(
                    "WORK_CENTER_OVERLAP",
                    "El periodo informado se solapa con otra asignacion de centro de trabajo del empleado.",
                    overlap.overlaps().isEmpty() ? null : Map.of("overlaps", periods(overlap.overlaps()))
            );
        }
        if (ex instanceof WorkCenterOutsidePresencePeriodException) {
            return conflict(
                    "WORK_CENTER_OUTSIDE_PRESENCE",
                    "El periodo informado queda fuera de cualquier presencia valida del empleado.",
                    null
            );
        }
        if (ex instanceof WorkCenterPresenceCoverageGapException gap) {
            return conflict(
                    "WORK_CENTER_COVERAGE_GAP",
                    "La operacion dejaria sin centro de trabajo un tramo de la presencia del empleado.",
                    Map.of(
                            "gaps", periods(gap.gaps()),
                            "stretchCandidates", occurrences(gap.stretchCandidates())
                    )
            );
        }
        if (ex instanceof WorkCenterAlreadyClosedException) {
            return conflict(
                    "WORK_CENTER_ALREADY_CLOSED",
                    "La asignacion de centro de trabajo ya estaba cerrada y no puede cerrarse nuevamente.",
                    null
            );
        }
        return conflict(
                "WORK_CENTER_INVALID_PERIOD",
                "La operacion de centro de trabajo entra en conflicto con las reglas temporales del empleado.",
                null
        );
    }

    private static List<WorkCenterPeriodResponse> periods(List<WorkCenterPeriod> periods) {
        return periods.stream()
                .map(period -> new WorkCenterPeriodResponse(period.startDate(), period.endDate()))
                .toList();
    }

    private static List<WorkCenterOccurrenceResponse> occurrences(List<WorkCenterOccurrence> occurrences) {
        return occurrences.stream()
                .map(occurrence -> new WorkCenterOccurrenceResponse(
                        occurrence.workCenterAssignmentNumber(),
                        occurrence.startDate(),
                        occurrence.endDate()
                ))
                .toList();
    }

    private ResponseEntity<WorkCenterErrorResponse> notFound(
            String code,
            String message,
            Map<String, Object> details
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new WorkCenterErrorResponse(code, message, details));
    }

    private ResponseEntity<WorkCenterErrorResponse> conflict(
            String code,
            String message,
            Map<String, Object> details
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new WorkCenterErrorResponse(code, message, details));
    }
}
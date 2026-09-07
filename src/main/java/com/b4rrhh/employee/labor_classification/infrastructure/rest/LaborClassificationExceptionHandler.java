package com.b4rrhh.employee.labor_classification.infrastructure.rest;

import com.b4rrhh.employee.labor_classification.domain.exception.InvalidLaborClassificationDateRangeException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationAgreementCategoryRelationInvalidException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationAgreementInvalidException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationAlreadyClosedException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationCategoryInvalidException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationCoverageIncompleteException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationEmployeeNotFoundException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationIsACorrectionException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationNotFoundException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationOutsidePresencePeriodException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationOverlapException;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassificationPeriod;
import com.b4rrhh.employee.labor_classification.infrastructure.rest.dto.LaborClassificationErrorResponse;
import com.b4rrhh.employee.labor_classification.infrastructure.rest.dto.LaborClassificationPeriodResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {
        LaborClassificationController.class,
        LaborClassificationCatalogController.class
})
public class LaborClassificationExceptionHandler {

    @ExceptionHandler({
            LaborClassificationEmployeeNotFoundException.class,
            LaborClassificationNotFoundException.class
    })
    public ResponseEntity<LaborClassificationErrorResponse> handleNotFound(RuntimeException ex) {
        if (ex instanceof LaborClassificationNotFoundException) {
            return notFound(
                    "LABOR_CLASSIFICATION_NOT_FOUND",
                    "No existe la clasificación laboral indicada para el empleado.",
                    null
            );
        }

        return notFound(
                "LABOR_CLASSIFICATION_NOT_FOUND",
                "No se ha encontrado el empleado solicitado para clasificación laboral.",
                null
        );
    }

    @ExceptionHandler({
            LaborClassificationAgreementInvalidException.class,
            LaborClassificationCategoryInvalidException.class,
            LaborClassificationAgreementCategoryRelationInvalidException.class,
            InvalidLaborClassificationDateRangeException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<LaborClassificationErrorResponse> handleBadRequest(RuntimeException ex) {
        if (ex instanceof LaborClassificationAgreementInvalidException) {
            return notFound(
                    "AGREEMENT_NOT_FOUND",
                    "El convenio indicado no existe o no está activo para la fecha informada.",
                    Map.of("field", "agreementCode")
            );
        }
        if (ex instanceof LaborClassificationCategoryInvalidException) {
            return notFound(
                    "AGREEMENT_CATEGORY_NOT_FOUND",
                    "La categoría de convenio indicada no existe o no está activa para la fecha informada.",
                    Map.of("field", "agreementCategoryCode")
            );
        }
        if (ex instanceof LaborClassificationAgreementCategoryRelationInvalidException) {
            return conflict(
                    "AGREEMENT_CATEGORY_RELATION_INVALID",
                    "La categoría de convenio no pertenece al convenio indicado para la fecha informada.",
                    null
            );
        }

        // Dates that do not hold up are a malformed request, not a clash with
        // the series: 400, and the message names the field, which is what a
        // client needs to tell its own mistake from a decision the series
        // made (backend#69). This was the only one of the five temporal
        // verticals answering it with a 409 and a canned message that named
        // nothing, while its own contract already declared the 400.
        return badRequest(
                "LABOR_CLASSIFICATION_INVALID_PERIOD",
                ex.getMessage(),
                null
        );
    }

    /**
     * A plan rejection says what it ran into (ADR-057): the shared dates, the
     * gap and the neighbours to stretch, or the occurrence an add would
     * correct instead of adding a second one.
     */
    @ExceptionHandler({
            LaborClassificationOverlapException.class,
            LaborClassificationOutsidePresencePeriodException.class,
            LaborClassificationCoverageIncompleteException.class,
            LaborClassificationIsACorrectionException.class,
            LaborClassificationAlreadyClosedException.class
    })
    public ResponseEntity<LaborClassificationErrorResponse> handleConflict(RuntimeException ex) {
        if (ex instanceof LaborClassificationIsACorrectionException correction) {
            LaborClassificationPeriod corrected = correction.correctedOccurrence();
            return conflict(
                    "LABOR_CLASSIFICATION_IS_A_CORRECTION",
                    "La fecha de inicio coincide con la de la clasificación laboral del " + corrected.startDate()
                            + ": esto no añade una clasificación, corrige esa. Confírmalo como corrección.",
                    Map.of("correctedOccurrence", toPeriod(corrected))
            );
        }
        if (ex instanceof LaborClassificationOverlapException overlap) {
            return conflict(
                    "LABOR_CLASSIFICATION_OVERLAP",
                    "El periodo informado se solapa con otra clasificación laboral del empleado.",
                    overlap.overlaps().isEmpty() ? null : Map.of("overlaps", toPeriods(overlap.overlaps()))
            );
        }
        if (ex instanceof LaborClassificationOutsidePresencePeriodException) {
            return conflict(
                    "LABOR_CLASSIFICATION_OUTSIDE_PRESENCE",
                    "El periodo informado queda fuera de cualquier presencia válida del empleado.",
                    null
            );
        }
        if (ex instanceof LaborClassificationCoverageIncompleteException gap) {
            return conflict(
                    "LABOR_CLASSIFICATION_INCOMPLETE_COVERAGE",
                    "La operación dejaría huecos en la cobertura de clasificación laboral frente a presence.",
                    Map.of(
                            "gaps", toPeriods(gap.gaps()),
                            "stretchCandidates", toPeriods(gap.stretchCandidates())
                    )
            );
        }

        return conflict(
                "LABOR_CLASSIFICATION_ALREADY_CLOSED",
                "La clasificación laboral ya estaba cerrada y no puede cerrarse nuevamente.",
                null
        );
    }

    private static LaborClassificationPeriodResponse toPeriod(LaborClassificationPeriod period) {
        return new LaborClassificationPeriodResponse(period.startDate(), period.endDate());
    }

    private static List<LaborClassificationPeriodResponse> toPeriods(List<LaborClassificationPeriod> periods) {
        return periods.stream().map(LaborClassificationExceptionHandler::toPeriod).toList();
    }

    private ResponseEntity<LaborClassificationErrorResponse> notFound(
            String code,
            String message,
            Map<String, Object> details
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new LaborClassificationErrorResponse(code, message, details));
    }

    private ResponseEntity<LaborClassificationErrorResponse> badRequest(
            String code,
            String message,
            Map<String, Object> details
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new LaborClassificationErrorResponse(code, message, details));
    }

    private ResponseEntity<LaborClassificationErrorResponse> conflict(
            String code,
            String message,
            Map<String, Object> details
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new LaborClassificationErrorResponse(code, message, details));
    }
}

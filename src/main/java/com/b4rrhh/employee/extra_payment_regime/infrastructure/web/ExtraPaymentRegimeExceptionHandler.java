package com.b4rrhh.employee.extra_payment_regime.infrastructure.web;

import com.b4rrhh.employee.extra_payment_regime.domain.exception.InvalidExtraPaymentRegimeDateRangeException;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeAlreadyClosedException;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeCoverageGapException;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeEmployeeNotFoundException;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeIsACorrectionException;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeNotFoundException;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeNumberConflictException;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeOutsidePresencePeriodException;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeOverlapException;
import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegimeOccurrence;
import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegimePeriod;
import com.b4rrhh.employee.extra_payment_regime.infrastructure.web.dto.ExtraPaymentRegimeErrorResponse;
import com.b4rrhh.employee.extra_payment_regime.infrastructure.web.dto.ExtraPaymentRegimeOccurrenceResponse;
import com.b4rrhh.employee.extra_payment_regime.infrastructure.web.dto.ExtraPaymentRegimePeriodResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice(assignableTypes = ExtraPaymentRegimeController.class)
public class ExtraPaymentRegimeExceptionHandler {

    @ExceptionHandler({
            ExtraPaymentRegimeEmployeeNotFoundException.class,
            ExtraPaymentRegimeNotFoundException.class
    })
    public ResponseEntity<ExtraPaymentRegimeErrorResponse> handleNotFound(RuntimeException ex) {
        if (ex instanceof ExtraPaymentRegimeNotFoundException) {
            return notFound(
                    "EXTRA_PAYMENT_REGIME_NOT_FOUND",
                    "No existe el tramo de regimen de pagas extras indicado para el empleado.",
                    null
            );
        }

        return notFound(
                "EXTRA_PAYMENT_REGIME_NOT_FOUND",
                "No se ha encontrado el empleado solicitado para el regimen de pagas extras.",
                null
        );
    }

    @ExceptionHandler({
            InvalidExtraPaymentRegimeDateRangeException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ExtraPaymentRegimeErrorResponse> handleBadRequest(RuntimeException ex) {
        return badRequest(
                "EXTRA_PAYMENT_REGIME_INVALID_PERIOD",
                ex.getMessage(),
                null
        );
    }

    @ExceptionHandler({
            ExtraPaymentRegimeOverlapException.class,
            ExtraPaymentRegimeCoverageGapException.class,
            ExtraPaymentRegimeIsACorrectionException.class,
            ExtraPaymentRegimeOutsidePresencePeriodException.class,
            ExtraPaymentRegimeAlreadyClosedException.class,
            ExtraPaymentRegimeNumberConflictException.class
    })
    public ResponseEntity<ExtraPaymentRegimeErrorResponse> handleConflict(RuntimeException ex) {
        if (ex instanceof ExtraPaymentRegimeIsACorrectionException correction) {
            ExtraPaymentRegimeOccurrence corrected = correction.correctedOccurrence();
            return conflict(
                    "EXTRA_PAYMENT_REGIME_IS_A_CORRECTION",
                    "La fecha de inicio coincide con la del tramo n.º " + corrected.extraPaymentRegimeNumber()
                            + ": esto no añade un tramo, corrige ese. Confírmalo como corrección.",
                    Map.of("correctedOccurrence", occurrences(List.of(corrected)).get(0))
            );
        }
        if (ex instanceof ExtraPaymentRegimeOverlapException overlap) {
            return conflict(
                    "EXTRA_PAYMENT_REGIME_OVERLAP",
                    "El periodo informado se solapa con otro tramo de regimen de pagas extras del empleado.",
                    overlap.overlaps().isEmpty() ? null : Map.of("overlaps", periods(overlap.overlaps()))
            );
        }
        if (ex instanceof ExtraPaymentRegimeCoverageGapException gap) {
            return conflict(
                    "EXTRA_PAYMENT_REGIME_COVERAGE_GAP",
                    "El regimen de pagas extras dejaria sin cubrir un tramo de la presencia del empleado.",
                    Map.of(
                            "gaps", periods(gap.gaps()),
                            "stretchCandidates", occurrences(gap.stretchCandidates())
                    )
            );
        }
        if (ex instanceof ExtraPaymentRegimeOutsidePresencePeriodException) {
            return conflict(
                    "EXTRA_PAYMENT_REGIME_OUTSIDE_PRESENCE",
                    "El periodo informado queda fuera de cualquier presence válida del empleado.",
                    null
            );
        }
        if (ex instanceof ExtraPaymentRegimeNumberConflictException) {
            return conflict(
                    "EXTRA_PAYMENT_REGIME_NUMBER_CONFLICT",
                    "Se ha producido un conflicto de numeración funcional para el regimen de pagas extras del empleado.",
                    null
            );
        }

        return conflict(
                "EXTRA_PAYMENT_REGIME_ALREADY_CLOSED",
                "El tramo de regimen de pagas extras ya estaba cerrado y no puede cerrarse nuevamente.",
                null
        );
    }

    private static List<ExtraPaymentRegimePeriodResponse> periods(List<ExtraPaymentRegimePeriod> periods) {
        return periods.stream()
                .map(period -> new ExtraPaymentRegimePeriodResponse(period.startDate(), period.endDate()))
                .toList();
    }

    private static List<ExtraPaymentRegimeOccurrenceResponse> occurrences(List<ExtraPaymentRegimeOccurrence> occurrences) {
        return occurrences.stream()
                .map(occurrence -> new ExtraPaymentRegimeOccurrenceResponse(
                        occurrence.extraPaymentRegimeNumber(),
                        occurrence.startDate(),
                        occurrence.endDate()
                ))
                .toList();
    }

    private ResponseEntity<ExtraPaymentRegimeErrorResponse> notFound(
            String code,
            String message,
            Map<String, Object> details
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ExtraPaymentRegimeErrorResponse(code, message, details));
    }

    private ResponseEntity<ExtraPaymentRegimeErrorResponse> badRequest(
            String code,
            String message,
            Map<String, Object> details
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ExtraPaymentRegimeErrorResponse(code, message, details));
    }

    private ResponseEntity<ExtraPaymentRegimeErrorResponse> conflict(
            String code,
            String message,
            Map<String, Object> details
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ExtraPaymentRegimeErrorResponse(code, message, details));
    }
}
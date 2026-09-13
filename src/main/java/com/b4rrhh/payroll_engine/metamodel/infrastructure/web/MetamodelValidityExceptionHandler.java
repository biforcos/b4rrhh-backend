package com.b4rrhh.payroll_engine.metamodel.infrastructure.web;

import com.b4rrhh.payroll_engine.metamodel.domain.exception.ValidityWindowDoesNotCoverWholePeriodsException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * La vigencia que cubre periodos enteros es un invariante de la reglamentacion, no de un
 * vertical: lo comparten las asignaciones, las alimentaciones y las filas de tabla. Por eso
 * el manejador vive en {@code metamodel} y no en ninguno de los tres.
 *
 * <p>Va a la maxima precedencia porque el vertical de concepto mapea
 * {@code IllegalArgumentException} a 400 con su propio cuerpo, y sin esto una vigencia mal
 * puesta saldria con el mensaje del vertical equivocado.
 */
@RestControllerAdvice(basePackages = "com.b4rrhh.payroll_engine")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MetamodelValidityExceptionHandler {

    @ExceptionHandler(ValidityWindowDoesNotCoverWholePeriodsException.class)
    public ResponseEntity<MetamodelValidityErrorResponse> handleWindowDoesNotCoverWholePeriods(
            ValidityWindowDoesNotCoverWholePeriodsException ex
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MetamodelValidityErrorResponse(ex.getMessage()));
    }
}

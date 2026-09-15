package com.b4rrhh.payroll.infrastructure.web;

import com.b4rrhh.payroll.application.usecase.PayrollLaunchInputMissingException;
import com.b4rrhh.payroll.domain.exception.InvalidPayrollArgumentException;
import com.b4rrhh.payroll.domain.exception.PayrollBusinessKeyConflictException;
import com.b4rrhh.payroll.domain.exception.PayrollCalculationFailedException;
import com.b4rrhh.payroll.domain.exception.PayrollEmployeePresenceNotFoundException;
import com.b4rrhh.payroll.domain.exception.PayrollInvalidStateTransitionException;
import com.b4rrhh.payroll.domain.exception.PayrollNotFoundException;
import com.b4rrhh.payroll.domain.exception.PayrollRecalculationNotAllowedException;
import com.b4rrhh.payroll.domain.exception.PayrollTypeInvalidException;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {PayrollController.class, PayrollCalculationRunController.class})
public class PayrollExceptionHandler {

    @ExceptionHandler({
            PayrollNotFoundException.class,
            PayrollEmployeePresenceNotFoundException.class
    })
    public ResponseEntity<PayrollErrorResponse> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(PayrollErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler({
            InvalidPayrollArgumentException.class,
            IllegalArgumentException.class,
            PayrollTypeInvalidException.class
    })
    public ResponseEntity<PayrollErrorResponse> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(PayrollErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler({
            PayrollInvalidStateTransitionException.class,
            PayrollRecalculationNotAllowedException.class,
            PayrollBusinessKeyConflictException.class
    })
    public ResponseEntity<PayrollErrorResponse> handleConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(PayrollErrorResponse.of(ex.getMessage()));
    }

    /**
     * Un calculo que falla ya no sale por la puerta de atras.
     *
     * <p>422 y no 500: la peticion esta bien y el estado del recibo tambien; lo que no se puede
     * procesar es la reglamentacion que hay detras —una vinculacion desactivada, una fila de
     * tabla sin vigencia, un operando que falta—. Es lo mismo que ya significa el 422 en el
     * alta y en el cese, y la diferencia le importa a quien mira: un 500 se reintenta, un 422
     * se arregla. El paso 7 del camino —tocar una regla y recalcular— produce exactamente esto
     * (#100).
     */
    @ExceptionHandler(PayrollCalculationFailedException.class)
    public ResponseEntity<PayrollErrorResponse> handleCalculationFailed(PayrollCalculationFailedException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new PayrollErrorResponse(ex.getMessageCode(), ex.getMessage(), ex.getDetails()));
    }

    /**
     * Y el otro fallo de calculo, que no es el mismo: la unidad era elegible y le faltaba un dato
     * de entrada. El lanzamiento masivo lo separa desde el backend#85 y le da su propio codigo y
     * su propio contador; por la puerta de uno en uno se iba con el resto en un 500, que ademas
     * de mudo los confundia.
     */
    @ExceptionHandler(PayrollLaunchInputMissingException.class)
    public ResponseEntity<PayrollErrorResponse> handleInputMissing(PayrollLaunchInputMissingException ex) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("reasonCode", ex.getReasonCode());
        details.putAll(ex.getDetails());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new PayrollErrorResponse(
                        PayrollLaunchInputMissingException.MESSAGE_CODE, ex.getMessage(), details));
    }
}

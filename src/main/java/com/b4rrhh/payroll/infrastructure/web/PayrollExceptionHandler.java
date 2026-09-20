package com.b4rrhh.payroll.infrastructure.web;

import com.b4rrhh.payroll.application.usecase.PayrollLaunchInputMissingException;
import com.b4rrhh.payroll.document.domain.exception.PayslipDocumentNotArchivedException;
import com.b4rrhh.payroll.document.domain.exception.PayslipDocumentStorageUnavailableException;
import com.b4rrhh.payroll.document.infrastructure.web.PayslipDocumentController;
import com.b4rrhh.payroll.domain.exception.InvalidPayrollArgumentException;
import com.b4rrhh.payroll.domain.exception.PayrollBusinessKeyConflictException;
import com.b4rrhh.payroll.domain.exception.PayrollCalculationFailedException;
import com.b4rrhh.payroll.domain.exception.PayrollEmployeePresenceNotFoundException;
import com.b4rrhh.payroll.domain.exception.PayrollInvalidStateTransitionException;
import com.b4rrhh.payroll.domain.exception.PayrollNotFoundException;
import com.b4rrhh.payroll.domain.exception.PayrollRecalculationNotAllowedException;
import com.b4rrhh.payroll.domain.exception.PayrollTypeInvalidException;
import com.b4rrhh.payroll.domain.exception.PayrollUnitAlreadyClaimedException;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {
        PayrollController.class,
        PayrollCalculationRunController.class,
        PayslipDocumentController.class
})
public class PayrollExceptionHandler {

    @ExceptionHandler({
            PayrollNotFoundException.class,
            PayrollEmployeePresenceNotFoundException.class
    })
    public ResponseEntity<PayrollErrorResponse> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(PayrollErrorResponse.of(ex.getMessage()));
    }

    /**
     * El almacen de documentos no contesta ({@code backend#112}).
     *
     * <p>503 y no 500: no es que el recibo este mal, es que la pieza que guarda los papeles no
     * esta. Quien lo reciba <b>puede reintentar</b> y eso es lo que separa este fallo de los otros
     * — decirlo con el codigo evita que la pantalla invente un mensaje de catastrofe para algo que
     * se arregla volviendo a darle al boton.
     */
    @ExceptionHandler(PayslipDocumentStorageUnavailableException.class)
    public ResponseEntity<PayrollErrorResponse> handleStorageUnavailable(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new PayrollErrorResponse(
                "PAYSLIP_DOCUMENT_STORAGE_UNAVAILABLE", ex.getMessage(), null));
    }

    /**
     * Un recibo cerrado al que le falta su documento ({@code backend#112}).
     *
     * <p>409 porque el recibo existe y esta en un estado que no cuadra con lo que se le pide, y
     * con codigo porque no se parece a nada: no es que no haya recibo —eso es 404— ni que el
     * almacen este caido —eso es 503—. Es que este recibo se cerro antes de que cerrar emitiera el
     * papel, y <b>no se regenera</b>: seria otro documento del mismo recibo.
     */
    @ExceptionHandler(PayslipDocumentNotArchivedException.class)
    public ResponseEntity<PayrollErrorResponse> handleDocumentNotArchived(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new PayrollErrorResponse(
                "PAYSLIP_DOCUMENT_NOT_ARCHIVED", ex.getMessage(), null));
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
     * Y el conflicto que no es de estado sino de momento: la unidad la esta calculando otro
     * ahora mismo (backend#101).
     *
     * <p>409 como los demas conflictos, pero con codigo, porque este se puede reintentar y los
     * otros no: un recibo validado no va a dejar de estarlo por esperar, y una unidad cogida se
     * suelta en cuanto el otro termina. Quien lo pinte necesita distinguirlo sin leer la prosa
     * inglesa del {@code message}, y el codigo es el mismo que el lanzamiento masivo escribe en
     * el mensaje de la unidad, traducido ya por la V125.
     */
    @ExceptionHandler(PayrollUnitAlreadyClaimedException.class)
    public ResponseEntity<PayrollErrorResponse> handleAlreadyClaimed(PayrollUnitAlreadyClaimedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new PayrollErrorResponse(ex.getMessageCode(), ex.getMessage(), ex.getDetails()));
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

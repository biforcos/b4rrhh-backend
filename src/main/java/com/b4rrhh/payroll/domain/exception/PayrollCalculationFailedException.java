package com.b4rrhh.payroll.domain.exception;

import java.util.Map;

/**
 * El calculo de una unidad termino en error.
 *
 * <p>El lanzamiento masivo ya sabia decir esto: guarda un mensaje de ejecucion con el codigo
 * {@code UNIT_CALCULATION_ERROR}, que el catalogo de la V125 traduce a «El cálculo falló». El
 * recalculo puntual tiraba la misma excepcion a la calle y el cliente recibia un 500 sin nada
 * que ensenar (#100). Esto le da la misma forma: <b>un mismo fallo se llama igual por las dos
 * puertas</b>, y la pantalla del recibo puede decir lo que ya dice la de Operaciones.
 */
public class PayrollCalculationFailedException extends RuntimeException {

    /**
     * El mismo codigo que {@code LaunchPayrollCalculationService} escribe en
     * {@code payroll.calculation_run_message} cuando una unidad falla. Que este literal y aquel
     * sigan diciendo lo mismo lo vigila {@code EveryRunMessageCodeIsInTheCatalogTest}, que
     * cruza los dos contra el catalogo sembrado en la V125 (backend#81).
     */
    public static final String MESSAGE_CODE = "UNIT_CALCULATION_ERROR";

    private final transient Map<String, Object> details;

    public PayrollCalculationFailedException(RuntimeException cause) {
        super(cause.getMessage(), cause);
        this.details = Map.of("exceptionType", cause.getClass().getSimpleName());
    }

    public String getMessageCode() {
        return MESSAGE_CODE;
    }

    /** Lo mismo que el lanzamiento guarda en los detalles del mensaje de la unidad. */
    public Map<String, Object> getDetails() {
        return details;
    }
}

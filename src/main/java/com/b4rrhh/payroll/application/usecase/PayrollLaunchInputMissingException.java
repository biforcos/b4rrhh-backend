package com.b4rrhh.payroll.application.usecase;

import java.util.Map;

public class PayrollLaunchInputMissingException extends RuntimeException {

    /**
     * El codigo de mensaje con el que el lanzamiento masivo anota esta unidad. No es lo mismo que
     * {@link #getReasonCode()}, que dice <b>que</b> dato faltaba: esto dice como se llama el
     * suceso, y es el codigo que el catalogo de la V125 traduce. Un recalculo puntual que se
     * encuentra lo mismo contesta con este, y no con el de «el calculo fallo»: el lanzamiento
     * los separa a proposito desde el backend#85 —«ya estaba hecho» no pide nada de nadie y esto
     * siempre pide que alguien mire—, y perder esa distincion por la puerta de uno en uno seria
     * deshacerlo (#100).
     */
    public static final String MESSAGE_CODE = "UNIT_ELIGIBLE_REAL_SKIPPED_MISSING_INPUT";

    private final String reasonCode;
    private final Map<String, Object> details;

    public PayrollLaunchInputMissingException(String reasonCode, String message, Map<String, Object> details) {
        super(message);
        this.reasonCode = reasonCode;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}

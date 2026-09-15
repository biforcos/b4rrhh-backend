package com.b4rrhh.payroll.domain.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * La unidad la esta calculando otro ahora mismo.
 *
 * <p>Primero el que llegue, y quien pierde lo dice (backend#101). La reserva de
 * {@code payroll.calculation_claim} es lo unico que decide quien llego antes, y desde este issue
 * la toman los dos caminos: el lanzamiento masivo, que ya lo hacia, y el recalculo puntual, que
 * pasaba de largo. El que no la consigue no espera —un boton principal que se queda pensando
 * cinco minutos porque alguien lanzo la plantilla es peor que un «ahora no»— y sale por aqui.
 *
 * <p>No es un fallo de calculo y no puede contarse como tal: ahi no fallo nada, la unidad estaba
 * cogida. Por eso lleva el codigo que el lanzamiento ya usaba para lo mismo en vez de uno nuevo.
 */
public class PayrollUnitAlreadyClaimedException extends RuntimeException {

    /**
     * El mismo codigo que {@code LaunchPayrollCalculationService} escribe en
     * {@code payroll.calculation_run_message} cuando una unidad esta cogida, sembrado en el
     * catalogo por la V125. Un mismo suceso se llama igual por las dos puertas, como el
     * {@code UNIT_CALCULATION_ERROR} del #100.
     */
    public static final String MESSAGE_CODE = "UNIT_ALREADY_CLAIMED";

    private final transient Map<String, Object> details;

    public PayrollUnitAlreadyClaimedException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String payrollPeriodCode,
            String payrollTypeCode,
            Integer presenceNumber
    ) {
        super("Payroll calculation unit is already claimed by another calculation run. Business key "
                + ruleSystemCode + "/"
                + employeeTypeCode + "/"
                + employeeNumber + "/"
                + payrollPeriodCode + "/"
                + payrollTypeCode + "/"
                + presenceNumber + " is being calculated right now; try again in a moment");
        Map<String, Object> detalles = new LinkedHashMap<>();
        detalles.put("ruleSystemCode", ruleSystemCode);
        detalles.put("employeeTypeCode", employeeTypeCode);
        detalles.put("employeeNumber", employeeNumber);
        detalles.put("payrollPeriodCode", payrollPeriodCode);
        detalles.put("payrollTypeCode", payrollTypeCode);
        detalles.put("presenceNumber", presenceNumber);
        this.details = Collections.unmodifiableMap(detalles);
    }

    public String getMessageCode() {
        return MESSAGE_CODE;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}

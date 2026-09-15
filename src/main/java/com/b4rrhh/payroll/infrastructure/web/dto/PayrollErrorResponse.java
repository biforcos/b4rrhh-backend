package com.b4rrhh.payroll.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * La forma de error de /payrolls. {@code message} es lo unico que llevaba hasta el #100.
 *
 * <p>{@code code} y {@code details} son opcionales y van con {@code NON_NULL} a proposito: los
 * errores que ya existian (404, 409) siguen contestando exactamente {@code {"message": ...}},
 * byte a byte. Lo que se anade es una forma <b>mas rica</b> para el fallo de calculo, no una
 * segunda forma: una tercera manera de decir «ha fallado» es justo lo que el backend#78 esta
 * intentando deshacer en /payroll-engine.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PayrollErrorResponse(String code, String message, Map<String, Object> details) {

    public static PayrollErrorResponse of(String message) {
        return new PayrollErrorResponse(null, message, null);
    }
}

package com.b4rrhh.employee.presence.application.port;

import java.util.List;

/**
 * Los recibos ya cerrados de una presencia (backend#172): lo que se pagó no se rectifica por debajo.
 */
public interface PresencePayrollLookupPort {

    /** Los periodos (aaaamm) con recibo DEFINITIVE de esa presencia, ordenados. */
    List<String> definitivePeriods(String ruleSystemCode, String employeeTypeCode, String employeeNumber,
                                   Integer presenceNumber);
}

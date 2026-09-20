package com.b4rrhh.payroll.document.domain.model;

import com.b4rrhh.payroll.domain.model.Payroll;

/**
 * Donde se guarda el documento de un recibo ({@code backend#112}).
 *
 * <p>La direccion es la <b>clave de negocio</b> del recibo y no su id: los ids surrogados cambian
 * entre corridas, y un documento entregado tiene que poder encontrarse con lo que va impreso en el
 * papel. Es la misma decision que el ADR-004 toma para el API, aplicada al almacen.
 *
 * <p>La presencia forma parte de la direccion y no es un adorno: {@code EMP000001} es un
 * readmitido y su recibo vive en la presencia 2. Una clave que diera por hecho la 1 acertaria en
 * 998 empleados de la semilla y se equivocaria de documento en dos.
 */
public final class PayslipDocumentKey {

    private PayslipDocumentKey() {
    }

    /** Por ejemplo {@code ESP/202609/NORMAL/INTERNAL/EMP000001-2.pdf}. */
    public static String of(Payroll payroll) {
        return String.join("/",
                payroll.getRuleSystemCode(),
                payroll.getPayrollPeriodCode(),
                payroll.getPayrollTypeCode(),
                payroll.getEmployeeTypeCode(),
                payroll.getEmployeeNumber() + "-" + payroll.getPresenceNumber() + ".pdf");
    }
}

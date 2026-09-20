package com.b4rrhh.payroll.document.domain.exception;

/**
 * Un recibo definitivo al que le falta su documento ({@code backend#112}).
 *
 * <p>No deberia poder pasar: desde este issue, cerrar y archivar son el mismo acto y el cierre
 * falla si el archivo no se puede escribir. Existe para los recibos que se cerraron <b>antes</b>,
 * y para que ese caso se vea en vez de taparse regenerando el documento.
 *
 * <p>Regenerarlo seria la salida comoda y es justo la prohibida: daria un papel parecido al que el
 * empleado tiene y no el mismo, y nadie sabria que no es el mismo.
 */
public class PayslipDocumentNotArchivedException extends RuntimeException {

    public PayslipDocumentNotArchivedException(String objectKey) {
        super("El recibo esta cerrado y su documento no esta en el almacen (" + objectKey
                + "). No se regenera: seria otro documento del mismo recibo.");
    }
}

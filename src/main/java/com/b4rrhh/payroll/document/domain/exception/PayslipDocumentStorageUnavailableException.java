package com.b4rrhh.payroll.document.domain.exception;

/**
 * El almacen de documentos no contesta ({@code backend#112}).
 *
 * <p><b>Cerrar un recibo con esto encima tiene que fallar</b>, y eso no es una eleccion tecnica:
 * un {@code DEFINITIVE} sin su documento es una promesa rota. Los dos errores no cuestan igual
 * —fallar el cierre se reintenta en un minuto; un cerrado sin PDF es un documento que no existe—,
 * y cuando no cuestan igual el defecto va del lado barato.
 */
public class PayslipDocumentStorageUnavailableException extends RuntimeException {

    public PayslipDocumentStorageUnavailableException(String objectKey, Throwable cause) {
        super("El almacen de documentos no responde para " + objectKey, cause);
    }
}

package com.b4rrhh.payroll.document.domain.port;

import java.util.Optional;

/**
 * Donde viven los documentos de los recibos cerrados ({@code backend#112}).
 *
 * <p>Guarda <b>documentos</b>, no borradores. Lo que se escribe aqui es lo que el empleado tiene
 * en la mano, y por eso solo pasa por aqui lo definitivo: un recibo que todavia se puede recalcular
 * no ha entregado nada que archivar.
 */
public interface PayslipDocumentStorage {

    /**
     * Guarda el documento. Falla con
     * {@link com.b4rrhh.payroll.document.domain.exception.PayslipDocumentStorageUnavailableException}
     * si el almacen no contesta, y quien lo llama <b>no lo tapa</b>.
     */
    void store(String objectKey, byte[] content, String contentType);

    /**
     * Lo guardado, si esta.
     *
     * <p>Vacio y caido son dos cosas distintas y se distinguen: vacio devuelve
     * {@link Optional#empty()}, caido revienta. Confundirlos convertiria un almacen apagado en
     * «ese recibo no tiene documento», que es la mentira mas cara de todas.
     */
    Optional<byte[]> retrieve(String objectKey);
}

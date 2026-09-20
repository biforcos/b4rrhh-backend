package com.b4rrhh.payroll.document.domain.model;

/**
 * El documento de un recibo, listo para entregar ({@code backend#112}).
 *
 * @param content     los bytes.
 * @param contentType con que tipo se sirve.
 * @param fileName    con que nombre se guarda en el disco de quien lo descarga. Un borrador lo
 *                    lleva escrito en el nombre: el fichero sale del navegador y acaba en una
 *                    carpeta, lejos de la pantalla que sabia en que estado estaba el recibo.
 * @param definitive  si esto salio del almacen —y entonces <b>es</b> el documento— o si se ha
 *                    renderizado ahora mismo para mirarlo.
 */
public record PayslipDocument(
        byte[] content,
        String contentType,
        String fileName,
        boolean definitive
) {
}

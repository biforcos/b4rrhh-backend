package com.b4rrhh.payroll.document.application.port;

/**
 * Quien convierte el contenido del recibo en el fichero que se entrega ({@code backend#112}).
 *
 * <p>Es un puerto secundario y no un servicio de aplicacion porque dibujar en A4 es un detalle de
 * infraestructura: la libreria que lo hace se eligio y puede volver a elegirse. Lo que no cambia
 * es lo que hay que dibujar, y eso es {@link PayslipDocumentContent}.
 */
public interface PayslipDocumentRenderer {

    /** Los bytes del documento. Del tipo que declare {@link #contentType()}. */
    byte[] render(PayslipDocumentContent content);

    /** Con que tipo se sirve lo que sale de aqui. */
    String contentType();
}

package com.b4rrhh.payroll.document;

import com.b4rrhh.payroll.application.service.PayrollSnapshotProfiles;
import com.b4rrhh.payroll.document.application.port.PayslipDocumentContent;
import com.b4rrhh.payroll.document.application.service.PayslipDocumentContentFactory;
import com.b4rrhh.payroll.document.infrastructure.pdf.PdfBoxPayslipDocumentRenderer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dibujar dos veces el mismo recibo da los mismos bytes ({@code backend#174}, punto 1).
 *
 * <p>Es la condicion para regenerar un cerrado sin documento: si el papel de hoy no es <b>el
 * mismo</b> que el de ayer, regenerarlo es inventarse un segundo documento del recibo. Y no lo era
 * por un detalle que no se ve abriendo el PDF: PDFBox escribe en el trailer un {@code /ID} que
 * saca de la hora del reloj si nadie le da uno. Dos dibujos con un milisegundo de diferencia
 * diferian en esos dieciseis bytes.
 *
 * <p>La espera entre los dos dibujos es lo que hace que esto pruebe algo: sin ella, los dos caen
 * en el mismo milisegundo y el test sale verde con el reloj dentro.
 */
class ThePdfIsTheSameEveryTimeItIsDrawnTest {

    private final PayslipDocumentContentFactory contenido =
            new PayslipDocumentContentFactory(new PayrollSnapshotProfiles(new ObjectMapper()));
    private final PdfBoxPayslipDocumentRenderer renderer = new PdfBoxPayslipDocumentRenderer();

    @Test
    void drawingTheSameClosedPayslipTwiceGivesTheSameBytes() throws InterruptedException {
        PayslipDocumentContent cerrado = contenido.contentOf(
                PayslipDocumentFixtures.cerrado(PayslipDocumentFixtures.emp000001()),
                PayslipDocumentFixtures.seccionesDelModeloOficial());

        byte[] primero = renderer.render(cerrado);
        Thread.sleep(20);
        byte[] segundo = renderer.render(cerrado);

        assertArrayEquals(primero, segundo,
                "El mismo recibo, dibujado otra vez, tiene que ser el mismo documento");
    }

    /**
     * Y dos recibos distintos no comparten identificador: el {@code /ID} sale del recibo, no es
     * una constante. Una constante tambien haria verde el test de arriba.
     */
    @Test
    void twoDifferentPayslipsDoNotShareTheirIdentifier() {
        byte[] uno = renderer.render(contenido.contentOf(
                PayslipDocumentFixtures.cerrado(PayslipDocumentFixtures.emp000001()),
                PayslipDocumentFixtures.seccionesDelModeloOficial()));
        byte[] otro = renderer.render(contenido.contentOf(
                PayslipDocumentFixtures.cerrado(PayslipDocumentFixtures.emp000005()),
                PayslipDocumentFixtures.seccionesDelModeloOficial()));

        String idDeUno = identificador(uno);
        String idDeOtro = identificador(otro);
        assertTrue(!idDeUno.isEmpty() && !idDeOtro.isEmpty(), "Los dos llevan /ID en el trailer");
        assertFalse(idDeUno.equals(idDeOtro), "Y no es el mismo: " + idDeUno);
    }

    private static String identificador(byte[] pdf) {
        String texto = new String(pdf, java.nio.charset.StandardCharsets.ISO_8859_1);
        int desde = texto.lastIndexOf("/ID");
        return desde < 0 ? "" : texto.substring(desde, texto.indexOf(']', desde) + 1);
    }
}

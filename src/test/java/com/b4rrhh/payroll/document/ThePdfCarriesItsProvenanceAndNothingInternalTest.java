package com.b4rrhh.payroll.document;

import com.b4rrhh.payroll.application.service.PayrollSnapshotProfiles;
import com.b4rrhh.payroll.document.application.service.PayslipDocumentContentFactory;
import com.b4rrhh.payroll.document.infrastructure.pdf.PdfBoxPayslipDocumentRenderer;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollConcept;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static com.b4rrhh.payroll.document.ThePdfSaysTheSameAsTheFolioTest.textoDe;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El PDF lleva de donde sale y no lleva nada de dentro ({@code backend#112}, criterios 6 y 3).
 *
 * <h2>El candado contra «ya que estamos, añado el grafo»</h2>
 *
 * <p>La explicacion es la herramienta del que opera; el PDF es el documento del que cobra. La
 * tentacion de meter en el papel los mnemonicos, los pasos o el grafo es real y llega siempre por
 * el mismo camino —«total, ya lo tenemos calculado»—, y la diferencia entre un recibo y un volcado
 * de motor es justo esa.
 *
 * <p>Lo que el papel si hace es <b>apuntar</b> a la explicacion: lleva impresa la clave de negocio
 * del recibo, y con ella cualquiera con acceso lo abre en el sistema y ve de donde sale cada
 * linea. <b>El puente es la clave, no el contenido.</b> Por eso este test comprueba las dos mitades
 * a la vez: que la clave esta, y que lo interno no.
 *
 * <p>Los mnemonicos no se escriben a mano en la lista prohibida: se sacan del propio recibo. Un
 * concepto nuevo queda vigilado sin tocar este fichero, que es la diferencia entre un candado y
 * una foto.
 */
class ThePdfCarriesItsProvenanceAndNothingInternalTest {

    private final PayrollSnapshotProfiles profiles = new PayrollSnapshotProfiles(new ObjectMapper());
    private final PayslipDocumentContentFactory contenido = new PayslipDocumentContentFactory(profiles);
    private final PdfBoxPayslipDocumentRenderer renderizador = new PdfBoxPayslipDocumentRenderer();

    /** Criterio 6: ni un mnemonico, en ningun recibo de la semilla. */
    @Test
    void noMnemonicOfAnyLineReachesThePaper() throws IOException {
        for (Payroll payroll : List.of(
                PayslipDocumentFixtures.cerrado(PayslipDocumentFixtures.emp000001()),
                PayslipDocumentFixtures.cerrado(PayslipDocumentFixtures.emp000005()))) {
            String texto = textoDe(pdfDe(payroll));
            for (PayrollConcept concept : payroll.getConcepts()) {
                assertFalse(texto.contains(concept.getConceptMnemonic()),
                        "El mnemonico «" + concept.getConceptMnemonic() + "» es el identificador "
                                + "del concepto en el motor y no tiene nada que hacer en el papel");
            }
        }
    }

    /** Criterio 6: ni el rastro del calculo —pasos, ejecuciones, grafo—. */
    @Test
    void noTraceOfTheCalculationReachesThePaper() throws IOException {
        String texto = textoDe(pdfDe(PayslipDocumentFixtures.cerrado(PayslipDocumentFixtures.emp000001())))
                .toLowerCase();

        for (String interno : List.of(
                "paso ", "pasos", "execution_order", "orden de ejecución", "orden de ejecucion",
                "grafo", "operando", "mnemónico", "mnemonico", "engine_provided",
                "rate_by_quantity", "direct_amount", "aggregate", "tramos")) {
            assertFalse(texto.contains(interno),
                    "«" + interno + "» es explicacion del calculo, y la explicacion vive en la "
                            + "pantalla: el papel lleva la clave para llegar a ella");
        }
    }

    /**
     * Criterio 6: la procedencia completa, que es la mitad que si tiene que estar.
     *
     * <p>Sin la clave, el papel es una hoja con numeros que no se puede volver a encontrar. Con
     * ella, es la puerta de entrada al recibo y a su explicacion.
     */
    @Test
    void theWholeProvenanceIsPrinted() throws IOException {
        Payroll payroll = PayslipDocumentFixtures.cerrado(PayslipDocumentFixtures.emp000001());
        String texto = textoDe(pdfDe(payroll));

        assertTrue(texto.contains("ESP/INTERNAL/EMP000001/202609/NORMAL/2"),
                "La clave de negocio entera, presencia incluida: EMP000001 es un readmitido y su "
                        + "recibo vive en la presencia 2, no en la 1");
        assertTrue(texto.contains("Presencia: 2"), "La presencia, deletreada");
        assertTrue(texto.contains("Ejecución: 1"), "La ejecucion que produjo el recibo");
        assertTrue(texto.contains("20/09/2026 18:04"), "Cuando se calculo");
        assertTrue(texto.contains("GRAPH 1.0"), "Con que motor y con que version");
        assertTrue(texto.contains("EMP000001"), "Y la matricula de quien cobra");
    }

    /**
     * Criterio 3, la mitad que se ve en el papel: un recibo no definitivo lo dice.
     *
     * <p>La marca va en diagonal y en todas las paginas, no en un pie discreto: un recibo no
     * definitivo se puede volver a calcular, y entonces este papel deja de valer. Eso tiene que
     * verse desde el otro lado de una mesa.
     */
    @Test
    void aPayslipThatIsNotDefinitiveSaysSo() throws IOException {
        String borrador = textoDe(pdfDe(PayslipDocumentFixtures.emp000001()));
        assertTrue(borrador.contains("BORRADOR"), "La marca tiene que estar y tiene que verse");
        assertTrue(borrador.contains("no es el recibo definitivo"),
                "Y tiene que decir que significa, no solo gritar una palabra");

        String definitivo = textoDe(pdfDe(PayslipDocumentFixtures.cerrado(PayslipDocumentFixtures.emp000001())));
        assertFalse(definitivo.contains("BORRADOR"),
                "El documento que se entrega no lleva marca de borrador");
    }

    private byte[] pdfDe(Payroll payroll) {
        return renderizador.render(contenido.contentOf(
                payroll, PayslipDocumentFixtures.seccionesDelModeloOficial()));
    }
}

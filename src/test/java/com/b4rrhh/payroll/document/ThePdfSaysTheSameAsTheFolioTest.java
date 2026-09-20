package com.b4rrhh.payroll.document;

import com.b4rrhh.payroll.application.service.PayrollSnapshotProfiles;
import com.b4rrhh.payroll.document.application.service.PayslipDocumentContentFactory;
import com.b4rrhh.payroll.document.application.service.PayslipNumbers;
import com.b4rrhh.payroll.document.infrastructure.pdf.PdfBoxPayslipDocumentRenderer;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.infrastructure.web.assembler.PayrollResponseAssembler;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollConceptResponse;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El PDF dice lo mismo que el folio ({@code backend#112}, criterios 5 y 7).
 *
 * <h2>Por que se compara entre dos salidas y no contra una lista escrita a mano</h2>
 *
 * <p>Es el patron del censo del {@code backend#94}, un nivel mas arriba: alli se comparaban dos
 * capas, aqui dos <b>salidas</b> del mismo recibo. Una lista de quince lineas esperadas copiada en
 * el test seria verde el dia que se escribe y dejaria de significar nada al siguiente: lo que hay
 * que defender no es que el PDF diga «Salario base», es que <b>diga lo que dice la pantalla</b>,
 * incluido el dia que la pantalla cambie.
 *
 * <p>Por eso el «antes» de la comparacion es {@code PayrollResponse}, que es exactamente lo que el
 * folio pinta, y el «despues» es el texto extraido del PDF ya generado. No se comprueba la llamada
 * que genera el PDF: se lee el PDF.
 *
 * <p>Esta comparacion es tambien lo que obligo a que los numeros se escriban en un solo sitio. El
 * primer intento uso el {@code NumberFormat} de Java por defecto y la base de cotizacion salio
 * {@code 1.323,00} en el papel y {@code 1323,00} en la pantalla: el mismo numero, dos textos. El
 * español agrupa los miles a partir de cinco digitos y {@code DecimalFormat} no lo sabe.
 */
class ThePdfSaysTheSameAsTheFolioTest {

    private final PayrollSnapshotProfiles profiles = new PayrollSnapshotProfiles(new ObjectMapper());
    private final PayrollResponseAssembler folio = new PayrollResponseAssembler(profiles);
    private final PayslipDocumentContentFactory contenido = new PayslipDocumentContentFactory(profiles);
    private final PdfBoxPayslipDocumentRenderer renderizador = new PdfBoxPayslipDocumentRenderer();

    /** Criterio 5: linea a linea, el papel y la pantalla dicen lo mismo. */
    @Test
    void everyLineOfTheScreenIsOnThePaper() throws IOException {
        // El recibo cerrado, y no el borrador: la marca de borrador va en diagonal sobre la
        // pagina y el extractor de texto la entrelaza con las filas. Lo que se compara aqui es el
        // documento que se entrega.
        Payroll payroll = PayslipDocumentFixtures.cerrado(PayslipDocumentFixtures.emp000001());
        PayrollResponse pantalla = folio.toResponse(payroll, false);
        List<String> papel = lineasDe(pdfDe(payroll));

        for (PayrollConceptResponse concept : pantalla.concepts()) {
            if ("NET_PAY".equals(concept.conceptNatureCode())) {
                // El liquido no es una fila de tabla en ninguna de las dos salidas: se pinta como
                // la linea de cierre del recibo, con su nombre y su importe y sin las columnas de
                // cantidad y tarifa, que es como sale en una nomina de verdad. Lo comprueba
                // theNetPayIsTheOneTheScreenShows.
                continue;
            }
            String esperada = filaDe(concept);
            assertTrue(papel.stream().anyMatch(linea -> linea.contains(esperada)),
                    "El PDF no dice lo que dice el folio en la linea "
                            + concept.lineNumber() + ": «" + esperada + "»\nEl PDF dice:\n"
                            + String.join("\n", papel));
        }
    }

    /** Criterio 5: y no dice ninguna linea de mas. */
    @Test
    void thePaperHasNoLineTheScreenDoesNotHave() throws IOException {
        Payroll payroll = PayslipDocumentFixtures.cerrado(PayslipDocumentFixtures.emp000001());
        PayrollResponse pantalla = folio.toResponse(payroll, false);
        List<String> papel = lineasDe(pdfDe(payroll));

        long filasDelPapel = papel.stream()
                .filter(linea -> linea.startsWith("202609 "))
                .count();
        assertEquals(pantalla.concepts().size() - 1, filasDelPapel,
                "El liquido no se pinta como fila de tabla, asi que el papel tiene que traer "
                        + "una fila menos que lineas tiene el recibo");
    }

    /** Criterio 5: los cinco bloques, con su nombre y en el orden que declara el catalogo. */
    @Test
    void theFiveBlocksAreThereInTheDeclaredOrder() throws IOException {
        String texto = textoDe(pdfDe(PayslipDocumentFixtures.cerrado(PayslipDocumentFixtures.emp000001())));

        List<String> bloques = List.of(
                "Devengos",
                "Deducciones",
                "Liquido total a percibir",
                "Determinacion de las bases de cotizacion",
                "Aportacion empresarial");

        int anterior = -1;
        for (String bloque : bloques) {
            int donde = texto.indexOf(bloque);
            assertTrue(donde > anterior,
                    "El bloque «" + bloque + "» falta o sale fuera de orden en el PDF");
            anterior = donde;
        }
    }

    /** Criterio 5: el liquido del recibo de {@code EMP000001} es {@code 822,97 €}. */
    @Test
    void theNetPayIsTheOneTheScreenShows() throws IOException {
        Payroll payroll = PayslipDocumentFixtures.cerrado(PayslipDocumentFixtures.emp000001());
        PayrollConceptResponse liquido = folio.toResponse(payroll, false).concepts().stream()
                .filter(c -> "NET_PAY".equals(c.conceptNatureCode()))
                .findFirst()
                .orElseThrow();

        assertEquals("822,97", PayslipNumbers.valor(liquido.amount()));
        assertTrue(textoDe(pdfDe(payroll)).contains("822,97 €"),
                "El liquido tiene que salir con su moneda y ser el mismo que el del folio");
    }

    /**
     * Criterio 5: solo se totaliza el bloque cuyas lineas son sumandos.
     *
     * <p>Son tres reglas y las tres tienen que verse aqui, porque cada una tapa un numero falso
     * distinto:
     *
     * <ul>
     *   <li><b>Los devengos no se suman</b>: ya traen el 970, que <i>es</i> esa suma. Sumarlos
     *       otra vez daria el doble.</li>
     *   <li><b>La aportacion empresarial si</b>: el motor no la totaliza y el modelo oficial pide
     *       el total del recuadro. Es la unica cifra del PDF que no viene de una linea del
     *       recibo.</li>
     *   <li><b>Las bases no se suman nunca</b>, aunque no traigan total propio. La base de
     *       contingencias comunes y la base sujeta a retencion son dos magnitudes distintas del
     *       mismo mes: {@code 1323,00 + 1068,75 = 2391,75} no es ninguna magnitud.</li>
     * </ul>
     *
     * <p>Esa tercera es <b>la unica linea en la que el PDF y el folio no dicen lo mismo</b>, y es
     * a proposito: el folio la suma hoy —un defecto que la {@code V139} destapo al llenar un
     * recuadro que antes salia vacio— y el papel no lo hereda. Un numero sin significado en un
     * documento que se entrega es peor que una diferencia entre dos salidas.
     */
    @Test
    void onlyTheBlockWhoseLinesAreAddendsCarriesASum() throws IOException {
        String texto = textoDe(pdfDe(PayslipDocumentFixtures.cerrado(PayslipDocumentFixtures.emp000001())));

        assertTrue(texto.contains("Total aportacion empresarial"),
                "La aportacion empresarial es el bloque que el motor no totaliza y necesita el suyo");
        assertTrue(texto.contains("423,76"),
                "312,23 + 93,27 + 7,94 + 2,65 + 7,67 = 423,76");

        assertTrue(!texto.contains("Total devengos"),
                "Los devengos ya traen el 970: sumarlos otra vez daria el doble");
        assertTrue(!texto.contains("2391,75") && !texto.contains("2.391,75"),
                "Dos bases de cotizacion no se suman, y el folio si lo hace hoy" + texto);
    }

    /** Criterio 7: A4, y el recibo cabe en una hoja. Tambien el mas largo de la semilla. */
    @Test
    void theOfficialModelIsA4AndFitsInOnePage() throws IOException {
        for (Payroll payroll : List.of(
                PayslipDocumentFixtures.emp000001(),
                PayslipDocumentFixtures.emp000005())) {
            try (PDDocument documento = Loader.loadPDF(pdfDe(payroll))) {
                assertEquals(1, documento.getNumberOfPages(),
                        "El recibo de " + payroll.getEmployeeNumber() + " tiene que caber en una hoja");
                PDRectangle hoja = documento.getPage(0).getMediaBox();
                assertEquals(PDRectangle.A4.getWidth(), hoja.getWidth(), 0.5f);
                assertEquals(PDRectangle.A4.getHeight(), hoja.getHeight(), 0.5f);
            }
        }
    }

    // ---------------------------------------------------------------------

    private byte[] pdfDe(Payroll payroll) {
        return renderizador.render(contenido.contentOf(
                payroll, PayslipDocumentFixtures.seccionesDelModeloOficial()));
    }

    private static String filaDe(PayrollConceptResponse concept) {
        return String.join(" ",
                concept.originPeriodCode(),
                concept.conceptCode(),
                concept.conceptLabel(),
                PayslipNumbers.valor(concept.quantity()),
                PayslipNumbers.valor(concept.rate()),
                PayslipNumbers.valor(concept.amount()));
    }

    static String textoDe(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            PDFTextStripper lector = new PDFTextStripper();
            lector.setSortByPosition(true);
            return lector.getText(documento).replace(' ', ' ');
        }
    }

    static List<String> lineasDe(byte[] pdf) throws IOException {
        List<String> lineas = new ArrayList<>();
        for (String linea : textoDe(pdf).split("\\R")) {
            String normalizada = linea.trim().replaceAll("\\s+", " ");
            if (!normalizada.isEmpty()) {
                lineas.add(normalizada);
            }
        }
        return lineas;
    }
}

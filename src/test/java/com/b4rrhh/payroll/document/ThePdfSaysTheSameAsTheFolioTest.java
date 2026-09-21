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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * Criterio 2 del {@code backend#114}: <b>no hay ninguna cifra en el PDF que no venga de una
     * linea del recibo.</b>
     *
     * <p>Esta frase es la que se queria poder decir, y hasta hoy no se podia. El recuadro de
     * aportacion empresarial era el unico bloque con total en el modelo oficial al que el motor
     * no le daba uno, asi que la plantilla del papel lo sumaba —y este mismo test lo declaraba
     * como «la unica cifra del PDF que no viene de una linea del recibo»—. La {@code V141} le
     * da su concepto, el {@code 725}, y con eso la excepcion se retira.
     *
     * <p>Se comprueba por barrido y no por lista: se buscan <b>todas</b> las cifras con decimales
     * del texto del PDF y se exige que cada una este entre las que el recibo trae escritas. Una
     * lista de cifras esperadas volveria a dejar sitio para que manana alguien sume algo; esto
     * no, porque lo que afirma es una ausencia.
     *
     * <p>El recuadro de bases sigue sin total y sigue sin necesitarlo: {@code 1323,00 + 1068,75}
     * no es ninguna magnitud. Eso lo decidio el {@code frontend#79} para la pantalla y esto lo
     * sostiene para el papel —por eso tambien se comprueba que el {@code 2391,75} no aparece—.
     */
    @Test
    void noFigureOnThePaperComesFromOutsideTheReceipt() throws IOException {
        Payroll payroll = PayslipDocumentFixtures.cerrado(PayslipDocumentFixtures.emp000001());

        Set<String> lasQueTraeElRecibo = new HashSet<>();
        for (PayrollConceptResponse concept : folio.toResponse(payroll, false).concepts()) {
            lasQueTraeElRecibo.add(PayslipNumbers.valor(concept.quantity()));
            lasQueTraeElRecibo.add(PayslipNumbers.valor(concept.rate()));
            lasQueTraeElRecibo.add(PayslipNumbers.valor(concept.amount()));
        }

        String texto = textoDe(pdfDe(payroll));
        List<String> inventadas = new ArrayList<>();
        Matcher cifra = Pattern.compile("(?<![\\d.])\\d[\\d.]*,\\d+").matcher(texto);
        while (cifra.find()) {
            if (!lasQueTraeElRecibo.contains(cifra.group())) {
                inventadas.add(cifra.group());
            }
        }

        assertEquals(List.of(), inventadas,
                "El papel no puede escribir una cifra que no este en una linea del recibo."
                        + "\nEl PDF dice:\n"
                        + texto);
    }

    /** Y el total que antes sumaba el papel ahora sale porque el motor lo calculo. */
    @Test
    void theEmployerContributionTotalComesFromItsOwnLine() throws IOException {
        Payroll payroll = PayslipDocumentFixtures.cerrado(PayslipDocumentFixtures.emp000001());
        PayrollConceptResponse total = folio.toResponse(payroll, false).concepts().stream()
                .filter(c -> "TOTAL_EMPLOYER_CONTRIBUTION".equals(c.conceptNatureCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "el recibo tiene que traer el total de la aportacion empresarial como linea"));

        assertEquals("423,76", PayslipNumbers.valor(total.amount()),
                "312,23 + 93,27 + 7,94 + 2,65 + 7,67 = 423,76");

        String texto = textoDe(pdfDe(payroll));
        assertTrue(texto.contains("Total aportacion empresarial"),
                "y el papel lo tiene que pintar con el nombre que trae la linea");
        assertTrue(!texto.contains("2391,75") && !texto.contains("2.391,75"),
                "dos bases de cotizacion no se suman, ni en el papel ni en la pantalla");
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

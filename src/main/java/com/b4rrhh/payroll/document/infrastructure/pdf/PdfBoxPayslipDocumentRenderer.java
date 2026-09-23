package com.b4rrhh.payroll.document.infrastructure.pdf;

import com.b4rrhh.payroll.document.application.port.PayslipDocumentContent;
import com.b4rrhh.payroll.document.application.port.PayslipDocumentRenderer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * El recibo, dibujado en A4 ({@code backend#112}).
 *
 * <h2>Por que se dibuja por coordenadas y no se convierte un HTML</h2>
 *
 * <p>Porque el issue prohibe que el artefacto dependa de un motor de maquetacion ajeno, y un
 * HTML-a-PDF es exactamente eso un paso mas abajo: el mismo documento sale distinto segun la
 * version de la libreria que interpreta el CSS. Aqui la pagina la decide este fichero, y dos
 * ejecuciones del mismo codigo sobre el mismo recibo dan el mismo dibujo.
 *
 * <p>La eleccion de PDFBox tampoco es de gusto: es Apache-2.0 —sin condiciones que arrastrar a un
 * producto— y trae {@code PDFTextStripper}, que es lo que permite que los tests <b>lean</b> el PDF
 * generado en vez de dar por buena la llamada que lo genero.
 *
 * <h2>Lo que este fichero no decide</h2>
 *
 * <p>Nada de lo que se dice. Los numeros llegan escritos, los bloques llegan ordenados y los
 * nombres llegan elegidos, todo desde {@code PayslipDocumentContentFactory}. Si esas reglas
 * vivieran aqui, la unica manera de comprobarlas seria leyendo un PDF.
 *
 * <h2>Una pagina, pero sin recortar</h2>
 *
 * <p>El recibo tiene que caber en una hoja, y cabe: el mas largo de la semilla tiene
 * <b>treinta y dos lineas</b> y entra con sitio para cuatro mas. Eso no salio solo —con las tres
 * bases del {@code backend#121} se salia a una segunda pagina— y lo arreglo el
 * {@code backend#125} apretando el aire: interlineado, margen y las alturas de los rotulos. <b>No
 * se quito ninguna linea</b>, que son las del modelo oficial, ni se bajo el cuerpo de letra.
 *
 * <p>Aun asi hay salto de pagina, porque la alternativa a saltar no es «siempre una hoja», es
 * <b>perder lineas en silencio</b> el dia que un convenio traiga cuarenta conceptos. Un documento
 * que se entrega no puede terminar a mitad.
 */
@Component
public class PdfBoxPayslipDocumentRenderer implements PayslipDocumentRenderer {

    private static final String CONTENT_TYPE = "application/pdf";

    // --- La hoja ---------------------------------------------------------
    /**
     * El margen bajo de 42 a 36 ({@code backend#125}).
     *
     * <p>Seis puntos, que son los seis que le faltaban al recibo mas largo por arriba. El ancho
     * crece con el —las columnas se miden desde aqui— y eso le viene bien a la columna de
     * concepto, que es la que recorta.
     */
    private static final float MARGEN = 36f;
    private static final float DERECHA = PDRectangle.A4.getWidth() - MARGEN;
    private static final float ALTO = PDRectangle.A4.getHeight();
    /** Por debajo de aqui empieza el pie con la procedencia, y no se escribe encima. */
    private static final float SUELO = 104f;

    // --- Las columnas de una linea --------------------------------------
    private static final float COL_PERIODO = MARGEN;
    private static final float COL_CLAVE = MARGEN + 36f;
    private static final float COL_CONCEPTO = MARGEN + 70f;
    private static final float ANCHO_CONCEPTO = 250f;
    private static final float FIN_CANTIDAD = MARGEN + 373f;
    private static final float FIN_TARIFA = MARGEN + 428f;
    private static final float FIN_IMPORTE = DERECHA;

    // --- Alturas ---------------------------------------------------------
    /**
     * Las alturas apretadas para que el recibo mas largo del modelo oficial quepa en una hoja
     * ({@code backend#125}).
     *
     * <p>Venian de cuando el recibo mas largo tenia diecinueve lineas. Con las tres bases son
     * treinta y dos, y con las de antes —linea 12, rotulo 15, hueco 9— hacian falta 597 puntos
     * donde hay 537. <b>Aqui no se quita ninguna linea</b>: son las del modelo oficial. Lo que se
     * quita es aire.
     *
     * <p>El grueso lo pone el interlineado: dos puntos por linea, treinta y una veces. El cuerpo
     * de letra <b>no se toca</b> —8 y 6,8 puntos— porque un recibo de salarios se lee; lo que se
     * ajusta es el aire entre lineas, y 10 puntos para una tipografia de 8 es el interlineado
     * corriente de una tabla.
     */
    private static final float ALTO_LINEA = 10f;
    private static final float ALTO_CABECERA_TABLA = 11f;
    private static final float ALTO_ROTULO_BLOQUE = 13f;
    /** El rotulo de un apartado, mas bajo que el del bloque: es un escalon, no otro bloque. */
    private static final float ALTO_ROTULO_APARTADO = 11f;
    private static final float HUECO_ENTRE_BLOQUES = 7f;

    private static final float CUERPO = 8f;
    private static final float MENUDA = 6.8f;

    private final PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDFont negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    @Override
    public String contentType() {
        return CONTENT_TYPE;
    }

    @Override
    public byte[] render(PayslipDocumentContent content) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {

            Hoja hoja = new Hoja(document);
            cabecera(hoja, content);
            for (PayslipDocumentContent.Block block : content.blocks()) {
                bloque(hoja, block);
            }
            hoja.cerrar();

            pie(document, content.provenance());
            if (content.draft()) {
                marcaDeBorrador(document);
            }

            document.save(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se ha podido dibujar el recibo en PDF", e);
        }
    }

    // ---------------------------------------------------------------------
    // Cabecera: titulo, las dos cajas y los datos laborales
    // ---------------------------------------------------------------------

    private void cabecera(Hoja hoja, PayslipDocumentContent content) throws IOException {
        hoja.texto(negrita, 14f, MARGEN, hoja.bajar(18f), "Recibo de nómina");

        if (content.draft()) {
            hoja.color(0.72f, 0.16f, 0.16f);
            hoja.texto(negrita, 8.5f, MARGEN, hoja.bajar(13f),
                    "BORRADOR · documento provisional, no es el recibo definitivo");
            hoja.color(0f, 0f, 0f);
        }

        float arriba = hoja.bajar(8f);
        float anchoCaja = (DERECHA - MARGEN - 11f) / 2f;
        // 52 y no 56: la caja mas llena es la del trabajador, con nombre y cuatro detalles, y su
        // ultima linea cae a nueve puntos del borde de abajo. Cuatro puntos menos de aire y ni un
        // dato fuera (backend#125).
        float alto = 52f;
        caja(hoja, MARGEN, arriba - alto, anchoCaja, alto, content.company());
        caja(hoja, MARGEN + anchoCaja + 11f, arriba - alto, anchoCaja, alto, content.employee());
        hoja.situar(arriba - alto - 9f);

        datosLaborales(hoja, content.labor());
    }

    private void caja(Hoja hoja, float x, float y, float ancho, float alto,
                      PayslipDocumentContent.Party party) throws IOException {
        hoja.recuadro(x, y, ancho, alto);
        float linea = y + alto - 13f;
        hoja.texto(negrita, 9.5f, x + 7f, linea, recortar(party.name(), negrita, 9.5f, ancho - 14f));
        for (String detalle : party.details()) {
            // Nueve y no diez: la caja del trabajador lleva cuatro detalles -NIF, matricula y dos
            // lineas de domicilio- y con diez la ultima se apoyaba en la raya de abajo desde que
            // la caja bajo a 52 (backend#125).
            linea -= 9f;
            hoja.texto(normal, CUERPO, x + 7f, linea, recortar(detalle, normal, CUERPO, ancho - 14f));
        }
    }

    /**
     * El recuadro de datos laborales.
     *
     * <p>El convenio ocupa dos tercios y no un tercio, que es lo que habia al principio: el nombre
     * de la semilla —«Convenio colectivo del sector de grandes almacenes 99002405»— se cortaba en
     * «...de grand…», y un convenio a medias en un recibo no identifica ningun convenio.
     */
    private void datosLaborales(Hoja hoja, PayslipDocumentContent.LaborData labor) throws IOException {
        // 51 y no 54: la fila de abajo escribe su valor a 48,5 del borde de arriba, asi que este
        // es el minimo que la deja dentro con holgura (backend#125).
        float alto = 51f;
        float arriba = hoja.y();
        hoja.recuadro(MARGEN, arriba - alto, DERECHA - MARGEN, alto);
        hoja.color(0.35f, 0.35f, 0.35f);
        hoja.texto(negrita, MENUDA, MARGEN + 7f, arriba - 11f, "DATOS LABORALES");
        hoja.color(0f, 0f, 0f);

        float anchoCelda = (DERECHA - MARGEN - 14f) / 3f;
        celda(hoja, MARGEN + 7f, arriba - 21f, anchoCelda * 2f, "Convenio", labor.agreement());
        celda(hoja, MARGEN + 7f + anchoCelda * 2f, arriba - 21f, anchoCelda, "Categoría", labor.category());
        celda(hoja, MARGEN + 7f, arriba - 40f, anchoCelda, "Período de liquidación", labor.period());
        celda(hoja, MARGEN + 7f + anchoCelda, arriba - 40f, anchoCelda, "Centro de trabajo", labor.workCenter());
        celda(hoja, MARGEN + 7f + anchoCelda * 2f, arriba - 40f, anchoCelda, "Antigüedad", labor.seniority());

        hoja.situar(arriba - alto - HUECO_ENTRE_BLOQUES);
    }

    private void celda(Hoja hoja, float x, float y, float ancho, String rotulo, String valor)
            throws IOException {
        hoja.color(0.42f, 0.42f, 0.42f);
        hoja.texto(normal, MENUDA, x, y, rotulo);
        hoja.color(0f, 0f, 0f);
        hoja.texto(negrita, CUERPO, x, y - 8.5f, recortar(valor, negrita, CUERPO, ancho - 6f));
    }

    // ---------------------------------------------------------------------
    // Los bloques del modelo oficial
    // ---------------------------------------------------------------------

    private void bloque(Hoja hoja, PayslipDocumentContent.Block block) throws IOException {
        if (block.closing()) {
            liquido(hoja, block);
            return;
        }

        hoja.reservar(ALTO_ROTULO_BLOQUE + ALTO_CABECERA_TABLA + ALTO_LINEA);
        rotulo(hoja, block.label());
        cabeceraDeTabla(hoja);

        for (PayslipDocumentContent.Group group : block.groups()) {
            // El rotulo del apartado, cuando lo hay. Un bloque sin apartados trae un unico grupo
            // sin rotulo y esto no pinta nada, que es como se imprimen los devengos (backend#121).
            if (group.label() != null) {
                if (hoja.reservar(ALTO_ROTULO_APARTADO + ALTO_LINEA)) {
                    cabeceraDeTabla(hoja);
                }
                rotuloDeApartado(hoja, group.label());
            }
            for (PayslipDocumentContent.Line line : group.lines()) {
                if (hoja.reservar(ALTO_LINEA)) {
                    cabeceraDeTabla(hoja);
                }
                float y = hoja.bajar(ALTO_LINEA);
                hoja.texto(normal, MENUDA, COL_PERIODO, y, line.period());
                hoja.texto(normal, MENUDA, COL_CLAVE, y, line.code());
                hoja.texto(normal, CUERPO, COL_CONCEPTO, y,
                        recortar(line.label(), normal, CUERPO, ANCHO_CONCEPTO));
                hoja.derecha(normal, CUERPO, FIN_CANTIDAD, y, line.quantity());
                hoja.derecha(normal, CUERPO, FIN_TARIFA, y, line.rate());
                hoja.derecha(normal, CUERPO, FIN_IMPORTE, y, line.amount());
            }
        }

        hoja.situar(hoja.y() - HUECO_ENTRE_BLOQUES);
    }

    /**
     * El rotulo de un apartado: en negrita y sin fondo, para que se lea como un escalon dentro
     * del bloque y no como un bloque nuevo. El fondo gris es lo que distingue a los bloques.
     */
    private void rotuloDeApartado(Hoja hoja, String label) throws IOException {
        float y = hoja.bajar(ALTO_ROTULO_APARTADO);
        hoja.texto(negrita, CUERPO, COL_CONCEPTO, y, label);
    }

    private void rotulo(Hoja hoja, String label) throws IOException {
        float y = hoja.bajar(ALTO_ROTULO_BLOQUE);
        hoja.color(0.91f, 0.91f, 0.91f);
        hoja.relleno(MARGEN, y - 3f, DERECHA - MARGEN, ALTO_ROTULO_BLOQUE);
        hoja.color(0f, 0f, 0f);
        hoja.texto(negrita, 9f, MARGEN + 5f, y, label);
    }

    private void cabeceraDeTabla(Hoja hoja) throws IOException {
        float y = hoja.bajar(ALTO_CABECERA_TABLA);
        hoja.color(0.42f, 0.42f, 0.42f);
        hoja.texto(normal, MENUDA, COL_PERIODO, y, "Período");
        hoja.texto(normal, MENUDA, COL_CLAVE, y, "Clave");
        hoja.texto(normal, MENUDA, COL_CONCEPTO, y, "Concepto");
        hoja.derecha(normal, MENUDA, FIN_CANTIDAD, y, "Cantidad");
        hoja.derecha(normal, MENUDA, FIN_TARIFA, y, "Tarifa/Base");
        hoja.derecha(normal, MENUDA, FIN_IMPORTE, y, "Importe");
        hoja.color(0f, 0f, 0f);
        hoja.raya(MARGEN, y - 3f, DERECHA);
    }

    /** El liquido se pinta como la linea de cierre del recibo, no como una tabla de una fila. */
    private void liquido(Hoja hoja, PayslipDocumentContent.Block block) throws IOException {
        hoja.reservar(26f);
        float y = hoja.bajar(24f);
        hoja.color(0.13f, 0.20f, 0.33f);
        hoja.relleno(MARGEN, y - 6f, DERECHA - MARGEN, 24f);
        hoja.color(1f, 1f, 1f);
        hoja.texto(negrita, 10.5f, MARGEN + 7f, y, block.label());
        String importe = block.lines().isEmpty() ? "—" : block.lines().get(0).amount();
        hoja.derecha(negrita, 12f, FIN_IMPORTE - 7f, y, importe + " €");
        hoja.color(0f, 0f, 0f);
        hoja.situar(hoja.y() - HUECO_ENTRE_BLOQUES);
    }

    // ---------------------------------------------------------------------
    // El pie: de donde sale este papel
    // ---------------------------------------------------------------------

    private void pie(PDDocument document, PayslipDocumentContent.Provenance provenance)
            throws IOException {
        PDPage ultima = document.getPage(document.getNumberOfPages() - 1);
        try (PDPageContentStream cs = new PDPageContentStream(
                document, ultima, PDPageContentStream.AppendMode.APPEND, true, true)) {
            cs.setStrokingColor(0.72f, 0.72f, 0.72f);
            cs.setLineWidth(0.5f);
            cs.moveTo(MARGEN, 92f);
            cs.lineTo(DERECHA, 92f);
            cs.stroke();

            cs.setNonStrokingColor(0.35f, 0.35f, 0.35f);
            escribir(cs, negrita, MENUDA, MARGEN, 80f, "PROCEDENCIA DE ESTE DOCUMENTO");
            escribir(cs, normal, MENUDA, MARGEN, 70f, "Clave del recibo: " + provenance.businessKey());
            escribir(cs, normal, MENUDA, MARGEN, 61f, "Presencia: " + provenance.presence()
                    + "     Ejecución: " + provenance.runId()
                    + "     Calculado el: " + provenance.calculatedAt()
                    + "     Motor: " + provenance.engine());
            escribir(cs, normal, MENUDA, MARGEN, 52f,
                    "Con esa clave, quien tenga este papel encuentra el recibo en el sistema y ve de dónde sale cada línea.");
        }
    }

    /**
     * La marca de que esto no es el documento entregado ({@code backend#112}).
     *
     * <p>Va en todas las paginas y en diagonal, no en un pie discreto: una marca que haya que
     * buscar no marca nada. Un recibo no definitivo se puede volver a calcular y entonces este
     * papel deja de valer, y eso tiene que verse desde el otro lado de una mesa.
     *
     * <p>Y va <b>debajo</b> del contenido ({@code PREPEND}), no encima. Encima tapaba media
     * columna de conceptos —«Base de cotizacion por co…encia…unes»—, y un borrador sigue siendo un
     * documento que alguien tiene que poder leer para decidir si lo cierra. Una marca que impide
     * comprobar las cifras no avisa: estorba.
     */
    private void marcaDeBorrador(PDDocument document) throws IOException {
        for (PDPage page : document.getPages()) {
            try (PDPageContentStream cs = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.PREPEND, true, true)) {
                // El estado grafico se guarda y se devuelve. Sin esto, el gris de la marca se
                // cuela en el contenido —que se dibuja despues, en su propio flujo— y el titulo
                // del recibo sale del color de la marca de agua.
                cs.saveGraphicsState();
                cs.setNonStrokingColor(0.88f, 0.84f, 0.84f);
                cs.beginText();
                cs.setFont(negrita, 58f);
                cs.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(38), 78f, 230f));
                cs.showText(limpiar("BORRADOR"));
                cs.endText();
                cs.restoreGraphicsState();
            }
        }
    }

    // ---------------------------------------------------------------------
    // Utiles de dibujo
    // ---------------------------------------------------------------------

    /** La hoja en curso y por donde va el lapiz. Salta de pagina sola cuando se acaba el sitio. */
    private final class Hoja {

        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream cs;
        private float y;

        private Hoja(PDDocument document) throws IOException {
            this.document = document;
            nuevaPagina();
        }

        private void nuevaPagina() throws IOException {
            if (cs != null) {
                cs.close();
            }
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            cs = new PDPageContentStream(document, page);
            y = ALTO - MARGEN;
        }

        /** Reserva sitio; devuelve true si para ello ha habido que empezar otra hoja. */
        private boolean reservar(float alto) throws IOException {
            if (y - alto < SUELO) {
                nuevaPagina();
                return true;
            }
            return false;
        }

        private float bajar(float alto) {
            y -= alto;
            return y;
        }

        private void situar(float nuevaY) {
            y = nuevaY;
        }

        private float y() {
            return y;
        }

        private void cerrar() throws IOException {
            cs.close();
        }

        private void color(float r, float g, float b) throws IOException {
            cs.setNonStrokingColor(r, g, b);
        }

        private void texto(PDFont font, float size, float x, float yy, String value)
                throws IOException {
            escribir(cs, font, size, x, yy, value);
        }

        private void derecha(PDFont font, float size, float finX, float yy, String value)
                throws IOException {
            String limpio = limpiar(value);
            escribir(cs, font, size, finX - ancho(font, size, limpio), yy, limpio);
        }

        private void raya(float x1, float yy, float x2) throws IOException {
            cs.setStrokingColor(0.72f, 0.72f, 0.72f);
            cs.setLineWidth(0.5f);
            cs.moveTo(x1, yy);
            cs.lineTo(x2, yy);
            cs.stroke();
        }

        private void recuadro(float x, float yy, float ancho, float alto) throws IOException {
            cs.setStrokingColor(0.72f, 0.72f, 0.72f);
            cs.setLineWidth(0.5f);
            cs.addRect(x, yy, ancho, alto);
            cs.stroke();
        }

        private void relleno(float x, float yy, float ancho, float alto) throws IOException {
            cs.addRect(x, yy, ancho, alto);
            cs.fill();
        }
    }

    private static void escribir(PDPageContentStream cs, PDFont font, float size,
                                 float x, float y, String value) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(limpiar(value));
        cs.endText();
    }

    private static float ancho(PDFont font, float size, String value) throws IOException {
        return font.getStringWidth(value) / 1000f * size;
    }

    /** Recorta con puntos suspensivos lo que no quepa. Nada se sale de su columna. */
    private static String recortar(String value, PDFont font, float size, float ancho)
            throws IOException {
        String limpio = limpiar(value);
        if (ancho(font, size, limpio) <= ancho) {
            return limpio;
        }
        StringBuilder corto = new StringBuilder(limpio);
        while (corto.length() > 1 && ancho(font, size, corto + "…") > ancho) {
            corto.deleteCharAt(corto.length() - 1);
        }
        return corto + "…";
    }

    /**
     * Deja solo lo que las tipografias estandar saben dibujar.
     *
     * <p>Helvetica va con {@code WinAnsiEncoding}: cubre el español entero y el euro, y no cubre
     * todo Unicode. Un nombre con un caracter de fuera reventaria el dibujo <b>entero</b> con una
     * excepcion, o sea que un empleado con un apellido raro se quedaria sin recibo. Sustituirlo por
     * un interrogante deja un hueco que se ve; que no salga el documento, no.
     */
    private static String limpiar(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder limpio = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            if (c == '\n' || c == '\r' || c == '\t') {
                limpio.append(' ');
            } else if (c < 0x20 || (c >= 0x7F && c <= 0x9F)) {
                limpio.append(' ');
            } else if (c <= 0xFF || EXTRAS_WIN_ANSI.indexOf(c) >= 0) {
                limpio.append(c);
            } else {
                limpio.append('?');
            }
        }
        return limpio.toString();
    }

    /** Lo que WinAnsi añade por encima de Latin-1: el euro, las comillas tipograficas y compañia. */
    private static final String EXTRAS_WIN_ANSI =
            "€‚ƒ„…†‡ˆ‰Š‹ŒŽ"
                    + "‘’“”•–—˜™š›œ"
                    + "žŸ";

}

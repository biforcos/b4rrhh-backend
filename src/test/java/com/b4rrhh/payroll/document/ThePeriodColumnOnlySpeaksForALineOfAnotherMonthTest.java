package com.b4rrhh.payroll.document;

import com.b4rrhh.payroll.document.application.port.PayslipDocumentContent;
import com.b4rrhh.payroll.document.application.service.PayslipDocumentContentFactory;
import com.b4rrhh.payroll.document.infrastructure.pdf.PdfBoxPayslipDocumentRenderer;
import com.b4rrhh.payroll.application.service.PayrollSnapshotProfiles;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La columna de período sólo habla cuando la línea es de otro mes ({@code backend#138}).
 *
 * <p>Es la regla de los programas de nómina, y la que la pantalla adopta en {@code frontend#89}: con
 * veinte filas iguales la columna no informa; con una distinta, salta a la vista. Por eso las líneas del
 * propio mes van en blanco y las de atraso llevan su mes, escrito para una persona —{@code 08/2026}— y no
 * para una máquina.
 *
 * <p>Y el literal de la línea de atraso es <b>el del concepto, tal cual</b>: el origen va en su columna, y
 * decirlo también en el nombre era decirlo dos veces.
 */
class ThePeriodColumnOnlySpeaksForALineOfAnotherMonthTest {

    private final PayslipDocumentContentFactory contenido =
            new PayslipDocumentContentFactory(new PayrollSnapshotProfiles(new ObjectMapper()));
    private final PdfBoxPayslipDocumentRenderer renderizador = new PdfBoxPayslipDocumentRenderer();

    @Test
    void unaLineaPropiaNoImprimePeriodoYUnaDeAtrasoSi() {
        List<PayslipDocumentContent.Line> lineas = lineasDe(conAtraso());

        List<PayslipDocumentContent.Line> propias = lineas.stream()
                .filter(l -> !"08/2026".equals(l.period())).toList();
        List<PayslipDocumentContent.Line> deAtraso = lineas.stream()
                .filter(l -> "08/2026".equals(l.period())).toList();

        assertEquals(1, deAtraso.size(), "la línea de atraso lleva su mes, en MM/AAAA: " + lineas);
        for (PayslipDocumentContent.Line propia : propias) {
            assertEquals("", propia.period(),
                    "una línea del propio mes no imprime período: " + propia);
        }
    }

    @Test
    void enElPapelElAtrasoSeDistingueSoloPorSuColumna() throws IOException {
        String texto = ThePdfSaysTheSameAsTheFolioTest.textoDe(renderizador.render(contenido.contentOf(
                conAtraso(), PayslipDocumentFixtures.seccionesDelModeloOficial())));

        assertTrue(texto.contains("08/2026 101 Salario base"),
                "la línea de atraso: su mes, su clave y el literal del concepto tal cual\n" + texto);
        assertFalse(texto.contains("202609 101"),
                "y el período del propio recibo ya no se imprime en cada fila\n" + texto);
    }

    private static Payroll conAtraso() {
        return PayslipDocumentFixtures.cerrado(
                PayslipDocumentFixtures.conUnAtraso(PayslipDocumentFixtures.emp000001(), "202608"));
    }

    private List<PayslipDocumentContent.Line> lineasDe(Payroll payroll) {
        return contenido.contentOf(payroll, PayslipDocumentFixtures.seccionesDelModeloOficial())
                .blocks().stream()
                .flatMap(b -> b.groups().stream())
                .flatMap(g -> g.lines().stream())
                .toList();
    }
}

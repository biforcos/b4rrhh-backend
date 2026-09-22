package com.b4rrhh.payroll.document;

import com.b4rrhh.payroll.application.service.PayrollSnapshotProfiles;
import com.b4rrhh.payroll.document.application.service.PayslipDocumentContentFactory;
import com.b4rrhh.payroll.document.application.usecase.GetPayslipDocumentCommand;
import com.b4rrhh.payroll.document.application.usecase.GetPayslipDocumentService;
import com.b4rrhh.payroll.document.domain.exception.PayslipDocumentNotArchivedException;
import com.b4rrhh.payroll.document.domain.model.PayslipDocument;
import com.b4rrhh.payroll.document.domain.port.PayslipDocumentStorage;
import com.b4rrhh.payroll.document.infrastructure.pdf.PdfBoxPayslipDocumentRenderer;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import com.b4rrhh.payroll_engine.concept.domain.model.PayslipSection;
import com.b4rrhh.payroll_engine.concept.domain.port.PayslipSectionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Servir un definitivo lo saca del almacen; servir un borrador lo dibuja y no lo guarda
 * ({@code backend#112}, criterios 1, 2 y 3).
 *
 * <h2>El criterio 2, que es el que de verdad prueba algo</h2>
 *
 * <p>Es el criterio 3 del {@code backend#109} un nivel mas arriba. Alli se cambiaba un literal del
 * catalogo y se miraba la <b>linea</b> de un recibo ya calculado; aqui se cambia y se mira el
 * <b>documento</b>. Y se miran los dos lados, porque uno solo no distingue nada:
 *
 * <ul>
 *   <li>El cerrado sigue diciendo el literal viejo. No porque se genere igual: porque no se
 *       genera, se lee del almacen.</li>
 *   <li>El calculado, recalculado y vuelto a pedir, dice el nuevo. Todavia no se ha entregado
 *       nada.</li>
 * </ul>
 *
 * <p><b>Si las dos salidas se parecieran, esto no estaria hecho.</b> Seria igual de verde con un
 * documento que se regenerase en cada peticion, que es justo la solucion que el issue prohibe.
 */
class ServingADefinitiveDocumentNeverRegeneratesItTest {

    private static final GetPayslipDocumentCommand EMP000001 = new GetPayslipDocumentCommand(
            "ESP", "INTERNAL", "EMP000001", "202609", "NORMAL", 2);

    private final AlmacenDeMentira almacen = new AlmacenDeMentira();
    private final RepositorioDeMentira repositorio = new RepositorioDeMentira();

    private final GetPayslipDocumentService servicio = new GetPayslipDocumentService(
            repositorio,
            almacen,
            new PayslipDocumentContentFactory(new PayrollSnapshotProfiles(new ObjectMapper())),
            new PdfBoxPayslipDocumentRenderer(),
            seccionesDelCatalogo());

    /**
     * Criterio 2: el cerrado dice el literal viejo y el calculado dice el nuevo.
     *
     * <p>«Cambiar un literal en el catalogo» aqui es recalcular el recibo con otro nombre en la
     * linea, que es lo que un cambio de catalogo produce: el literal viaja congelado en la linea
     * desde el {@code backend#109}, asi que un recibo que no se recalcula no lo ve cambiar. El
     * cerrado no se recalcula <b>y ademas</b> no se regenera; el calculado hace las dos cosas.
     */
    @Test
    void theClosedOneKeepsTheOldLabelAndTheCalculatedOneTakesTheNewOne() {
        // Se cierra un recibo que dice «Salario base». Su documento queda escrito.
        Payroll cerrado = PayslipDocumentFixtures.cerrado(
                PayslipDocumentFixtures.emp000001ConLiteral("Salario base"));
        almacen.objetos.put("ESP/202609/NORMAL/INTERNAL/EMP000001-2.pdf",
                new PdfBoxPayslipDocumentRenderer().render(
                        new PayslipDocumentContentFactory(new PayrollSnapshotProfiles(new ObjectMapper()))
                                .contentOf(cerrado, PayslipDocumentFixtures.seccionesDelModeloOficial())));

        // Alguien renombra el concepto en el catalogo y todo lo que se recalcula coge el nombre
        // nuevo. El recibo cerrado no se recalcula.
        repositorio.recibo = cerrado;
        String delCerrado = texto(servicio.getDocument(EMP000001));

        repositorio.recibo = PayslipDocumentFixtures.emp000001ConLiteral("Salario basico mensual");
        String delCalculado = texto(servicio.getDocument(EMP000001));

        assertTrue(delCerrado.contains("Salario base") && !delCerrado.contains("Salario basico mensual"),
                "El documento entregado dice lo que decia cuando se entrego");
        assertTrue(delCalculado.contains("Salario basico mensual"),
                "Y el que todavia no se ha entregado enseña lo nuevo");
    }

    /** Criterio 1: pedirlo dos veces devuelve los mismos bytes, porque es el mismo objeto. */
    @Test
    void askingTwiceForADefinitiveReturnsTheSameBytes() {
        Payroll cerrado = PayslipDocumentFixtures.cerrado(PayslipDocumentFixtures.emp000001());
        repositorio.recibo = cerrado;
        almacen.objetos.put("ESP/202609/NORMAL/INTERNAL/EMP000001-2.pdf", new byte[]{'%', 'P', 'D', 'F'});

        PayslipDocument primera = servicio.getDocument(EMP000001);
        PayslipDocument segunda = servicio.getDocument(EMP000001);

        assertArrayEquals(primera.content(), segunda.content());
        assertTrue(primera.definitive());
        assertEquals("recibo-EMP000001-202609.pdf", primera.fileName());
    }

    /** Criterio 3: un no definitivo se dibuja al vuelo, se marca, y no deja nada en el almacen. */
    @Test
    void aDraftIsRenderedOnDemandAndNothingIsWritten() {
        repositorio.recibo = PayslipDocumentFixtures.emp000001();

        PayslipDocument documento = servicio.getDocument(EMP000001);

        assertFalse(documento.definitive());
        assertEquals("recibo-EMP000001-202609-borrador.pdf", documento.fileName(),
                "El nombre del fichero lo dice: sale del navegador y acaba en una carpeta, "
                        + "lejos de la pantalla que sabia en que estado estaba el recibo");
        assertTrue(texto(documento).contains("BORRADOR"));
        assertTrue(almacen.objetos.isEmpty(), "El almacen guarda documentos, y un borrador no lo es");
    }

    /** Un NOT_VALID tambien se sirve: es la pantalla la que decide no ofrecer el gesto. */
    @Test
    void anInvalidatedPayslipStillHasAPaperToLookAt() {
        repositorio.recibo = PayslipDocumentFixtures.emp000001().invalidate("REVISION");

        PayslipDocument documento = servicio.getDocument(EMP000001);

        assertFalse(documento.definitive());
        assertTrue(texto(documento).contains("BORRADOR"));
    }

    /**
     * Un cerrado sin documento se dice, no se tapa regenerandolo.
     *
     * <p>Desde este issue no puede pasar —cerrar y archivar son el mismo acto—, pero los recibos
     * cerrados antes existen. Regenerar les daria un papel parecido al que tienen y no el mismo, y
     * nadie sabria que no es el mismo.
     */
    @Test
    void aClosedPayslipWithoutItsDocumentSaysSoInsteadOfMakingANewOne() {
        repositorio.recibo = PayslipDocumentFixtures.cerrado(PayslipDocumentFixtures.emp000001());

        PayslipDocumentNotArchivedException fallo = assertThrows(
                PayslipDocumentNotArchivedException.class,
                () -> servicio.getDocument(EMP000001));

        assertTrue(fallo.getMessage().contains("no se regenera")
                        || fallo.getMessage().contains("No se regenera"),
                "El error dice por que no hay un PDF de repuesto: " + fallo.getMessage());
        assertTrue(almacen.objetos.isEmpty(), "Y no escribe nada de paso");
    }

    // ---------------------------------------------------------------------

    private static String texto(PayslipDocument documento) {
        try {
            return ThePdfSaysTheSameAsTheFolioTest.textoDe(documento.content());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class AlmacenDeMentira implements PayslipDocumentStorage {
        private final Map<String, byte[]> objetos = new HashMap<>();

        @Override
        public void store(String objectKey, byte[] content, String contentType) {
            objetos.put(objectKey, content);
        }

        @Override
        public Optional<byte[]> retrieve(String objectKey) {
            return Optional.ofNullable(objetos.get(objectKey));
        }
    }

    private static final class RepositorioDeMentira implements PayrollRepository {
        private Payroll recibo;

        @Override
        public Optional<Payroll> findByBusinessKey(String ruleSystemCode, String employeeTypeCode,
                                                   String employeeNumber, String payrollPeriodCode,
                                                   String payrollTypeCode, Integer presenceNumber) {
            return Optional.ofNullable(recibo);
        }

        @Override
        public List<Payroll> findByFilters(String ruleSystemCode, String payrollPeriodCode,
                                           String employeeNumber, PayrollStatus status) {
            return List.of();
        }

        @Override
        public Payroll save(Payroll payroll) {
            throw new UnsupportedOperationException("Servir un documento no escribe nada");
        }

        @Override
        public void deleteById(Long id) {
        }

        @Override
        public void flush() {
        }
    }

    private static PayslipSectionRepository seccionesDelCatalogo() {
        return new PayslipSectionRepository() {
            @Override
            public List<PayslipSection> findByRuleSystemCode(String ruleSystemCode) {
                return PayslipDocumentFixtures.seccionesDelModeloOficial();
            }

            @Override
            public Map<String, String> findSectionCodeByNature(String ruleSystemCode) {
                return Map.of();
            }

            @Override
            public Map<String, String> findSubsectionCodeByConcept(String ruleSystemCode) {
                return Map.of();
            }
        };
    }
}

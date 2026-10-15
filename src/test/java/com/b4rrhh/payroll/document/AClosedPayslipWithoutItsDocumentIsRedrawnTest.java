package com.b4rrhh.payroll.document;

import com.b4rrhh.payroll.application.service.PayrollSnapshotProfiles;
import com.b4rrhh.payroll.document.application.service.PayslipDocumentArchiver;
import com.b4rrhh.payroll.document.application.service.PayslipDocumentContentFactory;
import com.b4rrhh.payroll.document.application.usecase.GetPayslipDocumentCommand;
import com.b4rrhh.payroll.document.application.usecase.GetPayslipDocumentService;
import com.b4rrhh.payroll.document.domain.model.PayslipDocument;
import com.b4rrhh.payroll.document.domain.port.PayslipDocumentStorage;
import com.b4rrhh.payroll.document.infrastructure.pdf.PdfBoxPayslipDocumentRenderer;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import com.b4rrhh.payroll.domain.port.PayrollSearchPage;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Un recibo cerrado sin documento lo regenera de sus filas definitivas ({@code backend#174}).
 *
 * <p>Es lo que les pasa a los 7.025 cerrados de la semilla en la demo: el cierre en masa archivo
 * su PDF en el MinIO de la maquina que fabrico la semilla, y el volcado solo lleva la base. Hasta
 * aqui eso era un 409 que decia la verdad y dejaba al usuario sin papel.
 *
 * <p>Regenerar es licito <b>porque sale el mismo documento</b>, y eso es lo que se comprueba byte a
 * byte: se cierra, se pierde el PDF, se vuelve a pedir, y lo que llega es lo que se archivo. Las
 * dos condiciones de que sea asi viven en el commit anterior: el {@code /ID} del PDF ya no sale
 * del reloj ({@link ThePdfIsTheSameEveryTimeItIsDrawnTest}) y los bloques del recibo solo crecen
 * ({@link ThePayslipBlocksOnlyGrowTest}).
 */
class AClosedPayslipWithoutItsDocumentIsRedrawnTest {

    private static final String CLAVE = "ESP/202609/NORMAL/INTERNAL/EMP000001-2.pdf";
    private static final GetPayslipDocumentCommand EMP000001 = new GetPayslipDocumentCommand(
            "ESP", "INTERNAL", "EMP000001", "202609", "NORMAL", 2);

    private final AlmacenDeMentira almacen = new AlmacenDeMentira();
    private final RepositorioDeMentira repositorio = new RepositorioDeMentira();
    private final PayslipDocumentContentFactory contenido =
            new PayslipDocumentContentFactory(new PayrollSnapshotProfiles(new ObjectMapper()));
    private final PdfBoxPayslipDocumentRenderer renderer = new PdfBoxPayslipDocumentRenderer();
    private final PayslipDocumentArchiver archivador =
            new PayslipDocumentArchiver(contenido, renderer, almacen, secciones());
    private final GetPayslipDocumentService servicio = new GetPayslipDocumentService(
            repositorio, almacen, contenido, renderer, secciones(), archivador);

    @Test
    void closingLosingTheDocumentAndDownloadingGivesBackTheSameBytes() throws InterruptedException {
        Payroll cerrado = PayslipDocumentFixtures.cerrado(PayslipDocumentFixtures.emp000001());
        repositorio.recibo = cerrado;
        archivador.archive(cerrado);
        byte[] alCerrar = almacen.objetos.get(CLAVE).clone();

        // Se pierde el documento, y pasa el tiempo: con el reloj dentro del PDF, esto basta para
        // que la segunda generacion difiera de la primera.
        almacen.objetos.clear();
        Thread.sleep(20);

        PayslipDocument descargado = servicio.getDocument(EMP000001);

        assertArrayEquals(alCerrar, descargado.content(),
                "El documento regenerado es, byte a byte, el que se archivo al cerrar");
        assertTrue(descargado.definitive(), "Y se sirve como lo que es: el documento, no un borrador");
        assertEquals("recibo-EMP000001-202609.pdf", descargado.fileName());
    }

    /** Y se guarda: la siguiente descarga ya lo lee del almacen y no lo vuelve a dibujar. */
    @Test
    void theRedrawnDocumentIsStoredUnderItsKey() {
        Payroll cerrado = PayslipDocumentFixtures.cerrado(PayslipDocumentFixtures.emp000001());
        repositorio.recibo = cerrado;

        PayslipDocument descargado = servicio.getDocument(EMP000001);

        assertArrayEquals(descargado.content(), almacen.objetos.get(CLAVE),
                "Queda en el almacen con la clave de negocio del recibo: " + almacen.objetos.keySet());
        assertEquals(1, almacen.escrituras);

        servicio.getDocument(EMP000001);
        assertEquals(1, almacen.escrituras, "La segunda descarga lo encuentra y no escribe nada");
    }

    /** Lo que no cambia: un recibo sin cerrar sigue siendo un borrador, y no deja nada guardado. */
    @Test
    void aPayslipThatIsNotClosedIsStillADraftAndLeavesNothing() {
        repositorio.recibo = PayslipDocumentFixtures.emp000001();
        assertEquals(PayrollStatus.CALCULATED, repositorio.recibo.getStatus());

        PayslipDocument documento = servicio.getDocument(EMP000001);

        assertEquals("recibo-EMP000001-202609-borrador.pdf", documento.fileName());
        assertTrue(almacen.objetos.isEmpty(), "Un borrador no se guarda: el almacen guarda documentos");
    }

    // ---------------------------------------------------------------------

    private static final class AlmacenDeMentira implements PayslipDocumentStorage {
        private final Map<String, byte[]> objetos = new HashMap<>();
        private int escrituras;

        @Override
        public void store(String objectKey, byte[] content, String contentType) {
            escrituras++;
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
        public PayrollSearchPage findPageByFilters(
                String ruleSystemCode, String payrollPeriodCode, String employeeNumber, PayrollStatus status,
                int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Payroll save(Payroll payroll) {
            throw new UnsupportedOperationException("Servir un documento no toca el recibo");
        }

        @Override
        public void deleteById(Long id) {
        }

        @Override
        public void flush() {
        }
    }

    private static PayslipSectionRepository secciones() {
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

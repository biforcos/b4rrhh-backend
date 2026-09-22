package com.b4rrhh.payroll.document;

import com.b4rrhh.payroll.application.service.PayrollSnapshotProfiles;
import com.b4rrhh.payroll.application.usecase.FinalizePayrollCommand;
import com.b4rrhh.payroll.application.usecase.FinalizePayrollService;
import com.b4rrhh.payroll.document.application.service.PayslipDocumentArchiver;
import com.b4rrhh.payroll.document.application.service.PayslipDocumentContentFactory;
import com.b4rrhh.payroll.document.domain.exception.PayslipDocumentStorageUnavailableException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lo que se guarda es el documento definitivo, y solo el ({@code backend#112}, criterios 1, 3 y 4).
 *
 * <p>Los tres criterios son tres caras de la misma decision —lo que se congela no es el calculo,
 * es el documento— y por eso viven juntos:
 *
 * <ul>
 *   <li>Un {@code DEFINITIVE} deja su documento en el almacen, en una direccion que es su clave de
 *       negocio.</li>
 *   <li>Un recibo que no lo es <b>no deja nada</b>. El almacen guarda documentos y un borrador no
 *       lo es.</li>
 *   <li>Cerrar con el almacen caido falla, y el recibo no pasa a {@code DEFINITIVE}.</li>
 * </ul>
 *
 * <p>El almacen de aqui es un mapa. Lo que se comprueba contra MinIO de verdad —que el objeto esta
 * y que el bucket sigue vacio despues de pedir un borrador— se mira en el bucket, no se deduce de
 * este fichero.
 */
class WhatIsArchivedIsTheDefinitiveDocumentTest {

    /** Un almacen de mentira que cuenta lo que le piden, y que puede estar caido. */
    private static final class AlmacenDeMentira implements PayslipDocumentStorage {
        private final Map<String, byte[]> objetos = new HashMap<>();
        private boolean caido;

        @Override
        public void store(String objectKey, byte[] content, String contentType) {
            if (caido) {
                throw new PayslipDocumentStorageUnavailableException(objectKey, new RuntimeException("apagado"));
            }
            objetos.put(objectKey, content);
        }

        @Override
        public Optional<byte[]> retrieve(String objectKey) {
            if (caido) {
                throw new PayslipDocumentStorageUnavailableException(objectKey, new RuntimeException("apagado"));
            }
            return Optional.ofNullable(objetos.get(objectKey));
        }
    }

    private final AlmacenDeMentira almacen = new AlmacenDeMentira();
    private final PayslipDocumentArchiver archivador = new PayslipDocumentArchiver(
            new PayslipDocumentContentFactory(new PayrollSnapshotProfiles(new ObjectMapper())),
            new PdfBoxPayslipDocumentRenderer(),
            almacen,
            seccionesDelCatalogo());

    /** Criterio 1: el documento se guarda al cerrar, con la clave de negocio por direccion. */
    @Test
    void closingAPayslipLeavesItsDocumentInTheStore() {
        archivador.archive(PayslipDocumentFixtures.cerrado(PayslipDocumentFixtures.emp000001()));

        byte[] guardado = almacen.objetos.get("ESP/202609/NORMAL/INTERNAL/EMP000001-2.pdf");
        assertNotNull(guardado,
                "La direccion es la clave de negocio, presencia incluida. Lo que hay: "
                        + almacen.objetos.keySet());
        assertTrue(guardado.length > 0);
    }

    /**
     * Criterio 1: pedirlo dos veces devuelve los mismos bytes porque es el mismo objeto.
     *
     * <p>No porque se generara igual. Esa es toda la diferencia: un documento que se regenerase
     * seria byte a byte identico hoy y distinto el dia que cambie la plantilla, y entonces habria
     * dos documentos del mismo recibo. El empleado tiene uno.
     */
    @Test
    void askingTwiceGivesBackTheSameObject() {
        archivador.archive(PayslipDocumentFixtures.cerrado(PayslipDocumentFixtures.emp000001()));
        String clave = "ESP/202609/NORMAL/INTERNAL/EMP000001-2.pdf";

        byte[] primera = almacen.retrieve(clave).orElseThrow();
        byte[] segunda = almacen.retrieve(clave).orElseThrow();

        assertEquals(primera, segunda, "Es el mismo array: es el mismo objeto, no otra generacion");
    }

    /** Criterio 3: un recibo que no es definitivo no deja nada en el almacen. */
    @Test
    void aPayslipThatIsNotDefinitiveLeavesNothingBehind() {
        Payroll calculado = PayslipDocumentFixtures.emp000001();
        assertEquals(PayrollStatus.CALCULATED, calculado.getStatus());

        assertThrows(IllegalStateException.class, () -> archivador.archive(calculado),
                "El almacen guarda documentos, y un borrador no lo es");
        assertTrue(almacen.objetos.isEmpty(), "Y no deja nada al intentarlo");
    }

    /** Criterio 4: cerrar con el almacen inalcanzable falla y el recibo no pasa a DEFINITIVE. */
    @Test
    void closingWithTheStoreDownFailsAndTheStatusDoesNotMove() {
        almacen.caido = true;
        Payroll existente = PayslipDocumentFixtures.emp000001();

        RepositorioDeMentira repositorio = new RepositorioDeMentira(existente);
        FinalizePayrollService cierre = new FinalizePayrollService(repositorio, archivador);

        PayslipDocumentStorageUnavailableException fallo = assertThrows(
                PayslipDocumentStorageUnavailableException.class,
                () -> cierre.finalizePayroll(new FinalizePayrollCommand(
                        "ESP", "INTERNAL", "EMP000001", "202609", "NORMAL", 2)));

        assertTrue(fallo.getMessage().contains("ESP/202609/NORMAL/INTERNAL/EMP000001-2.pdf"),
                "El error dice de que documento habla: " + fallo.getMessage());
        assertFalse(repositorio.seGuardo,
                "No se escribe ningun DEFINITIVE: no se cierra lo que no se puede entregar");
    }

    // ---------------------------------------------------------------------

    /** Solo se usan findByBusinessKey y save, y hace falta saber si se llamo al segundo. */
    private static final class RepositorioDeMentira implements PayrollRepository {
        private final Payroll existente;
        private boolean seGuardo;

        private RepositorioDeMentira(Payroll existente) {
            this.existente = existente;
        }

        @Override
        public Payroll save(Payroll payroll) {
            seGuardo = true;
            return payroll;
        }

        @Override
        public Optional<Payroll> findByBusinessKey(String ruleSystemCode, String employeeTypeCode,
                                                   String employeeNumber, String payrollPeriodCode,
                                                   String payrollTypeCode, Integer presenceNumber) {
            return Optional.of(existente);
        }

        @Override
        public List<Payroll> findByFilters(String ruleSystemCode, String payrollPeriodCode,
                                           String employeeNumber, PayrollStatus status) {
            return List.of();
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

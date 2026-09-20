package com.b4rrhh.payroll.document.application.service;

import com.b4rrhh.payroll.document.application.port.PayslipDocumentRenderer;
import com.b4rrhh.payroll.document.domain.model.PayslipDocumentKey;
import com.b4rrhh.payroll.document.domain.port.PayslipDocumentStorage;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll_engine.concept.domain.port.PayslipSectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * El acto de archivar el documento de un recibo que se cierra ({@code backend#112}).
 *
 * <h2>Por que esto existe como pieza y no como tres lineas dentro del cierre</h2>
 *
 * <p>Porque hay <b>dos</b> caminos a {@code DEFINITIVE} —cerrar uno y cerrar en masa— y la
 * invariante «un definitivo tiene su documento» tiene que valer en los dos. Escrito dos veces, el
 * dia que alguien anada un tercer camino lo escribira cero. Escrito aqui, el candado que lo vigila
 * puede preguntar una sola cosa: quien llama a {@code finalizePayroll()}, ¿tambien llama a esto?
 * Eso es {@code EveryPathToDefinitiveArchivesItsDocumentTest}.
 *
 * <h2>Se genera una vez y no se regenera nunca</h2>
 *
 * <p>Lo guardado <b>es</b> el documento. No se vuelve a generar «para actualizar la plantilla» ni
 * «porque el de antes tenia una errata»: dos generaciones con plantillas distintas darian dos
 * documentos del mismo recibo, y el empleado tiene uno. El del almacen es el que tiene el.
 *
 * <p>Si hay que cambiar un documento entregado, eso es otra operacion, con otro nombre, y no
 * existe todavia.
 */
@Service
public class PayslipDocumentArchiver {

    private static final Logger log = LoggerFactory.getLogger(PayslipDocumentArchiver.class);

    private final PayslipDocumentContentFactory contentFactory;
    private final PayslipDocumentRenderer renderer;
    private final PayslipDocumentStorage storage;
    private final PayslipSectionRepository payslipSectionRepository;

    public PayslipDocumentArchiver(
            PayslipDocumentContentFactory contentFactory,
            PayslipDocumentRenderer renderer,
            PayslipDocumentStorage storage,
            PayslipSectionRepository payslipSectionRepository
    ) {
        this.contentFactory = contentFactory;
        this.renderer = renderer;
        this.storage = storage;
        this.payslipSectionRepository = payslipSectionRepository;
    }

    /**
     * Genera y guarda el documento del recibo que se esta cerrando.
     *
     * <p><b>No atrapa nada.</b> Si el almacen no contesta, la excepcion sube y el cierre no pasa:
     * el recibo se queda como estaba y el error dice por que. Es la consecuencia que hay que
     * aceptar de frente —fallar el cierre se reintenta en un minuto; un cerrado sin PDF es un
     * documento que no existe.
     *
     * @param definitive el recibo <b>ya en {@code DEFINITIVE}</b>, porque lo que se archiva es el
     *                   documento final: uno construido desde un recibo en otro estado llevaria
     *                   impresa la marca de borrador y se quedaria asi para siempre.
     */
    public void archive(Payroll definitive) {
        if (definitive.getStatus() != PayrollStatus.DEFINITIVE) {
            throw new IllegalStateException(
                    "Solo se archiva el documento de un recibo definitivo, y este esta en "
                            + definitive.getStatus() + ". El almacen guarda documentos, y un "
                            + "borrador no lo es.");
        }

        String objectKey = PayslipDocumentKey.of(definitive);
        byte[] documento = renderer.render(contentFactory.contentOf(
                definitive,
                payslipSectionRepository.findByRuleSystemCode(definitive.getRuleSystemCode())));

        storage.store(objectKey, documento, renderer.contentType());
        log.info("[RECIBO] Documento archivado | {} | {} bytes", objectKey, documento.length);
    }
}

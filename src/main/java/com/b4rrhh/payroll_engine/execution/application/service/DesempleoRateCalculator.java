package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.model.TechnicalConceptSegmentData;
import com.b4rrhh.payroll_engine.execution.domain.port.SsCotizacionTiposRepository;
import com.b4rrhh.payroll_engine.execution.domain.port.SsDesempleoModalidadRepository;

import java.math.BigDecimal;

/**
 * El tipo de desempleo del tramo, que depende del contrato ({@code backend#124}).
 *
 * <p>Es el mismo {@link SsCotizacionRateCalculator} con un paso delante: la contingencia no esta
 * fija en el calculador, se compone con la modalidad que le toca al contrato del tramo. Asi el
 * <b>que</b> —5,50 o 6,70— sigue viviendo en {@code ss_cotizacion_tipos} con su vigencia, y el
 * <b>cual de los dos</b> en {@code ss_desempleo_modalidad_contrato}, tambien con la suya.
 *
 * <h2>Por que el tramo y no la asignacion de conceptos</h2>
 *
 * <p>Por lo mismo que el regimen de pagas extras (ADR-070 §2): un contrato puede cambiar a mitad
 * de mes, y el plan de conceptos se arma una vez para el periodo entero. Una condicion de
 * {@code concept_assignment} no sabria contestar dos cosas distintas en el mismo recibo. La
 * particion ya parte por contrato desde el {@code backend#47}, asi que el dato llega solo.
 *
 * <h2>Que pasa si falta</h2>
 *
 * <p>Se para la corrida, en los dos sitios donde puede faltar: sin contrato en el tramo y sin
 * modalidad declarada para ese contrato. Elegir la indefinida por defecto haria cotizar de menos
 * a un temporal, y eso es exactamente lo que este issue viene a arreglar: <b>el error barato es
 * el que no se ve</b>.
 */
public class DesempleoRateCalculator implements TechnicalConceptCalculator {

    private final String conceptCode;
    private final String contingencyPrefix;
    private final SsDesempleoModalidadRepository modalidades;
    private final SsCotizacionTiposRepository tipos;

    public DesempleoRateCalculator(
            String conceptCode,
            String contingencyPrefix,
            SsDesempleoModalidadRepository modalidades,
            SsCotizacionTiposRepository tipos) {

        this.conceptCode = conceptCode;
        this.contingencyPrefix = contingencyPrefix;
        this.modalidades = modalidades;
        this.tipos = tipos;
    }

    @Override
    public String conceptCode() {
        return conceptCode;
    }

    @Override
    public BigDecimal resolve(TechnicalConceptSegmentData context) {
        String contrato = context.contractCode();
        if (contrato == null || contrato.isBlank()) {
            throw new IllegalStateException(
                    "El tramo " + context.segmentStart() + ".." + context.segmentEnd()
                            + " no trae contrato, y sin el no se sabe por que modalidad de"
                            + " desempleo cotiza (concepto " + conceptCode + ")");
        }

        String modalidad = modalidades
                .findModalidad(context.ruleSystemCode(), contrato, context.periodEnd())
                .orElseThrow(() -> new IllegalStateException(
                        "El contrato " + contrato + " no tiene modalidad de desempleo declarada en"
                                + " payroll_engine.ss_desempleo_modalidad_contrato para"
                                + " ruleSystem=" + context.ruleSystemCode()
                                + " referenceDate=" + context.periodEnd()));

        String contingencia = contingencyPrefix + "_" + modalidad;
        return tipos.findRate(context.ruleSystemCode(), contingencia, context.periodEnd())
                .orElseThrow(() -> new IllegalStateException(
                        "No ss_cotizacion_tipos entry found for contingency=" + contingencia
                                + " ruleSystem=" + context.ruleSystemCode()
                                + " referenceDate=" + context.periodEnd()));
    }
}

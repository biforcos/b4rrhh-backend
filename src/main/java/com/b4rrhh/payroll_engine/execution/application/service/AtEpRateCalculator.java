package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.model.SsTarifaPrimaAt;
import com.b4rrhh.payroll_engine.execution.domain.model.TechnicalConceptSegmentData;
import com.b4rrhh.payroll_engine.execution.domain.port.SsTarifaPrimasAtRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * El tipo de la cuota de accidentes de trabajo y enfermedad profesional ({@code backend#122}).
 *
 * <p>Es el unico tipo de este catalogo que <b>no es el mismo para todo el mundo</b>: sale de la
 * actividad economica de la empresa. Dos empleados identicos en empresas con CNAE distinto cotizan
 * distinto, y eso no es una excepcion del modelo sino la norma —la prima de accidentes es el
 * precio del riesgo de la actividad—.
 *
 * <p>Sigue siendo {@code ENGINE_PROVIDED} con todas las de la ley (ADR-046, ADR-048): resuelve un
 * valor que se <b>consulta</b> en el contexto de ejecucion, no calcula un concepto economico. Lo
 * que calcula la cuota es el {@code 727}, un {@code PERCENTAGE} del grafo.
 *
 * <p>Suma los dos tipos de la tarifa porque lo que se paga es uno: el recibo ensena una linea de
 * accidentes de trabajo, no dos.
 *
 * <h2>Sin CNAE revienta la corrida, y esta bien</h2>
 *
 * <p>Una empresa sin CNAE, o un CNAE que ninguna entrada de la tarifa cubre, es catalogo mal
 * parametrizado y no un caso de negocio. Devolver cero dejaria un recibo con la cuota de
 * accidentes a cero y sin una linea que lo dijera —la regla del cero no la imprimiria— y eso es
 * exactamente un numero equivocado que no falla. Es la misma decision que el divisor a cero del
 * ADR-070.
 */
@Component
public class AtEpRateCalculator implements TechnicalConceptCalculator {

    private final SsTarifaPrimasAtRepository tarifa;

    public AtEpRateCalculator(SsTarifaPrimasAtRepository tarifa) {
        this.tarifa = tarifa;
    }

    @Override
    public String conceptCode() {
        return "P_AT_EP";
    }

    @Override
    public BigDecimal resolve(TechnicalConceptSegmentData context) {
        if (context.cnaeCode() == null || context.cnaeCode().isBlank()) {
            throw new IllegalStateException(
                    "No CNAE on the employee's company profile: the AT/EP premium rate cannot be"
                            + " resolved. ruleSystem=" + context.ruleSystemCode()
                            + " referenceDate=" + context.periodEnd());
        }
        return tarifa.findForCnae(context.ruleSystemCode(), context.cnaeCode(), context.periodEnd())
                .map(SsTarifaPrimaAt::tipoTotal)
                .orElseThrow(() -> new IllegalStateException(
                        "No ss_tarifa_primas_at entry covers cnae=" + context.cnaeCode()
                                + " ruleSystem=" + context.ruleSystemCode()
                                + " referenceDate=" + context.periodEnd()));
    }
}

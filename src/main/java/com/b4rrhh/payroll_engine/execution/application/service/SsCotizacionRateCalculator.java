package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.model.TechnicalConceptSegmentData;
import com.b4rrhh.payroll_engine.execution.domain.port.SsCotizacionTiposRepository;

import java.math.BigDecimal;

/**
 * Un tipo de cotizacion, leido del catalogo ({@code backend#105}).
 *
 * <p>Sustituye a nueve clases que devolvian nueve constantes de Java. Los mismos nueve numeros
 * estaban sembrados en {@code payroll_engine.ss_cotizacion_tipos} <b>con sus vigencias</b> y no
 * los leia nadie: una tabla con fechas que nadie consulta es la promesa de que el sistema sabe
 * que los tipos cambian con el tiempo, y no lo sabia.
 *
 * <h2>Por que gana la tabla y no el codigo</h2>
 *
 * <p>Porque la tabla <b>ya tiene vigencias</b>. Un cambio de tipos de cotizacion pasa por ley
 * cada ano, y con las constantes eso era un cambio de codigo, una compilacion y un despliegue.
 * Leer la tabla es lo unico que hace cierto que un tipo cambie a mitad de ano sin tocar nada.
 *
 * <p>Y se retira la duplicacion, que era lo peor de las dos: los numeros no estaban solo inertes
 * en el esquema, tenian un gemelo vivo en Java, y los dos podian divergir sin que nada se
 * enterara.
 *
 * <h2>Esto sigue siendo ENGINE_PROVIDED, y esta bien</h2>
 *
 * <p>{@code ENGINE_PROVIDED} quiere decir que el valor lo <b>resuelve el motor desde su
 * contexto</b> —una tasa, un tope, unos dias—, no que sea una constante compilada. Un tipo de
 * cotizacion buscado por fecha es exactamente eso. Lo que {@code ENGINE_PROVIDED} no puede ser
 * es un concepto economico calculado en Java.
 *
 * <h2>La fecha con la que se busca</h2>
 *
 * <p>{@code periodEnd}, la misma que usa {@link TopeMaxCotizacionCalculator}. A proposito: si el
 * tipo se resolviera por una fecha y el tope por otra, un mismo recibo podria mezclar el tipo de
 * un ano con el limite de otro, y eso no se veria en ninguna linea.
 */
public class SsCotizacionRateCalculator implements TechnicalConceptCalculator {

    private final String conceptCode;
    private final String contingencyCode;
    private final SsCotizacionTiposRepository tipos;

    public SsCotizacionRateCalculator(
            String conceptCode, String contingencyCode, SsCotizacionTiposRepository tipos) {
        this.conceptCode = conceptCode;
        this.contingencyCode = contingencyCode;
        this.tipos = tipos;
    }

    @Override
    public String conceptCode() {
        return conceptCode;
    }

    @Override
    public BigDecimal resolve(TechnicalConceptSegmentData context) {
        return tipos.findRate(context.ruleSystemCode(), contingencyCode, context.periodEnd())
                .orElseThrow(() -> new IllegalStateException(
                        "No ss_cotizacion_tipos entry found for contingency=" + contingencyCode
                                + " ruleSystem=" + context.ruleSystemCode()
                                + " referenceDate=" + context.periodEnd()));
    }
}

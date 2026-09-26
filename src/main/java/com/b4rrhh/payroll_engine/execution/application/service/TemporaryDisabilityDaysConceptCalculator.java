package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.model.ItPrestacionTramo;
import com.b4rrhh.payroll_engine.execution.domain.model.TechnicalConceptSegmentData;
import com.b4rrhh.payroll_engine.execution.domain.port.ItPrestacionTramoRepository;

import java.math.BigDecimal;

/**
 * Cuantos dias de este tramo del periodo caen en un tramo de porcentaje de la baja
 * ({@code backend#129}).
 *
 * <h2>Los dias se cuentan desde el inicio de la ausencia</h2>
 *
 * <p>Y el inicio puede estar en otro mes. Una baja que empezo el 25 de agosto llega al 1 de septiembre
 * con siete dias detras, asi que los dias de septiembre son del ocho al treinta y siete de la baja:
 * ocho en el tramo de la empresa, cinco en el primero de pago delegado y diecisiete en el segundo.
 *
 * <p><b>Eso no es leer otro mes</b> —eso es el {@code backend#128}, y es otra cosa—: es una fecha. El
 * tramo del periodo trae los dias ya contados ({@code SegmentAbsence}), y aqui solo se cruza el
 * intervalo con la fila de la tabla.
 *
 * <h2>Por que la tabla</h2>
 *
 * <p>Los limites de los tramos y sus porcentajes son cifras de una norma: art. 173.1 de la LGSS
 * —«desde el dia cuarto al decimoquinto de baja, ambos inclusive, el subsidio estara a cargo del
 * empresario»—, el articulo unico del RD 53/1980 —el 60 % del dia cuarto al veinte— y el art. 2 del
 * Decreto 3158/1966 —el 75 %—. Van en una tabla con su cita, por lo mismo que el tipo de desempleo
 * (ADR-072) y los topes de cada ejercicio (V153): cambian sin que cambie el programa.
 *
 * <p>Esto sigue siendo {@code ENGINE_PROVIDED} con todas las de la ley (ADR-046, ADR-048): resuelve un
 * valor buscandolo en su reglamentacion y derivandolo del contexto de ejecucion. Lo que no hace es
 * calcular un concepto economico: quien multiplica dias por importe es el grafo.
 */
public class TemporaryDisabilityDaysConceptCalculator implements TechnicalConceptCalculator {

    private final String conceptCode;
    private final String tramoCode;
    private final ItPrestacionTramoRepository tramos;

    public TemporaryDisabilityDaysConceptCalculator(
            String conceptCode, String tramoCode, ItPrestacionTramoRepository tramos) {
        this.conceptCode = conceptCode;
        this.tramoCode = tramoCode;
        this.tramos = tramos;
    }

    @Override
    public String conceptCode() {
        return conceptCode;
    }

    @Override
    public BigDecimal resolve(TechnicalConceptSegmentData context) {
        // Sin baja con derecho no hay dias que contar. Y no es una comprobacion de mas: este concepto
        // esta en el plan de TODO el mundo, porque la asignacion acota por sociedad, convenio y tipo de
        // empleado y no por empleado (ADR-074 §6).
        if (!context.needsDailyRegulatoryBase()) {
            return BigDecimal.ZERO;
        }

        ItPrestacionTramo tramo = tramos.findActive(
                        context.ruleSystemCode(),
                        context.absence().absenceTypeCode(),
                        tramoCode,
                        context.periodEnd())
                .orElseThrow(() -> new IllegalStateException(
                        "No hay tramo de prestacion " + tramoCode + " para "
                        + context.absence().absenceTypeCode() + " vigente el " + context.periodEnd()
                        + ": el catalogo no declara como se paga esta baja"));

        long primerDia = context.absence().daysBeforeSegment() + 1;
        long ultimoDia = context.absence().daysBeforeSegment() + context.daysInSegment();
        return BigDecimal.valueOf(tramo.daysWithin(primerDia, ultimoDia));
    }
}

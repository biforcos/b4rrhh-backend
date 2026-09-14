package com.b4rrhh.payroll.application.port;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Un paso que el motor dio calculando un recibo.
 *
 * <p>Hay uno por cada evaluación de un concepto, y son los 36 del motor y no los 14 que llegan al
 * folio: los 22 que se quedaban fuera —5 {@code BASE} y 17 {@code TECHNICAL}— son justo los que
 * explican de dónde sale el número ({@code backend#93}).
 *
 * <p><b>36 conceptos no son 36 pasos.</b> Un concepto de ámbito {@code SEGMENT} se evalúa una vez
 * por segmento, así que un empleado normal deja 36 pasos y uno del mes partido 40, con el 101 dos
 * veces y con dos precios distintos. Por eso la identidad es el orden de ejecución y no el
 * concepto.
 *
 * <p>El {@code executionScope} va explícito y no se deduce de que las fechas vengan nulas. Como las
 * dos cosas tienen que decir lo mismo, el invariante se comprueba aquí y, otra vez, en el esquema:
 * un paso {@code PERIOD} no tiene segmento y un paso de segmento siempre lo tiene.
 */
public record PayrollCalculationStep(
        int executionOrder,
        String conceptCode,
        String conceptMnemonic,
        String calculationType,
        String functionalNature,
        String executionScope,
        LocalDate segmentStartDate,
        LocalDate segmentEndDate,
        BigDecimal amount,
        BigDecimal quantity,
        BigDecimal rate,
        String payslipOrderCode
) {

    /** El ámbito cuyo paso cubre todo el periodo y, por eso, no tiene fechas de segmento. */
    public static final String PERIOD_SCOPE = "PERIOD";

    public PayrollCalculationStep {
        if (executionOrder <= 0) {
            throw new IllegalArgumentException("executionOrder must be positive");
        }
        Objects.requireNonNull(conceptCode, "conceptCode");
        Objects.requireNonNull(conceptMnemonic, "conceptMnemonic");
        Objects.requireNonNull(calculationType, "calculationType");
        Objects.requireNonNull(functionalNature, "functionalNature");
        Objects.requireNonNull(executionScope, "executionScope");
        Objects.requireNonNull(amount, "amount");

        boolean isPeriod = PERIOD_SCOPE.equals(executionScope);
        boolean hasSegment = segmentStartDate != null && segmentEndDate != null;
        if (isPeriod && (segmentStartDate != null || segmentEndDate != null)) {
            throw new IllegalArgumentException(
                    "A PERIOD step covers the whole period and must not carry segment dates: "
                            + conceptCode);
        }
        if (!isPeriod && !hasSegment) {
            throw new IllegalArgumentException(
                    "A " + executionScope + " step must carry both segment dates: " + conceptCode);
        }
        if (hasSegment && segmentStartDate.isAfter(segmentEndDate)) {
            throw new IllegalArgumentException(
                    "Segment start is after segment end for step " + conceptCode);
        }
    }

    /** Si este paso, además de calcularse, llegó al folio como línea de recibo. */
    public boolean isPayslipLine() {
        return payslipOrderCode != null;
    }
}

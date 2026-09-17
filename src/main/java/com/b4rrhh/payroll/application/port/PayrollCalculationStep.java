package com.b4rrhh.payroll.application.port;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Un paso que el motor dio calculando un recibo.
 *
 * <p>Hay uno por cada evaluación de un concepto, y son los 35 del motor y no los 14 que llegan al
 * folio: los 21 que se quedaban fuera —5 {@code BASE} y 16 {@code TECHNICAL}— son justo los que
 * explican de dónde sale el número ({@code backend#93}).
 *
 * <p><b>Un concepto no es un paso.</b> Un concepto de ámbito {@code SEGMENT} se evalúa una vez por
 * segmento, así que los 35 del catálogo dejan 35 pasos en un empleado normal y 39 en uno del mes
 * partido, con el 101 dos veces y con dos precios distintos. Por eso la identidad es el orden de
 * ejecución y no el concepto.
 *
 * <p>Esta frase decía «36 pasos y 40», y no era un recuento sino la cuenta del catálogo puesta
 * donde va la de los pasos: {@code P_SS} estaba en el catálogo de 36 y ningún plan lo pedía, así
 * que nunca llegó a ser un paso. Lo medido siempre fue 35 y 39. La {@code V130} lo retira
 * ({@code backend#96}) y las dos cuentas vuelven a ser la misma, pero el número de pasos no se
 * movió ni podía moverse: retirar un concepto que nadie ejecuta no añade ninguno.
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
        String payslipOrderCode,
        /**
         * El numero de linea del folio al que fue a parar este paso, o {@code null} si no llego al
         * folio ({@code backend#103}).
         *
         * <p>Es lo que hace explicita la relacion entre un paso y una linea, y va en las dos
         * direcciones: de la linea a sus pasos —los que llevan su numero— y del paso a su linea.
         * Varios pasos pueden compartir numero, y eso <b>es</b> la fusion: el folio agrupa por
         * {@code concepto|tarifa} y suma, asi que dos tramos al mismo precio salen en una sola
         * linea aunque no sean contiguos.
         *
         * <p>Se persiste en vez de derivarse, y el motivo es el ADR-062 §1: la relacion la conoce
         * la proyeccion en el momento de fusionar, y volver a deducirla despues obligaria a repetir
         * la regla de agrupacion en un segundo sitio — que es exactamente lo que aquel ADR existe
         * para impedir.
         */
        Integer payslipLineNumber,
        /**
         * La fila de tabla de la que este paso leyo su valor, o {@code null} si no lo leyo de
         * ninguna ({@code backend#107}).
         *
         * <p>El nulo es el caso normal y significa algo: un {@code AGGREGATE} no lee tablas, un
         * {@code PERCENTAGE} multiplica una base por un tipo y un {@code ENGINE_PROVIDED} deriva
         * del contexto. De los 38 pasos de un recibo ESP leen una fila dos: el precio del dia y
         * el de la hora extra.
         *
         * <p>Se guarda en vez de volver a resolverse al leer, y esa es toda la diferencia: la
         * busqueda es por vigencia y por categoria, asi que repetirla contesta <b>donde estaria
         * hoy</b> ese valor y no de donde salio. Es el defecto por el que se descarto la puerta
         * del {@code explain} en el {@code backend#93}.
         */
        TableRowOrigin sourceTableRow
) {

    /** El paso tal como sale del motor: todavia no sabe si ira al folio ni a que linea. */
    public PayrollCalculationStep(
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
        this(executionOrder, conceptCode, conceptMnemonic, calculationType, functionalNature,
                executionScope, segmentStartDate, segmentEndDate, amount, quantity, rate,
                payslipOrderCode, null, null);
    }

    /** El mismo paso recien salido del motor, sabiendo de que fila de tabla leyo su valor. */
    public PayrollCalculationStep(
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
            String payslipOrderCode,
            TableRowOrigin sourceTableRow
    ) {
        this(executionOrder, conceptCode, conceptMnemonic, calculationType, functionalNature,
                executionScope, segmentStartDate, segmentEndDate, amount, quantity, rate,
                payslipOrderCode, null, sourceTableRow);
    }

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

    /** El mismo paso, sabiendo ya en que linea del folio quedo. */
    public PayrollCalculationStep enLinea(int payslipLineNumber) {
        return new PayrollCalculationStep(executionOrder, conceptCode, conceptMnemonic,
                calculationType, functionalNature, executionScope, segmentStartDate, segmentEndDate,
                amount, quantity, rate, payslipOrderCode, payslipLineNumber, sourceTableRow);
    }

    /** Si este paso, además de calcularse, llegó al folio como línea de recibo. */
    public boolean isPayslipLine() {
        return payslipOrderCode != null;
    }

}

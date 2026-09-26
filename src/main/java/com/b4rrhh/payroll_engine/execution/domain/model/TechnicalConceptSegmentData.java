package com.b4rrhh.payroll_engine.execution.domain.model;

import com.b4rrhh.payroll_engine.segment.domain.model.SegmentAbsence;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TechnicalConceptSegmentData(
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate segmentStart,
        LocalDate segmentEnd,
        long daysInSegment,
        BigDecimal workingTimePercentage,
        String ruleSystemCode,
        String grupoCotizacionCode,
        String tipoNomina,
        /**
         * Si en este tramo las pagas extras del empleado se prorratean ({@code backend#118}).
         *
         * <p>Es lo que decide por cual de las dos puertas entra la prorrata en el recibo, y viaja
         * por el tramo y no por la asignacion porque el regimen puede cambiar a mitad de mes: el
         * plan de conceptos se arma una vez para el periodo entero, asi que una condicion de
         * {@code concept_assignment} no sabria contestar dos cosas distintas en el mismo recibo
         * (ADR-070).
         */
        boolean extraPaymentsProrated,
        /**
         * La actividad economica de la empresa del empleado, en CNAE ({@code backend#122}).
         *
         * <p>Puede ser nula. La lee {@code AtEpRateCalculator} para buscar el tipo en la tarifa
         * de primas; ningun otro calculador la necesita, y por eso no se valida aqui.
         */
        String cnaeCode,
        /**
         * El contrato vigente en este tramo ({@code backend#124}).
         *
         * <p>Puede ser nulo. Lo lee {@code DesempleoRateCalculator} para saber por que modalidad
         * cotiza el tramo -indefinida o de duracion determinada-, y es el quien se queja si falta:
         * ningun otro calculador lo necesita.
         */
        String contractCode,
        /**
         * La ausencia que hace que este tramo exista, o {@code null} ({@code backend#127}).
         *
         * <p>Solo la traen los tramos de ausencia <b>que no se paga</b>. La lee
         * {@code AccrualDaysConceptCalculator} para no devengar dias, y la leeran los contadores de
         * dias por tramo de porcentaje del {@code backend#129}. Unas vacaciones no dejan tramo
         * propio: no hay nada a lo que preguntarle por ellas (ADR-073).
         */
        SegmentAbsence absence,
        /**
         * La base de cotizacion diaria del recibo cerrado del mes anterior, o {@code null}
         * ({@code backend#128}).
         *
         * <p>La leen los dos calculadores de la base reguladora y nadie mas. Nula quiere decir o que
         * no hay recibo cerrado del mes anterior, o que esta unidad no necesita base reguladora.
         */
        BigDecimal previousPeriodDailyContributionBase
) {

    /** Si en este tramo no se devengan dias, que es lo que significa traer ausencia. */
    public boolean isUnpaidAbsenceSegment() {
        return absence != null;
    }

    /**
     * Si de este tramo sale base reguladora: es un tramo de baja por enfermedad comun
     * ({@code backend#128}).
     *
     * <p>Un tramo trabajado no la necesita, y por eso la base reguladora vale <b>cero</b> en el: lo
     * que la lee —la prestacion y la base durante la baja— tambien vale cero ahi, asi que el cero no
     * esconde nada y ahorra tener que preguntar dos veces lo mismo.
     */
    public boolean needsDailyRegulatoryBase() {
        return absence != null && absence.paysTemporaryDisabilityBenefit();
    }
}

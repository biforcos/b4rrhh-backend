package com.b4rrhh.payroll_engine.execution.domain.model;

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
        String cnaeCode
) {}

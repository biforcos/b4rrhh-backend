package com.b4rrhh.payroll_engine.segment.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Captures all data needed to calculate a single CalculationSegment for one employee.
 *
 * <p>This is a read-only value object assembled by the application layer before invoking
 * segment-level calculation logic. It carries both period-level and segment-level context
 * so that calculation rules need not re-derive them.
 *
 * <p>All date fields are inclusive. {@code daysInPeriod} and {@code daysInSegment} are
 * pre-computed inclusive day counts.
 */
public final class SegmentCalculationContext {

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be null or blank");
        }
    }

    private static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }

    private final String ruleSystemCode;
    private final String employeeTypeCode;
    private final String employeeNumber;

    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final LocalDate segmentStart;
    private final LocalDate segmentEnd;

    private final boolean firstSegment;
    private final boolean lastSegment;

    private final long daysInPeriod;
    private final long daysInSegment;

    private final BigDecimal workingTimePercentage;
    private final BigDecimal monthlySalaryAmount;
    private final Map<String, BigDecimal> employeeInputs;
    private final String grupoCotizacionCode;
    private final String tipoNomina;
    private final Map<String, BigDecimal> precomputedDirectAmounts;
    private final boolean extraPaymentsProrated;
    private final String cnaeCode;
    private final String contractCode;

    /**
     * Sin la actividad economica de la empresa, que es opcional y casi ningun calculo mira
     * ({@code backend#122}).
     *
     * <p>La necesita el tipo de accidentes de trabajo y nadie mas. Un contexto construido sin ella
     * calcula todo lo demas igual; lo que no puede es resolver esa cuota, y entonces la corrida
     * falla diciendo que falta, que es lo correcto.
     */
    public SegmentCalculationContext(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            LocalDate periodStart,
            LocalDate periodEnd,
            LocalDate segmentStart,
            LocalDate segmentEnd,
            boolean firstSegment,
            boolean lastSegment,
            long daysInPeriod,
            long daysInSegment,
            BigDecimal workingTimePercentage,
            BigDecimal monthlySalaryAmount,
            Map<String, BigDecimal> employeeInputs,
            String grupoCotizacionCode,
            String tipoNomina,
            Map<String, BigDecimal> precomputedDirectAmounts,
            boolean extraPaymentsProrated
    ) {
        this(ruleSystemCode, employeeTypeCode, employeeNumber, periodStart, periodEnd,
                segmentStart, segmentEnd, firstSegment, lastSegment, daysInPeriod, daysInSegment,
                workingTimePercentage, monthlySalaryAmount, employeeInputs, grupoCotizacionCode,
                tipoNomina, precomputedDirectAmounts, extraPaymentsProrated, null, null);
    }

    /**
     * Sin el contrato del tramo, que solo mira el tipo de desempleo ({@code backend#124}).
     */
    public SegmentCalculationContext(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            LocalDate periodStart,
            LocalDate periodEnd,
            LocalDate segmentStart,
            LocalDate segmentEnd,
            boolean firstSegment,
            boolean lastSegment,
            long daysInPeriod,
            long daysInSegment,
            BigDecimal workingTimePercentage,
            BigDecimal monthlySalaryAmount,
            Map<String, BigDecimal> employeeInputs,
            String grupoCotizacionCode,
            String tipoNomina,
            Map<String, BigDecimal> precomputedDirectAmounts,
            boolean extraPaymentsProrated,
            String cnaeCode
    ) {
        this(ruleSystemCode, employeeTypeCode, employeeNumber, periodStart, periodEnd,
                segmentStart, segmentEnd, firstSegment, lastSegment, daysInPeriod, daysInSegment,
                workingTimePercentage, monthlySalaryAmount, employeeInputs, grupoCotizacionCode,
                tipoNomina, precomputedDirectAmounts, extraPaymentsProrated, cnaeCode, null);
    }

    public SegmentCalculationContext(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            LocalDate periodStart,
            LocalDate periodEnd,
            LocalDate segmentStart,
            LocalDate segmentEnd,
            boolean firstSegment,
            boolean lastSegment,
            long daysInPeriod,
            long daysInSegment,
            BigDecimal workingTimePercentage,
            BigDecimal monthlySalaryAmount,
            Map<String, BigDecimal> employeeInputs,
            String grupoCotizacionCode,
            String tipoNomina,
            Map<String, BigDecimal> precomputedDirectAmounts,
            boolean extraPaymentsProrated,
            String cnaeCode,
            String contractCode
    ) {
        requireNonBlank(ruleSystemCode, "ruleSystemCode");
        requireNonBlank(employeeTypeCode, "employeeTypeCode");
        requireNonBlank(employeeNumber, "employeeNumber");
        requireNonNull(periodStart, "periodStart");
        requireNonNull(periodEnd, "periodEnd");
        requireNonNull(segmentStart, "segmentStart");
        requireNonNull(segmentEnd, "segmentEnd");
        if (periodEnd.isBefore(periodStart)) {
            throw new IllegalArgumentException("periodEnd must not be before periodStart");
        }
        if (segmentEnd.isBefore(segmentStart)) {
            throw new IllegalArgumentException("segmentEnd must not be before segmentStart");
        }
        if (segmentStart.isBefore(periodStart) || segmentEnd.isAfter(periodEnd)) {
            throw new IllegalArgumentException("segment [" + segmentStart + ", " + segmentEnd +
                    "] must be contained within period [" + periodStart + ", " + periodEnd + "]");
        }
        if (daysInPeriod <= 0) {
            throw new IllegalArgumentException("daysInPeriod must be > 0, got: " + daysInPeriod);
        }
        if (daysInSegment <= 0) {
            throw new IllegalArgumentException("daysInSegment must be > 0, got: " + daysInSegment);
        }
        requireNonNull(workingTimePercentage, "workingTimePercentage");
        requireNonNull(monthlySalaryAmount, "monthlySalaryAmount");
        requireNonNull(employeeInputs, "employeeInputs");
        requireNonBlank(grupoCotizacionCode, "grupoCotizacionCode");
        requireNonBlank(tipoNomina, "tipoNomina");
        requireNonNull(precomputedDirectAmounts, "precomputedDirectAmounts");
        this.ruleSystemCode = ruleSystemCode;
        this.employeeTypeCode = employeeTypeCode;
        this.employeeNumber = employeeNumber;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.segmentStart = segmentStart;
        this.segmentEnd = segmentEnd;
        this.firstSegment = firstSegment;
        this.lastSegment = lastSegment;
        this.daysInPeriod = daysInPeriod;
        this.daysInSegment = daysInSegment;
        this.workingTimePercentage = workingTimePercentage;
        this.monthlySalaryAmount = monthlySalaryAmount;
        this.employeeInputs = Map.copyOf(employeeInputs);
        this.grupoCotizacionCode = grupoCotizacionCode;
        this.tipoNomina = tipoNomina;
        this.precomputedDirectAmounts = Map.copyOf(precomputedDirectAmounts);
        this.extraPaymentsProrated = extraPaymentsProrated;
        // Puede venir nulo, y no se valida: es opcional en el perfil de la empresa y hay empresas
        // sin el. Quien lo necesita -el tipo de accidentes de trabajo- es quien tiene que decir
        // que le falta, y decirlo con el codigo de la empresa dentro (backend#122).
        this.cnaeCode = cnaeCode;
        // Igual que el CNAE: puede venir nulo y no se valida aqui. El unico que lo necesita es el
        // tipo de desempleo, y es el quien tiene que decir que falta (backend#124).
        this.contractCode = contractCode;
    }

    public String getRuleSystemCode() { return ruleSystemCode; }
    public String getEmployeeTypeCode() { return employeeTypeCode; }
    public String getEmployeeNumber() { return employeeNumber; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public LocalDate getSegmentStart() { return segmentStart; }
    public LocalDate getSegmentEnd() { return segmentEnd; }
    public boolean isFirstSegment() { return firstSegment; }
    public boolean isLastSegment() { return lastSegment; }
    public long getDaysInPeriod() { return daysInPeriod; }
    public long getDaysInSegment() { return daysInSegment; }
    public BigDecimal getWorkingTimePercentage() { return workingTimePercentage; }
    public BigDecimal getMonthlySalaryAmount() { return monthlySalaryAmount; }
    public Map<String, BigDecimal> getEmployeeInputs() { return employeeInputs; }
    public String getGrupoCotizacionCode() { return grupoCotizacionCode; }
    public String getTipoNomina() { return tipoNomina; }
    public Map<String, BigDecimal> getPrecomputedDirectAmounts() { return precomputedDirectAmounts; }

    /**
     * La actividad economica de la empresa del empleado, en CNAE ({@code backend#122}).
     *
     * <p>Se resuelve una vez por unidad de calculo y viaja por el contexto por lo mismo que el
     * grupo de cotizacion: es un dato del que dependen tipos, no un calculo. De el sale el tipo
     * de la cuota de accidentes de trabajo, buscando en la tarifa de primas.
     *
     * <p><b>Puede ser nulo</b>: el perfil de la empresa no lo exige y hay empresas sin el.
     */
    public String getCnaeCode() { return cnaeCode; }

    /**
     * El contrato vigente en este tramo ({@code backend#124}).
     *
     * <p>Viaja por el tramo y no por la asignacion de conceptos porque el contrato puede cambiar a
     * mitad de mes, igual que el regimen de pagas extras (ADR-070 §2). De el sale la modalidad de
     * desempleo -indefinida o de duracion determinada- y con ella el tipo.
     *
     * <p><b>Puede ser nulo</b> en los contextos que no lo traen.
     */
    public String getContractCode() { return contractCode; }

    /**
     * Si en este tramo las pagas extras del empleado se prorratean ({@code backend#118}).
     *
     * <p>Va en el tramo y no en la asignacion de conceptos porque el regimen cambia a mitad de
     * mes y el plan se arma una vez para el periodo entero (ADR-070).
     */
    public boolean isExtraPaymentsProrated() { return extraPaymentsProrated; }
}

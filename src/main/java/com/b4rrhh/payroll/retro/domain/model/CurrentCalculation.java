package com.b4rrhh.payroll.retro.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * <b>Lo que un mes cerrado vale hoy</b> ({@code backend#131}, ADR-076).
 *
 * <p>Tiene la misma forma que un recibo y no es un recibo, y esa distincion es la decision del paso:
 *
 * <blockquote>
 * El <b>recibo</b> es un documento: inmutable, se entrega, tiene PDF.<br>
 * El <b>calculo vigente</b> es estado: mutable, se pisa, no lo ve nadie.
 * </blockquote>
 *
 * <p>Confundirlas es como se pudren los motores de nomina. Un motor que deja recalcular el recibo de
 * agosto «porque hay que corregirlo» pierde la unica copia de lo que se le entrego al empleado, y a
 * partir de ahi ya no se puede explicar una nomina: el numero que el empleado tiene impreso deja de
 * existir en el sistema. Un motor que en cambio se niega a recalcular agosto no puede pagar un
 * atraso. La salida es tener las dos cosas, y que no se toquen.
 *
 * <p><b>No se pierde nada al pisar el vigente</b>: la historia vive en los recibos, cada linea de
 * atraso guardada en su mes con su periodo de origen ({@code backend#133}).
 */
public class CurrentCalculation {

    private final Long id;
    private final String ruleSystemCode;
    private final String employeeTypeCode;
    private final String employeeNumber;
    private final String payrollPeriodCode;
    private final String payrollTypeCode;
    private final Integer presenceNumber;
    private final Instant calculatedAt;

    /**
     * El run del periodo <b>abierto</b> que disparo la retro, no el del mes recalculado.
     *
     * <p>Un lanzamiento de septiembre escribe vigentes de junio, julio y agosto, y los tres llevan el
     * run de septiembre. Por eso el periodo y el run son dos datos y ninguno se deduce del otro.
     */
    private final Long runId;

    private final List<CurrentCalculationConcept> concepts;

    private CurrentCalculation(
            Long id,
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String payrollPeriodCode,
            String payrollTypeCode,
            Integer presenceNumber,
            Instant calculatedAt,
            Long runId,
            List<CurrentCalculationConcept> concepts
    ) {
        this.id = id;
        this.ruleSystemCode = ruleSystemCode;
        this.employeeTypeCode = employeeTypeCode;
        this.employeeNumber = employeeNumber;
        this.payrollPeriodCode = payrollPeriodCode;
        this.payrollTypeCode = payrollTypeCode;
        this.presenceNumber = presenceNumber;
        this.calculatedAt = calculatedAt;
        this.runId = runId;
        this.concepts = List.copyOf(concepts);
    }

    public static CurrentCalculation create(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String payrollPeriodCode,
            String payrollTypeCode,
            Integer presenceNumber,
            Instant calculatedAt,
            Long runId,
            List<CurrentCalculationConcept> concepts
    ) {
        return new CurrentCalculation(null, ruleSystemCode, employeeTypeCode, employeeNumber,
                payrollPeriodCode, payrollTypeCode, presenceNumber, calculatedAt, runId, concepts);
    }

    public static CurrentCalculation rehydrate(
            Long id,
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String payrollPeriodCode,
            String payrollTypeCode,
            Integer presenceNumber,
            Instant calculatedAt,
            Long runId,
            List<CurrentCalculationConcept> concepts
    ) {
        return new CurrentCalculation(id, ruleSystemCode, employeeTypeCode, employeeNumber,
                payrollPeriodCode, payrollTypeCode, presenceNumber, calculatedAt, runId, concepts);
    }

    public Long getId() { return id; }
    public String getRuleSystemCode() { return ruleSystemCode; }
    public String getEmployeeTypeCode() { return employeeTypeCode; }
    public String getEmployeeNumber() { return employeeNumber; }
    public String getPayrollPeriodCode() { return payrollPeriodCode; }
    public String getPayrollTypeCode() { return payrollTypeCode; }
    public Integer getPresenceNumber() { return presenceNumber; }
    public Instant getCalculatedAt() { return calculatedAt; }
    public Long getRunId() { return runId; }
    public List<CurrentCalculationConcept> getConcepts() { return concepts; }
}

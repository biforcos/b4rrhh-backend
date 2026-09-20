package com.b4rrhh.payroll.domain.model;

import com.b4rrhh.payroll.domain.exception.InvalidPayrollArgumentException;

import java.math.BigDecimal;

public class PayrollConcept {

    private final Integer lineNumber;
    private final String conceptCode;
    /**
     * El identificador del concepto en el motor ({@code backend#109}).
     *
     * <p>Esta junto al literal y no en su lugar: <b>dos campos, dos trabajos</b>. El mnemonico es
     * lo que las reglas referencian para encontrar un concepto y lo que el grafo y la pestana
     * «Calculo» usan para casar una linea con sus pasos. Hasta el backend#109 era lo unico que
     * habia, ocupando el hueco del nombre.
     */
    private final String conceptMnemonic;
    /**
     * Como se llamaba el concepto cuando se calculo esta linea ({@code backend#109}).
     *
     * <p><b>Se congela y no se resuelve al leer.</b> En cuanto exista el PDF, el recibo tiene un
     * gemelo fisico fuera del sistema: el papel que tiene el empleado dice «Salario base». Si la
     * pantalla dijera otra cosa porque alguien toco el catalogo, la pantalla estaria mintiendo
     * sobre lo que se entrego.
     *
     * <p>No hace falta ninguna regla nueva para eso: se congela por el mismo acto que congela
     * todo lo demas —el calculo— y el cierre lo hace permanente (ADR-059, ADR-062). Un recibo
     * {@code CALCULADA} que se recalcula coge el literal nuevo, y esta bien, porque todavia no se
     * ha entregado nada.
     */
    private final String conceptLabel;
    private final BigDecimal amount;
    private final BigDecimal quantity;
    private final BigDecimal rate;
    private final String conceptNatureCode;
    private final String originPeriodCode;
    private final Integer displayOrder;
    /**
     * El bloque del modelo oficial en el que se imprimio esta linea ({@code backend#109}).
     *
     * <p>Viaja congelado como {@code displayOrder} y como la naturaleza, y por lo mismo: el bloque
     * en el que sale una linea es parte del documento, no una decision que se tome al pintarlo. El
     * PDF lee el recibo y no le pregunta nada al catalogo.
     *
     * <p><b>Puede ser nulo</b>, y entonces hay que verlo: significa que la naturaleza del concepto
     * no tenia seccion declarada. Colocar la linea por defecto en un bloque donde no pinta nada
     * seria esconder eso.
     */
    private final String payslipSectionCode;
    /**
     * De cuantos pasos del motor viene esta linea ({@code backend#103}).
     *
     * <p>Uno en la inmensa mayoria. Mas de uno cuando el folio ha fundido varios tramos del
     * mismo concepto al mismo precio, que pueden <b>no ser contiguos</b>: la linea es correcta
     * y cuenta una historia falsa si nada dice que es una suma.
     */
    private final Integer mergedStepCount;

    public PayrollConcept(
            Integer lineNumber,
            String conceptCode,
            String conceptMnemonic,
            String conceptLabel,
            BigDecimal amount,
            BigDecimal quantity,
            BigDecimal rate,
            String conceptNatureCode,
            String originPeriodCode,
            Integer displayOrder
    ) {
        this(lineNumber, conceptCode, conceptMnemonic, conceptLabel, amount, quantity, rate,
                conceptNatureCode, originPeriodCode, displayOrder, 1, null);
    }

    public PayrollConcept(
            Integer lineNumber,
            String conceptCode,
            String conceptMnemonic,
            String conceptLabel,
            BigDecimal amount,
            BigDecimal quantity,
            BigDecimal rate,
            String conceptNatureCode,
            String originPeriodCode,
            Integer displayOrder,
            Integer mergedStepCount
    ) {
        this(lineNumber, conceptCode, conceptMnemonic, conceptLabel, amount, quantity, rate,
                conceptNatureCode, originPeriodCode, displayOrder, mergedStepCount, null);
    }

    /** El constructor completo, con los pasos que la linea funde y el bloque en el que sale. */
    public PayrollConcept(
            Integer lineNumber,
            String conceptCode,
            String conceptMnemonic,
            String conceptLabel,
            BigDecimal amount,
            BigDecimal quantity,
            BigDecimal rate,
            String conceptNatureCode,
            String originPeriodCode,
            Integer displayOrder,
            Integer mergedStepCount,
            String payslipSectionCode
    ) {
        this.lineNumber = requirePositive(lineNumber, "lineNumber");
        this.conceptCode = requireCode(conceptCode, "conceptCode", 30);
        this.conceptMnemonic = requireText(conceptMnemonic, "conceptMnemonic", 50);
        this.conceptLabel = requireText(conceptLabel, "conceptLabel", 200);
        this.amount = requireAmount(amount, "amount");
        this.quantity = normalizeDecimal(quantity, "quantity");
        this.rate = normalizeDecimal(rate, "rate");
        this.conceptNatureCode = requireCode(conceptNatureCode, "conceptNatureCode", 30);
        this.originPeriodCode = normalizeOptional(originPeriodCode, "originPeriodCode", 30);
        this.displayOrder = requirePositive(displayOrder, "displayOrder");
        this.mergedStepCount = requirePositive(mergedStepCount, "mergedStepCount");
        this.payslipSectionCode = normalizeOptional(payslipSectionCode, "payslipSectionCode", 30);
    }

    private static Integer requirePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new InvalidPayrollArgumentException(fieldName + " must be a positive integer");
        }
        return value;
    }

    private static String requireCode(String value, String fieldName, int maxLength) {
        String normalized = normalizeRequired(value, fieldName, maxLength);
        return normalized.toUpperCase();
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        return normalizeRequired(value, fieldName, maxLength);
    }

    private static String normalizeRequired(String value, String fieldName, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidPayrollArgumentException(fieldName + " is required");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new InvalidPayrollArgumentException(fieldName + " exceeds max length " + maxLength);
        }
        return normalized;
    }

    private static String normalizeOptional(String value, String fieldName, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new InvalidPayrollArgumentException(fieldName + " exceeds max length " + maxLength);
        }
        return normalized.toUpperCase();
    }

    private static BigDecimal requireAmount(BigDecimal value, String fieldName) {
        BigDecimal normalized = normalizeDecimal(value, fieldName);
        if (normalized == null) {
            throw new InvalidPayrollArgumentException(fieldName + " is required");
        }
        return normalized;
    }

    private static BigDecimal normalizeDecimal(BigDecimal value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value.scale() > 6) {
            throw new InvalidPayrollArgumentException(fieldName + " scale exceeds 6 decimals");
        }
        return value.stripTrailingZeros();
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public String getConceptCode() {
        return conceptCode;
    }

    public String getConceptMnemonic() {
        return conceptMnemonic;
    }

    public String getConceptLabel() {
        return conceptLabel;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public String getConceptNatureCode() {
        return conceptNatureCode;
    }

    public String getOriginPeriodCode() {
        return originPeriodCode;
    }

    public Integer getMergedStepCount() {
        return mergedStepCount;
    }

    /** Si esta linea es la suma de varios pasos y por tanto tiene que decirlo. */
    public boolean isMerged() {
        return mergedStepCount != null && mergedStepCount > 1;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public String getPayslipSectionCode() {
        return payslipSectionCode;
    }
}
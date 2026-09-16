package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.concept.domain.model.CalculationType;
import com.b4rrhh.payroll_engine.concept.domain.model.OperandRole;
import com.b4rrhh.payroll_engine.dependency.domain.model.ConceptNodeIdentity;
import com.b4rrhh.payroll_engine.execution.domain.exception.MissingConceptResultException;
import com.b4rrhh.payroll_engine.execution.domain.exception.MissingPlannedOperandException;
import com.b4rrhh.payroll_engine.execution.domain.model.ConceptExecutionPlanEntry;
import com.b4rrhh.payroll_engine.execution.domain.model.SegmentExecutionState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link PercentageConceptResolver}.
 *
 * <p>Reference: base × percentage / 100, rounded to scale 2 HALF_UP.
 */
class PercentageConceptResolverTest {

    private static final String RS = "ESP";

    private final PercentageConceptResolver resolver = new PercentageConceptResolver();

    private static ConceptNodeIdentity id(String code) {
        return new ConceptNodeIdentity(RS, code);
    }

    private static ConceptExecutionPlanEntry entry(String target, String baseCode, String pctCode) {
        return new ConceptExecutionPlanEntry(
                id(target),
                CalculationType.PERCENTAGE,
                Map.of(OperandRole.BASE, id(baseCode), OperandRole.PERCENTAGE, id(pctCode))
        );
    }

    private static SegmentExecutionState stateWith(String baseCode, BigDecimal base,
                                                    String pctCode, BigDecimal pct) {
        SegmentExecutionState state = new SegmentExecutionState();
        state.storeResult(id(baseCode), base);
        state.storeResult(id(pctCode), pct);
        return state;
    }

    // ── happy path ─────────────────────────────────────────────────────────

    @Test
    void percentageComputationIsCorrectForReferenceSegmentOne() {
        // 1038.33 × 15 / 100 = 155.7495, y el resolutor ya no lo redondea (backend#61)
        ConceptExecutionPlanEntry e = entry("RETENCION_IRPF_TRAMO", "TOTAL_DEVENGOS_SEGMENTO", "T_PCT_IRPF");
        SegmentExecutionState state = stateWith(
                "TOTAL_DEVENGOS_SEGMENTO", new BigDecimal("1038.33"),
                "T_PCT_IRPF", new BigDecimal("15"));

        BigDecimal result = resolver.resolve(e, state);

        assertEquals(0, new BigDecimal("155.74950000").compareTo(result),
                "sin redondear: 1038,33 x 15 / 100 = 155,7495");
    }

    @Test
    void percentageComputationIsCorrectForReferenceSegmentTwo() {
        // 653.33 × 15 / 100 = 97.9995, y el resolutor ya no lo redondea (backend#61)
        ConceptExecutionPlanEntry e = entry("RETENCION_IRPF_TRAMO", "TOTAL_DEVENGOS_SEGMENTO", "T_PCT_IRPF");
        SegmentExecutionState state = stateWith(
                "TOTAL_DEVENGOS_SEGMENTO", new BigDecimal("653.33"),
                "T_PCT_IRPF", new BigDecimal("15"));

        BigDecimal result = resolver.resolve(e, state);

        assertEquals(0, new BigDecimal("97.99950000").compareTo(result),
                "sin redondear: 653,33 x 15 / 100 = 97,9995");
    }

    @Test
    void zeroBaseProducesZeroResult() {
        ConceptExecutionPlanEntry e = entry("RETENCION_IRPF_TRAMO", "TOTAL_DEVENGOS_SEGMENTO", "T_PCT_IRPF");
        SegmentExecutionState state = stateWith(
                "TOTAL_DEVENGOS_SEGMENTO", BigDecimal.ZERO,
                "T_PCT_IRPF", new BigDecimal("15"));

        BigDecimal result = resolver.resolve(e, state);

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void zeroPercentageProducesZeroResult() {
        ConceptExecutionPlanEntry e = entry("RETENCION_IRPF_TRAMO", "TOTAL_DEVENGOS_SEGMENTO", "T_PCT_IRPF");
        SegmentExecutionState state = stateWith(
                "TOTAL_DEVENGOS_SEGMENTO", new BigDecimal("1038.33"),
                "T_PCT_IRPF", BigDecimal.ZERO);

        BigDecimal result = resolver.resolve(e, state);

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    /**
     * El resultado sale <b>exacto</b> (backend#61).
     *
     * <p>La division entre 100 sigue con escala 8, porque es un paso intermedio de esta misma
     * operacion; lo que ya no hace este resolutor es redondear el resultado del concepto. De eso se
     * encarga el motor una vez, con los decimales que el concepto declara, y por eso la mitad de
     * los porcentajes de una nomina pueden quedarse con mas de dos decimales si asi se declaran.
     */
    @Test
    void theResultIsExactAndTheEngineRoundsItLater() {
        ConceptExecutionPlanEntry e = entry("R", "B", "P");
        SegmentExecutionState state = stateWith(
                "B", new BigDecimal("100.00"),
                "P", new BigDecimal("33.333"));

        BigDecimal result = resolver.resolve(e, state);

        assertEquals(0, new BigDecimal("33.33300000").compareTo(result),
                "el resolutor no redondea: 100 x 33,333 / 100 = 33,333");
    }

    // ── fail-fast: missing planned operand ─────────────────────────────────

    @Test
    void missingBasePlannedOperandThrows() {
        // Entry wired with PERCENTAGE only, BASE absent
        ConceptExecutionPlanEntry e = new ConceptExecutionPlanEntry(
                id("RETENCION_IRPF_TRAMO"),
                CalculationType.PERCENTAGE,
                Map.of(OperandRole.PERCENTAGE, id("T_PCT_IRPF"))
        );
        SegmentExecutionState state = new SegmentExecutionState();
        state.storeResult(id("T_PCT_IRPF"), new BigDecimal("15"));

        assertThrows(MissingPlannedOperandException.class, () -> resolver.resolve(e, state));
    }

    @Test
    void missingPercentagePlannedOperandThrows() {
        // Entry wired with BASE only, PERCENTAGE absent
        ConceptExecutionPlanEntry e = new ConceptExecutionPlanEntry(
                id("RETENCION_IRPF_TRAMO"),
                CalculationType.PERCENTAGE,
                Map.of(OperandRole.BASE, id("TOTAL_DEVENGOS_SEGMENTO"))
        );
        SegmentExecutionState state = new SegmentExecutionState();
        state.storeResult(id("TOTAL_DEVENGOS_SEGMENTO"), new BigDecimal("1038.33"));

        assertThrows(MissingPlannedOperandException.class, () -> resolver.resolve(e, state));
    }

    // ── fail-fast: missing source amount in state ───────────────────────────

    @Test
    void missingBaseAmountInStateThrows() {
        ConceptExecutionPlanEntry e = entry("RETENCION_IRPF_TRAMO", "TOTAL_DEVENGOS_SEGMENTO", "T_PCT_IRPF");
        // State has PERCENTAGE source but NOT base
        SegmentExecutionState state = new SegmentExecutionState();
        state.storeResult(id("T_PCT_IRPF"), new BigDecimal("15"));

        assertThrows(MissingConceptResultException.class, () -> resolver.resolve(e, state));
    }

    @Test
    void missingPercentageAmountInStateThrows() {
        ConceptExecutionPlanEntry e = entry("RETENCION_IRPF_TRAMO", "TOTAL_DEVENGOS_SEGMENTO", "T_PCT_IRPF");
        // State has BASE source but NOT percentage
        SegmentExecutionState state = new SegmentExecutionState();
        state.storeResult(id("TOTAL_DEVENGOS_SEGMENTO"), new BigDecimal("1038.33"));

        assertThrows(MissingConceptResultException.class, () -> resolver.resolve(e, state));
    }
}

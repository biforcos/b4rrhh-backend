package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.concept.domain.model.CalculationType;
import com.b4rrhh.payroll_engine.concept.domain.model.OperandRole;
import com.b4rrhh.payroll_engine.dependency.domain.model.ConceptNodeIdentity;
import com.b4rrhh.payroll_engine.execution.domain.exception.MissingConceptResultException;
import com.b4rrhh.payroll_engine.execution.domain.exception.MissingPlannedOperandException;
import com.b4rrhh.payroll_engine.execution.domain.exception.UnsupportedTechnicalConceptException;
import com.b4rrhh.payroll_engine.execution.domain.model.AggregateSourceEntry;
import com.b4rrhh.payroll_engine.execution.domain.model.ConceptExecutionPlanEntry;
import com.b4rrhh.payroll_engine.execution.domain.model.SegmentExecutionState;
import com.b4rrhh.payroll_engine.segment.domain.model.SegmentCalculationContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DefaultSegmentExecutionEngine}.
 *
 * <p>Reference values:
 * <ul>
 *   <li>Monthly salary: 2000 €, 30 days in period, 14 days in segment, 100% working time</li>
 *   <li>T_DIAS_PRESENCIA_SEGMENTO = 14</li>
 *   <li>T_PRECIO_DIA = 2000 / 30 * 100/100 = 66.66666667 (scale 8 HALF_UP)</li>
 *   <li>SALARIO_BASE = 14 * 66.66666667 = 933.33 (scale 2 HALF_UP)</li>
 * </ul>
 */
class SegmentExecutionEngineTest {

    private static final String RULE_SYSTEM = "ESP";

    private final DefaultSegmentExecutionEngine engine =
            new DefaultSegmentExecutionEngine(
                    new SegmentTechnicalValueResolver(),
                    new RateByQuantityOperandResolver(),
                    new PercentageConceptResolver(),
                    new GreatestConceptResolver(),
                    new LeastConceptResolver(),
                    new TechnicalConceptCalculatorRegistry(List.of()));

    /** Builds an enriched SALARIO_BASE plan entry with QUANTITY and RATE operand wiring. */
    private static ConceptExecutionPlanEntry enrichedSalarioBaseEntry() {
        return new ConceptExecutionPlanEntry(
                node("SALARIO_BASE"),
                CalculationType.RATE_BY_QUANTITY,
                Map.of(
                        OperandRole.QUANTITY, node("T_DIAS_PRESENCIA_SEGMENTO"),
                        OperandRole.RATE,     node("T_PRECIO_DIA")
                )
        );
    }

    /** Builds an enriched PLUS_TRANSPORTE plan entry with QUANTITY=T_DIAS and RATE=T_PRECIO_TRANSPORTE. */
    private static ConceptExecutionPlanEntry enrichedPlusTransporteEntry() {
        return new ConceptExecutionPlanEntry(
                node("PLUS_TRANSPORTE"),
                CalculationType.RATE_BY_QUANTITY,
                Map.of(
                        OperandRole.QUANTITY, node("T_DIAS_PRESENCIA_SEGMENTO"),
                        OperandRole.RATE,     node("T_PRECIO_TRANSPORTE")
                )
        );
    }

    /**
     * {@code T_PRECIO_DIA} declarando sus ocho decimales (backend#61).
     *
     * <p>Desde el #61 el motor redondea <b>todo</b> concepto con los decimales que el concepto
     * declara, tambien los {@code DIRECT_AMOUNT} y los {@code ENGINE_PROVIDED}, que antes salian
     * tal cual. Un precio intermedio que necesita ocho decimales los declara; lo que ya no puede
     * pasar es que un valor lleve mas precision de la que dice llevar.
     *
     * <p>Sin esto, estos tests calcularian {@code 66,67} y arrastrarian el error hasta el salario:
     * son la demostracion de que la propiedad se lee.
     */
    private ConceptExecutionPlanEntry precioDiaConOchoDecimales() {
        return new ConceptExecutionPlanEntry(
                node("T_PRECIO_DIA"),
                CalculationType.DIRECT_AMOUNT,
                java.util.Map.of(),
                java.util.List.of(),
                new com.b4rrhh.payroll_engine.concept.domain.model.ConceptRounding(
                        8, java.math.RoundingMode.HALF_UP));
    }

    /** Builds an AGGREGATE TOTAL_DEVENGOS_SEGMENTO entry sourced from SALARIO_BASE and PLUS_TRANSPORTE. */
    private static ConceptExecutionPlanEntry aggregateTotalDevengosEntry() {
        return new ConceptExecutionPlanEntry(
                node("TOTAL_DEVENGOS_SEGMENTO"),
                CalculationType.AGGREGATE,
                Map.of(),
                List.of(new AggregateSourceEntry(node("SALARIO_BASE"), false),
                        new AggregateSourceEntry(node("PLUS_TRANSPORTE"), false))
        );
    }

    private static ConceptNodeIdentity node(String code) {
        return new ConceptNodeIdentity(RULE_SYSTEM, code);
    }

    private static SegmentCalculationContext context100pct(int daysInPeriod, int daysInSegment, BigDecimal salary) {
        LocalDate periodStart = LocalDate.of(2026, 4, 1);
        LocalDate periodEnd   = LocalDate.of(2026, 4, 30);
        LocalDate segStart    = LocalDate.of(2026, 4, 1);
        LocalDate segEnd      = segStart.plusDays(daysInSegment - 1);
        return new SegmentCalculationContext(
                RULE_SYSTEM, "EMP", "EMP0001",
                periodStart, periodEnd, segStart, segEnd,
                true, true,
                daysInPeriod, daysInSegment,
                new BigDecimal("100"),
                salary,
                Map.of(),
                "G02",
                "MENSUAL",
                Map.of()
        );
    }

    // ── T_DIAS_PRESENCIA_SEGMENTO (DIRECT_AMOUNT) ────────────────────────────

    @Test
    void directAmountDiasPresenciaReturnsSegmentDays() {
        SegmentCalculationContext ctx = context100pct(30, 14, new BigDecimal("2000.00"));
        List<ConceptExecutionPlanEntry> plan = List.of(
                new ConceptExecutionPlanEntry(node("T_DIAS_PRESENCIA_SEGMENTO"), CalculationType.DIRECT_AMOUNT)
        );

        SegmentExecutionState state = engine.execute(plan, ctx);

        assertEquals(0, new BigDecimal("14").compareTo(
                state.getRequiredAmount(node("T_DIAS_PRESENCIA_SEGMENTO"))));
    }

    // ── T_PRECIO_DIA (DIRECT_AMOUNT) ─────────────────────────────────────────

    @Test
    void directAmountPrecioDiaAt100PctIsCorrect() {
        // 2000 / 30 * 1.0 = 66.66666667 (scale 8)
        SegmentCalculationContext ctx = context100pct(30, 14, new BigDecimal("2000.00"));
        List<ConceptExecutionPlanEntry> plan = List.of(
                precioDiaConOchoDecimales()
        );

        SegmentExecutionState state = engine.execute(plan, ctx);

        assertEquals(0, new BigDecimal("66.66666667").compareTo(
                state.getRequiredAmount(node("T_PRECIO_DIA"))));
    }

    @Test
    void directAmountPrecioDiaAt50PctIsHalfOfFull() {
        // 2000 / 30 * 0.5 = 33.33333334
        LocalDate s = LocalDate.of(2026, 4, 15);
        LocalDate e = LocalDate.of(2026, 4, 30);
        SegmentCalculationContext ctx = new SegmentCalculationContext(
                RULE_SYSTEM, "EMP", "EMP0001",
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30),
                s, e, false, true, 30, 16,
                new BigDecimal("50"),
                new BigDecimal("2000.00"),
                Map.of(),
                "G02",
                "MENSUAL",
                Map.of()
        );
        List<ConceptExecutionPlanEntry> plan = List.of(
                precioDiaConOchoDecimales()
        );

        SegmentExecutionState state = engine.execute(plan, ctx);

        assertEquals(0, new BigDecimal("33.33333334").compareTo(
                state.getRequiredAmount(node("T_PRECIO_DIA"))));
    }

    // ── SALARIO_BASE (RATE_BY_QUANTITY) ──────────────────────────────────────

    @Test
    void salarioBaseIsComputedAfterDependencies() {
        // 14 * 66.66666667 = 933.33333338 → scale 2 HALF_UP = 933.33
        SegmentCalculationContext ctx = context100pct(30, 14, new BigDecimal("2000.00"));
        List<ConceptExecutionPlanEntry> plan = List.of(
                new ConceptExecutionPlanEntry(node("T_DIAS_PRESENCIA_SEGMENTO"), CalculationType.DIRECT_AMOUNT),
                precioDiaConOchoDecimales(),
                enrichedSalarioBaseEntry()
        );

        SegmentExecutionState state = engine.execute(plan, ctx);

        assertEquals(0, new BigDecimal("933.33").compareTo(
                state.getRequiredAmount(node("SALARIO_BASE"))));
    }

    @Test
    void salarioBaseAt50PctIs533_33() {
        // 16 * 33.33333334 = 533.33333344 → scale 2 HALF_UP = 533.33
        LocalDate s = LocalDate.of(2026, 4, 15);
        LocalDate e = LocalDate.of(2026, 4, 30);
        SegmentCalculationContext ctx = new SegmentCalculationContext(
                RULE_SYSTEM, "EMP", "EMP0001",
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30),
                s, e, false, true, 30, 16,
                new BigDecimal("50"),
                new BigDecimal("2000.00"),
                Map.of(),
                "G02",
                "MENSUAL",
                Map.of()
        );
        List<ConceptExecutionPlanEntry> plan = List.of(
                new ConceptExecutionPlanEntry(node("T_DIAS_PRESENCIA_SEGMENTO"), CalculationType.DIRECT_AMOUNT),
                precioDiaConOchoDecimales(),
                enrichedSalarioBaseEntry()
        );

        SegmentExecutionState state = engine.execute(plan, ctx);

        assertEquals(0, new BigDecimal("533.33").compareTo(
                state.getRequiredAmount(node("SALARIO_BASE"))));
    }

    // ── error cases ──────────────────────────────────────────────────────────

    @Test
    void salarioBaseMissingDependencyThrows() {
        SegmentCalculationContext ctx = context100pct(30, 14, new BigDecimal("2000.00"));
        // SALARIO_BASE entry has operand wiring, but dependencies are absent from state
        List<ConceptExecutionPlanEntry> plan = List.of(
                enrichedSalarioBaseEntry()
        );

        assertThrows(MissingConceptResultException.class, () -> engine.execute(plan, ctx));
    }

    @Test
    void percentageWithoutOperandWiringThrowsMissingPlannedOperandException() {
        // PERCENTAGE entry created without operand wiring (as if the plan was not built by DefaultExecutionPlanBuilder).
        // This is the closest reachable analogue to the former unsupported-type test:
        // all current CalculationType values are handled by the engine, so the default branch
        // is only reachable if a new enum value is added and the engine switch is not updated.
        SegmentCalculationContext ctx = context100pct(30, 14, new BigDecimal("2000.00"));
        List<ConceptExecutionPlanEntry> plan = List.of(
                new ConceptExecutionPlanEntry(node("SOME_CONCEPT"), CalculationType.PERCENTAGE)
        );

        assertThrows(MissingPlannedOperandException.class, () -> engine.execute(plan, ctx));
    }

    @Test
    void unsupportedDirectAmountConceptCodeThrows() {
        SegmentCalculationContext ctx = context100pct(30, 14, new BigDecimal("2000.00"));
        List<ConceptExecutionPlanEntry> plan = List.of(
                new ConceptExecutionPlanEntry(node("UNKNOWN_CONCEPT"), CalculationType.DIRECT_AMOUNT)
        );

        assertThrows(UnsupportedTechnicalConceptException.class, () -> engine.execute(plan, ctx));
    }

    @Test
    void stateContainsAllThreeConceptsFromFullPlan() {
        SegmentCalculationContext ctx = context100pct(30, 14, new BigDecimal("2000.00"));
        List<ConceptExecutionPlanEntry> plan = List.of(
                new ConceptExecutionPlanEntry(node("T_DIAS_PRESENCIA_SEGMENTO"), CalculationType.DIRECT_AMOUNT),
                precioDiaConOchoDecimales(),
                enrichedSalarioBaseEntry()
        );

        SegmentExecutionState state = engine.execute(plan, ctx);

        assertTrue(state.getOptionalAmount(node("T_DIAS_PRESENCIA_SEGMENTO")).isPresent());
        assertTrue(state.getOptionalAmount(node("T_PRECIO_DIA")).isPresent());
        assertTrue(state.getOptionalAmount(node("SALARIO_BASE")).isPresent());
    }

    // ── AGGREGATE ──────────────────────────────────────────────────

    @Test
    void aggregateConceptSumsTwoPriorSources() {
        // Segment 1 reference values: SALARIO_BASE = 933.33, PLUS_TRANSPORTE = 14 * 7.50 = 105.00
        // TOTAL_DEVENGOS_SEGMENTO = 933.33 + 105.00 = 1038.33
        SegmentCalculationContext ctx = context100pct(30, 14, new BigDecimal("2000.00"));
        List<ConceptExecutionPlanEntry> plan = List.of(
                new ConceptExecutionPlanEntry(node("T_DIAS_PRESENCIA_SEGMENTO"),  CalculationType.DIRECT_AMOUNT),
                precioDiaConOchoDecimales(),
                enrichedSalarioBaseEntry(),
                new ConceptExecutionPlanEntry(node("T_PRECIO_TRANSPORTE"),       CalculationType.DIRECT_AMOUNT),
                enrichedPlusTransporteEntry(),
                aggregateTotalDevengosEntry()
        );

        SegmentExecutionState state = engine.execute(plan, ctx);

        assertEquals(0, new BigDecimal("1038.33").compareTo(
                state.getRequiredAmount(node("TOTAL_DEVENGOS_SEGMENTO"))),
                "TOTAL_DEVENGOS_SEGMENTO must be the sum of SALARIO_BASE + PLUS_TRANSPORTE");
    }

    @Test
    void aggregateMissingSourceInStateThrows() {
        // Execute TOTAL_DEVENGOS_SEGMENTO before its sources are in state
        SegmentCalculationContext ctx = context100pct(30, 14, new BigDecimal("2000.00"));
        List<ConceptExecutionPlanEntry> plan = List.of(
                aggregateTotalDevengosEntry()
        );

        assertThrows(MissingConceptResultException.class, () -> engine.execute(plan, ctx));
    }

    // ── PERCENTAGE ─────────────────────────────────────────────────────────

    /** Builds an enriched RETENCION_IRPF_TRAMO entry: BASE=TOTAL_DEVENGOS_SEGMENTO, PERCENTAGE=T_PCT_IRPF. */
    private static ConceptExecutionPlanEntry percentageRetencionIrpfEntry() {
        return new ConceptExecutionPlanEntry(
                node("RETENCION_IRPF_TRAMO"),
                CalculationType.PERCENTAGE,
                Map.of(
                        OperandRole.BASE,       node("TOTAL_DEVENGOS_SEGMENTO"),
                        OperandRole.PERCENTAGE, node("T_PCT_IRPF")
                )
        );
    }

    @Test
    void percentageConceptComputesCorrectlyAfterDependencies() {
        // Full plan: all 7 concepts including RETENCION_IRPF_TRAMO
        // Segment 1 reference: TOTAL_DEVENGOS_SEGMENTO = 1038.33, T_PCT_IRPF = 15
        // RETENCION_IRPF_TRAMO = 1038.33 × 15 / 100 = 155.7495 → scale 2 HALF_UP = 155.75
        SegmentCalculationContext ctx = context100pct(30, 14, new BigDecimal("2000.00"));
        List<ConceptExecutionPlanEntry> plan = List.of(
                new ConceptExecutionPlanEntry(node("T_DIAS_PRESENCIA_SEGMENTO"), CalculationType.DIRECT_AMOUNT),
                precioDiaConOchoDecimales(),
                enrichedSalarioBaseEntry(),
                new ConceptExecutionPlanEntry(node("T_PRECIO_TRANSPORTE"),       CalculationType.DIRECT_AMOUNT),
                enrichedPlusTransporteEntry(),
                aggregateTotalDevengosEntry(),
                new ConceptExecutionPlanEntry(node("T_PCT_IRPF"),                CalculationType.DIRECT_AMOUNT),
                percentageRetencionIrpfEntry()
        );

        SegmentExecutionState state = engine.execute(plan, ctx);

        assertEquals(0, new BigDecimal("155.75").compareTo(
                state.getRequiredAmount(node("RETENCION_IRPF_TRAMO"))),
                "RETENCION_IRPF_TRAMO = 1038.33 × 15 / 100 = 155.75");
    }

    @Test
    void percentageMissingSourceInStateThrows() {
        // Execute RETENCION_IRPF_TRAMO before TOTAL_DEVENGOS_SEGMENTO is in state
        SegmentCalculationContext ctx = context100pct(30, 14, new BigDecimal("2000.00"));
        List<ConceptExecutionPlanEntry> plan = List.of(
                percentageRetencionIrpfEntry()
        );

        assertThrows(MissingConceptResultException.class, () -> engine.execute(plan, ctx));
    }

    @Test
    void directAmountUsesPrecomputedValueWhenConceptCodeIsNotTechnical() {
        BigDecimal precioDiaPleno = new BigDecimal("56.32");
        ConceptNodeIdentity priceId = new ConceptNodeIdentity(RULE_SYSTEM, "PRECIO_DIA_PLENO");

        SegmentCalculationContext ctx = new SegmentCalculationContext(
                RULE_SYSTEM, "EMP", "EMP0001",
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30),
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 14),
                true, false,
                30L, 14L,
                new BigDecimal("100"),
                new BigDecimal("2000.00"),
                Map.of(),
                "G02",
                "MENSUAL",
                Map.of("PRECIO_DIA_PLENO", precioDiaPleno)
        );

        List<ConceptExecutionPlanEntry> plan = List.of(
                new ConceptExecutionPlanEntry(priceId, CalculationType.DIRECT_AMOUNT)
        );

        SegmentExecutionState state = engine.execute(plan, ctx);

        assertEquals(0, precioDiaPleno.compareTo(state.getRequiredAmount(priceId)),
                "DIRECT_AMOUNT must return precomputed value when conceptCode is in precomputedDirectAmounts");
    }
}

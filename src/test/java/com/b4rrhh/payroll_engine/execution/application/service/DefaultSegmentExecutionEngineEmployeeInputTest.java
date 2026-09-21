package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.concept.domain.model.CalculationType;
import com.b4rrhh.payroll_engine.dependency.domain.model.ConceptNodeIdentity;
import com.b4rrhh.payroll_engine.execution.domain.model.ConceptExecutionPlanEntry;
import com.b4rrhh.payroll_engine.execution.domain.model.SegmentExecutionState;
import com.b4rrhh.payroll_engine.segment.domain.model.SegmentCalculationContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link DefaultSegmentExecutionEngine} covering the {@code EMPLOYEE_INPUT}
 * calculation type.
 */
class DefaultSegmentExecutionEngineEmployeeInputTest {

    private static final String RULE_SYSTEM = "ESP";

    private static ConceptNodeIdentity node(String code) {
        return new ConceptNodeIdentity(RULE_SYSTEM, code);
    }

    private static SegmentCalculationContext contextWithInputs(Map<String, BigDecimal> employeeInputs) {
        LocalDate periodStart = LocalDate.of(2026, 4, 1);
        LocalDate periodEnd   = LocalDate.of(2026, 4, 30);
        return new SegmentCalculationContext(
                RULE_SYSTEM, "EMP", "EMP0001",
                periodStart, periodEnd,
                periodStart, periodEnd,
                true, true,
                30, 30,
                new BigDecimal("100"),
                new BigDecimal("2000.00"),
                employeeInputs,
                "G02",
                "MENSUAL",
                Map.of(), false
        );
    }

    private final DefaultSegmentExecutionEngine engine =
            new DefaultSegmentExecutionEngine(
                    new SegmentTechnicalValueResolver(),
                    new RateByQuantityOperandResolver(),
                    new PercentageConceptResolver(),
                    new GreatestConceptResolver(),
                    new LeastConceptResolver(),
                    new QuotientConceptResolver(),
                    new TechnicalConceptCalculatorRegistry(List.of()));

    @Test
    void employeeInputConceptReturnsRegisteredQuantity() {
        BigDecimal registeredHours = new BigDecimal("40.00");
        Map<String, BigDecimal> inputs = Map.of("HORAS_EXTRA", registeredHours);
        SegmentCalculationContext ctx = contextWithInputs(inputs);

        List<ConceptExecutionPlanEntry> plan = List.of(
                new ConceptExecutionPlanEntry(node("HORAS_EXTRA"), CalculationType.EMPLOYEE_INPUT)
        );

        SegmentExecutionState state = engine.execute(plan, ctx);

        assertEquals(0, registeredHours.compareTo(state.getRequiredAmount(node("HORAS_EXTRA"))),
                "EMPLOYEE_INPUT concept must return the registered quantity from employeeInputs map");
    }

    @Test
    void employeeInputConceptReturnsZeroWhenConceptNotInMap() {
        SegmentCalculationContext ctx = contextWithInputs(Map.of());

        List<ConceptExecutionPlanEntry> plan = List.of(
                new ConceptExecutionPlanEntry(node("HORAS_EXTRA"), CalculationType.EMPLOYEE_INPUT)
        );

        SegmentExecutionState state = engine.execute(plan, ctx);

        assertEquals(0, BigDecimal.ZERO.compareTo(state.getRequiredAmount(node("HORAS_EXTRA"))),
                "EMPLOYEE_INPUT concept must return BigDecimal.ZERO when concept is not in employeeInputs map");
    }
}

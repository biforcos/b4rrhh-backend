package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.exception.UnsupportedCalculationTypeException;
import com.b4rrhh.payroll_engine.execution.domain.exception.UnsupportedTechnicalConceptException;
import com.b4rrhh.payroll_engine.execution.domain.model.AggregateSourceEntry;
import com.b4rrhh.payroll_engine.execution.domain.model.ConceptExecutionPlanEntry;
import com.b4rrhh.payroll_engine.execution.domain.model.SegmentExecutionState;
import com.b4rrhh.payroll_engine.execution.domain.model.TechnicalConceptSegmentData;
import com.b4rrhh.payroll_engine.segment.domain.model.SegmentCalculationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Default implementation of {@link SegmentExecutionEngine}.
 *
 * <h3>Execution flow</h3>
 * <ol>
 *   <li>Iterates the execution plan in the provided order (must be topological).</li>
 *   <li>For each entry, dispatches based on {@link com.b4rrhh.payroll_engine.concept.domain.model.CalculationType}:
 *       <ul>
 *         <li>{@code DIRECT_AMOUNT} — checks {@code precomputedDirectAmounts} first; falls back to
 *             {@link SegmentTechnicalValueResolver} when the concept code is absent</li>
 *         <li>{@code RATE_BY_QUANTITY} — reads pre-resolved QUANTITY and RATE operand
 *             identities from the plan entry, fetches them from state, multiplies
 *             rate × quantity, rounds to 2 decimal places HALF_UP</li>
 *         <li>{@code PERCENTAGE} — reads pre-resolved BASE and PERCENTAGE operand
 *             identities from the plan entry, fetches them from state, computes
 *             base × percentage / 100, rounds to 2 decimal places HALF_UP</li>
 *         <li>{@code AGGREGATE} — iterates the pre-resolved source list, sums the stored
 *             amount for each source, rounds to 2 decimal places HALF_UP</li>
 *       </ul>
 *   </li>
 *   <li>Stores each result in {@link SegmentExecutionState}.</li>
 * </ol>
 *
 * <h3>In-memory execution</h3>
 * <p>For all calculation types, the engine reads pre-resolved source
 * identities directly from {@link ConceptExecutionPlanEntry} and pre-computed amounts from
 * {@link SegmentExecutionState}. No repository access occurs at runtime.
 *
 * <h3>Rounding policy</h3>
 * <ul>
 *   <li>Intermediate scale: 8, HALF_UP (delegated to {@link SegmentTechnicalValueResolver})</li>
 *   <li>RATE_BY_QUANTITY final result: scale 2, HALF_UP (delegated to {@link RateByQuantityOperandResolver})</li>
 *   <li>PERCENTAGE final result: scale 2, HALF_UP (delegated to {@link PercentageConceptResolver})</li>
 *   <li>AGGREGATE final result: scale 2, HALF_UP (applied in-engine after summation)</li>
 * </ul>
 */
@Component
public class DefaultSegmentExecutionEngine implements SegmentExecutionEngine {

    private final SegmentTechnicalValueResolver technicalValueResolver;
    private final RateByQuantityOperandResolver rateByQuantityResolver;
    private final PercentageConceptResolver percentageConceptResolver;
    private final GreatestConceptResolver greatestConceptResolver;
    private final LeastConceptResolver leastConceptResolver;
    private final TechnicalConceptCalculatorRegistry technicalCalculatorRegistry;

    public DefaultSegmentExecutionEngine(
            SegmentTechnicalValueResolver technicalValueResolver,
            RateByQuantityOperandResolver rateByQuantityResolver,
            PercentageConceptResolver percentageConceptResolver,
            GreatestConceptResolver greatestConceptResolver,
            LeastConceptResolver leastConceptResolver,
            TechnicalConceptCalculatorRegistry technicalCalculatorRegistry
    ) {
        this.technicalValueResolver = technicalValueResolver;
        this.rateByQuantityResolver = rateByQuantityResolver;
        this.percentageConceptResolver = percentageConceptResolver;
        this.greatestConceptResolver = greatestConceptResolver;
        this.leastConceptResolver = leastConceptResolver;
        this.technicalCalculatorRegistry = technicalCalculatorRegistry;
    }

    @Override
    public SegmentExecutionState execute(
            List<ConceptExecutionPlanEntry> plan,
            SegmentCalculationContext context
    ) {
        SegmentExecutionState state = new SegmentExecutionState();

        for (ConceptExecutionPlanEntry entry : plan) {
            state.storeResult(entry.identity(), evaluate(entry, state, context));
        }

        return state;
    }

    @Override
    public BigDecimal evaluate(
            ConceptExecutionPlanEntry entry,
            SegmentExecutionState state,
            SegmentCalculationContext context
    ) {
        return entry.rounding().apply(evaluateExact(entry, state, context));
    }

    /**
     * El valor del concepto <b>antes</b> de redondear.
     *
     * <p>Separado del redondeo a proposito (backend#61, ADR-066). Hasta entonces cada resolutor
     * redondeaba por su cuenta a 2 y {@code HALF_UP}: tres sitios distintos con la misma constante
     * repetida, y ninguna forma de decir que el precio por dia necesita seis decimales y los dias
     * ninguno. Ahora <b>el motor redondea una vez y aqui no se redondea nada</b>, que es lo que
     * hace cierto el invariante de que nada se redondea dos veces.
     */
    private BigDecimal evaluateExact(
            ConceptExecutionPlanEntry entry,
            SegmentExecutionState state,
            SegmentCalculationContext context
    ) {
        return switch (entry.calculationType()) {
            case DIRECT_AMOUNT -> {
                String conceptCode = entry.identity().getConceptCode();
                BigDecimal precomputed = context.getPrecomputedDirectAmounts().get(conceptCode);
                if (precomputed != null) {
                    yield precomputed;
                }
                yield technicalValueResolver.resolve(conceptCode, context);
            }

            case RATE_BY_QUANTITY ->
                    rateByQuantityResolver.resolve(entry, state);

            case PERCENTAGE ->
                    percentageConceptResolver.resolve(entry, state);

            case GREATEST ->
                    greatestConceptResolver.resolve(entry, state);

            case LEAST ->
                    leastConceptResolver.resolve(entry, state);

            case AGGREGATE -> {
                BigDecimal sum = BigDecimal.ZERO;
                for (AggregateSourceEntry source : entry.aggregateSources()) {
                    BigDecimal sourceAmount = state.getRequiredAmount(source.identity());
                    sum = sum.add(source.invertSign() ? sourceAmount.negate() : sourceAmount);
                }
                // Sin redondear: los sumandos ya vienen redondeados con sus decimales, y volver a
                // redondear la suma seria la segunda vez. El motor le aplica una sola vez los
                // decimales del propio agregado al salir.
                yield sum;
            }

            case ENGINE_PROVIDED -> {
                String conceptCode = entry.identity().getConceptCode();
                TechnicalConceptCalculator calculator = technicalCalculatorRegistry.get(conceptCode);
                if (calculator == null) {
                    throw new UnsupportedTechnicalConceptException(conceptCode);
                }
                yield calculator.resolve(new TechnicalConceptSegmentData(
                        context.getPeriodStart(),
                        context.getPeriodEnd(),
                        context.getSegmentStart(),
                        context.getSegmentEnd(),
                        context.getDaysInSegment(),
                        context.getWorkingTimePercentage(),
                        context.getRuleSystemCode(),
                        context.getGrupoCotizacionCode(),
                        context.getTipoNomina()
                ));
            }

            case EMPLOYEE_INPUT ->
                    context.getEmployeeInputs()
                            .getOrDefault(entry.identity().getConceptCode(), BigDecimal.ZERO);

            default ->
                    throw new UnsupportedCalculationTypeException(entry.calculationType());
        };
    }
}

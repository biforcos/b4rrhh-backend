package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.application.port.AgreementProfileContext;
import com.b4rrhh.payroll.application.port.AgreementProfileLookupPort;
import com.b4rrhh.payroll.application.port.EmployeeTaxInfoContext;
import com.b4rrhh.payroll.application.port.EmployeeTaxInfoPayrollLookupPort;
import com.b4rrhh.rulesystem.agreementcategoryprofile.application.usecase.GetAgreementCategoryProfileQuery;
import com.b4rrhh.rulesystem.agreementcategoryprofile.application.usecase.GetAgreementCategoryProfileUseCase;
import com.b4rrhh.payroll.application.port.CompanyProfileContext;
import com.b4rrhh.payroll.application.port.EmployeePayrollInputLookupPort;
import com.b4rrhh.payroll.application.port.WorkCenterProfileContext;
import com.b4rrhh.payroll.application.port.WorkCenterProfileLookupPort;
import com.b4rrhh.payroll.application.port.CompanyProfileLookupPort;
import com.b4rrhh.payroll.application.port.EmployeePersonalDataContext;
import com.b4rrhh.payroll.application.port.EmployeePersonalDataLookupPort;
import com.b4rrhh.payroll.application.port.PayrollLaunchEligibleInputContext;
import com.b4rrhh.payroll.application.port.PayrollLaunchEligibleInputLookupPort;
import com.b4rrhh.payroll.application.service.PayrollConceptExecutionContext;
import com.b4rrhh.payroll.application.service.PayrollConceptExecutionResult;
import com.b4rrhh.payroll.application.service.PayrollConceptGraphCalculator;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollConcept;
import com.b4rrhh.payroll.domain.model.PayrollContextSnapshot;
import com.b4rrhh.payroll.domain.model.PayrollSegment;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.model.PayrollWarning;
import com.b4rrhh.payroll.infrastructure.config.PayrollLaunchExecutionProperties;
import com.b4rrhh.payroll_engine.concept.domain.model.CalculationType;
import com.b4rrhh.payroll_engine.concept.domain.model.ExecutionScope;
import com.b4rrhh.payroll_engine.concept.domain.model.OperandRole;
import com.b4rrhh.payroll_engine.dependency.domain.model.ConceptNodeIdentity;
import com.b4rrhh.payroll_engine.eligibility.domain.model.EmployeeAssignmentContext;
import com.b4rrhh.payroll_engine.execution.application.service.SegmentExecutionEngine;
import com.b4rrhh.payroll_engine.execution.domain.model.ConceptExecutionPlanEntry;
import com.b4rrhh.payroll_engine.execution.domain.model.SegmentExecutionState;
import com.b4rrhh.payroll_engine.planning.application.service.BuildEligibleExecutionPlanUseCase;
import com.b4rrhh.payroll_engine.planning.domain.model.EligibleExecutionPlanResult;
import com.b4rrhh.payroll_engine.segment.domain.model.SegmentCalculationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CalculatePayrollUnitService implements CalculatePayrollUnitUseCase {

    private static final Logger log = LoggerFactory.getLogger(CalculatePayrollUnitService.class);

    private final CalculatePayrollUseCase calculatePayrollUseCase;
    private final PayrollLaunchEligibleInputLookupPort payrollLaunchEligibleInputLookupPort;
    private final PayrollLaunchExecutionProperties payrollLaunchExecutionProperties;
    private final PayrollConceptGraphCalculator payrollConceptGraphCalculator;
    private final BuildEligibleExecutionPlanUseCase buildEligibleExecutionPlanUseCase;
    private final CompanyProfileLookupPort companyProfileLookupPort;
    private final EmployeePersonalDataLookupPort employeePersonalDataLookupPort;
    private final AgreementProfileLookupPort agreementProfileLookupPort;
    private final WorkCenterProfileLookupPort workCenterProfileLookupPort;
    private final SegmentExecutionEngine segmentExecutionEngine;
    private final EmployeePayrollInputLookupPort employeePayrollInputLookupPort;
    private final GetAgreementCategoryProfileUseCase getAgreementCategoryProfileUseCase;
    private final EmployeeTaxInfoPayrollLookupPort employeeTaxInfoLookupPort;

    public CalculatePayrollUnitService(
            CalculatePayrollUseCase calculatePayrollUseCase,
            PayrollLaunchEligibleInputLookupPort payrollLaunchEligibleInputLookupPort,
            PayrollLaunchExecutionProperties payrollLaunchExecutionProperties,
            PayrollConceptGraphCalculator payrollConceptGraphCalculator,
            BuildEligibleExecutionPlanUseCase buildEligibleExecutionPlanUseCase,
            CompanyProfileLookupPort companyProfileLookupPort,
            EmployeePersonalDataLookupPort employeePersonalDataLookupPort,
            AgreementProfileLookupPort agreementProfileLookupPort,
            WorkCenterProfileLookupPort workCenterProfileLookupPort,
            SegmentExecutionEngine segmentExecutionEngine,
            EmployeePayrollInputLookupPort employeePayrollInputLookupPort,
            GetAgreementCategoryProfileUseCase getAgreementCategoryProfileUseCase,
            EmployeeTaxInfoPayrollLookupPort employeeTaxInfoLookupPort
    ) {
        this.calculatePayrollUseCase = calculatePayrollUseCase;
        this.payrollLaunchEligibleInputLookupPort = payrollLaunchEligibleInputLookupPort;
        this.payrollLaunchExecutionProperties = payrollLaunchExecutionProperties;
        this.payrollConceptGraphCalculator = payrollConceptGraphCalculator;
        this.buildEligibleExecutionPlanUseCase = buildEligibleExecutionPlanUseCase;
        this.companyProfileLookupPort = companyProfileLookupPort;
        this.employeePersonalDataLookupPort = employeePersonalDataLookupPort;
        this.agreementProfileLookupPort = agreementProfileLookupPort;
        this.workCenterProfileLookupPort = workCenterProfileLookupPort;
        this.segmentExecutionEngine = segmentExecutionEngine;
        this.employeePayrollInputLookupPort = employeePayrollInputLookupPort;
        this.getAgreementCategoryProfileUseCase = getAgreementCategoryProfileUseCase;
        this.employeeTaxInfoLookupPort = employeeTaxInfoLookupPort;
    }

    @Override
    public Payroll calculate(CalculatePayrollUnitCommand command) {
        return calculateEligibleReal(command);
    }

    private Payroll calculateEligibleReal(CalculatePayrollUnitCommand command) {
        log.info("[NÓMINA] ▶ Iniciando cálculo ELIGIBLE_REAL | empleado={} tipo={} periodo={} presencia={}",
                command.employeeNumber(), command.employeeTypeCode(),
                command.payrollPeriodCode(), command.presenceNumber());

        Optional<PayrollLaunchEligibleInputContext> inputOpt = payrollLaunchEligibleInputLookupPort.findByUnitAndPeriod(
                command.ruleSystemCode(),
                command.employeeTypeCode(),
                command.employeeNumber(),
                command.presenceNumber(),
                command.periodStart(),
                command.periodEnd()
        );

        if (inputOpt.isEmpty()) {
            throw new PayrollLaunchInputMissingException(
                    "ELIGIBLE_INPUT_CONTEXT_NOT_FOUND",
                    "Eligible real execution skipped: launcher input context is missing for payroll unit",
                    Map.of(
                            "executionMode", "ELIGIBLE_REAL",
                            "employeeTypeCode", command.employeeTypeCode(),
                            "employeeNumber", command.employeeNumber(),
                            "presenceNumber", command.presenceNumber()
                    )
            );
        }

        PayrollLaunchEligibleInputContext input = inputOpt.get();
        if (input.agreementCode() == null || input.agreementCode().isBlank()) {
            throw new PayrollLaunchInputMissingException(
                    "AGREEMENT_CODE_MISSING",
                    "Eligible real execution skipped: agreementCode is required but missing in launcher context",
                    Map.of("executionMode", "ELIGIBLE_REAL")
            );
        }
        if (input.agreementCategoryCode() == null || input.agreementCategoryCode().isBlank()) {
            throw new PayrollLaunchInputMissingException(
                    "AGREEMENT_CATEGORY_MISSING",
                    "Eligible real execution skipped: agreementCategoryCode is required but missing in launcher context",
                    Map.of("executionMode", "ELIGIBLE_REAL")
            );
        }

        log.info("[NÓMINA] Contexto resuelto | empresa={} convenio={} categoría={} ventanas={}",
                input.companyCode(), input.agreementCode(), input.agreementCategoryCode(),
                input.workingTimeWindows() != null ? input.workingTimeWindows().size() : 0);

        var categoryProfile = getAgreementCategoryProfileUseCase.get(
                new GetAgreementCategoryProfileQuery(command.ruleSystemCode(), input.agreementCategoryCode()));
        String grupoCotizacionCode = categoryProfile.getGrupoCotizacionCode();
        String tipoNomina = categoryProfile.getTipoNomina().name();

        EmployeeAssignmentContext assignmentContext = new EmployeeAssignmentContext(
                command.ruleSystemCode(),
                input.companyCode(),
                input.agreementCode(),
                command.employeeTypeCode()
        );

        log.debug("[ENGINE] Construyendo plan de ejecución | RS={} convenio={} ref={}",
                command.ruleSystemCode(), input.agreementCode(), command.periodEnd());
        EligibleExecutionPlanResult planResult =
                buildEligibleExecutionPlanUseCase.build(assignmentContext, command.periodEnd());

        List<ConceptExecutionPlanEntry> plan = planResult.executionPlan();
        log.info("[NÓMINA] Plan de ejecución: {} pasos → {}",
                plan.size(),
                plan.stream().map(e -> e.identity().getConceptCode()).collect(Collectors.joining(" → ")));

        Map<String, com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept> engineConceptByCode =
                planResult.expandedConcepts().stream()
                        .collect(Collectors.toMap(
                                c -> c.getConceptCode(),
                                c -> c
                        ));

        PayrollConceptExecutionContext calcContext = new PayrollConceptExecutionContext(
                command.ruleSystemCode(),
                input.agreementCode(),
                input.agreementCategoryCode(),
                command.periodEnd()
        );

        List<SegmentSpec> segments = buildSegments(input, command.periodStart(), command.periodEnd());
        log.info("[NÓMINA] Segmentos de jornada: {}", segments.size());

        int period = command.periodStart().getYear() * 100 + command.periodStart().getMonthValue();
        Map<String, BigDecimal> employeeInputsForPeriod = employeePayrollInputLookupPort.findInputsByPeriod(
                command.ruleSystemCode(),
                command.employeeTypeCode(),
                command.employeeNumber(),
                period
        );

        // Pre-compute DIRECT_AMOUNT concepts once: their value comes from the rule system,
        // not from the segment, so it is the same wherever the concept is evaluated.
        Map<String, BigDecimal> precomputedDirectAmounts = new HashMap<>();
        for (ConceptExecutionPlanEntry entry : plan) {
            if (entry.calculationType() == CalculationType.DIRECT_AMOUNT) {
                String conceptCode = entry.identity().getConceptCode();
                PayrollConceptExecutionResult directResult =
                        payrollConceptGraphCalculator.calculateConceptResult(conceptCode, calcContext);
                precomputedDirectAmounts.put(conceptCode, directResult.amount());
                log.debug("[NOMINA] Pre-calculado DIRECT_AMOUNT {} = {}", conceptCode, directResult.amount());
            }
        }
        long daysInPeriod = ChronoUnit.DAYS.between(command.periodStart(), command.periodEnd()) + 1;
        BigDecimal monthlySalary = Objects.requireNonNullElse(
                payrollLaunchExecutionProperties.getEligibleRealMonthlySalaryAmount(), BigDecimal.ZERO);

        // One context and one state per segment, plus one context and one state for the period.
        // Which of them a concept is evaluated against is decided by its execution scope (ADR-058):
        //   SEGMENT — once per segment, against that segment's state; the period value is the sum.
        //   PERIOD  — once, against the period state, with the SEGMENT feeds already composed.
        List<SegmentCalculationContext> segmentContexts = new ArrayList<>(segments.size());
        List<SegmentExecutionState> segmentStates = new ArrayList<>(segments.size());
        for (int segIdx = 0; segIdx < segments.size(); segIdx++) {
            SegmentSpec seg = segments.get(segIdx);
            log.info("[NOMINA] Segmento {} de {} ({} dias, jornada={}%)",
                    seg.segmentStart(), seg.segmentEnd(), seg.daysInSegment(), seg.workingTimePercentage());
            segmentContexts.add(new SegmentCalculationContext(
                    command.ruleSystemCode(),
                    command.employeeTypeCode(),
                    command.employeeNumber(),
                    command.periodStart(),
                    command.periodEnd(),
                    seg.segmentStart(),
                    seg.segmentEnd(),
                    segIdx == 0,
                    segIdx == segments.size() - 1,
                    daysInPeriod,
                    seg.daysInSegment(),
                    seg.workingTimePercentage(),
                    monthlySalary,
                    employeeInputsForPeriod,
                    grupoCotizacionCode,
                    tipoNomina,
                    precomputedDirectAmounts
            ));
            segmentStates.add(new SegmentExecutionState());
        }
        SegmentCalculationContext periodContext = periodContext(
                command, segments, daysInPeriod, monthlySalary, employeeInputsForPeriod,
                grupoCotizacionCode, tipoNomina, precomputedDirectAmounts);
        SegmentExecutionState periodState = new SegmentExecutionState();

        List<ConceptRow> payslipRows = new ArrayList<>();
        int step = 0;
        for (ConceptExecutionPlanEntry entry : plan) {
            step++;
            String conceptCode = entry.identity().getConceptCode();
            var engineConcept = engineConceptByCode.get(conceptCode);

            if (engineConcept.getExecutionScope() == ExecutionScope.SEGMENT) {
                // Composition is always a sum: a SEGMENT magnitude only gets composed to reach the
                // payslip or to feed a PERIOD aggregate, and both are sums. A SEGMENT rate is never
                // read at period level, because no operand crosses from SEGMENT to PERIOD (ADR-058).
                BigDecimal composed = BigDecimal.ZERO;
                for (int segIdx = 0; segIdx < segments.size(); segIdx++) {
                    SegmentSpec seg = segments.get(segIdx);
                    SegmentExecutionState state = segmentStates.get(segIdx);
                    BigDecimal amount = segmentExecutionEngine.evaluate(entry, state, segmentContexts.get(segIdx));
                    state.storeResult(entry.identity(), amount);
                    composed = composed.add(amount);
                    log.info("[NÓMINA] [{}/{}] {} {} SEGMENT {}..{} → {} (q={} r={})",
                            step, plan.size(), conceptCode, entry.calculationType(),
                            seg.segmentStart(), seg.segmentEnd(), amount,
                            quantityOf(entry, state), rateOf(entry, state));
                    addPayslipRow(payslipRows, engineConcept, entry, state, amount);
                }
                periodState.storeResult(entry.identity(), composed);
                if (segments.size() > 1) {
                    log.info("[NÓMINA] [{}/{}] {} compuesto = {} (suma de {} tramos)",
                            step, plan.size(), conceptCode, composed, segments.size());
                }
            } else {
                BigDecimal amount = segmentExecutionEngine.evaluate(entry, periodState, periodContext);
                periodState.storeResult(entry.identity(), amount);
                // A period value is a single number, the same in every segment, so a later SEGMENT
                // concept may read it as an operand.
                for (SegmentExecutionState state : segmentStates) {
                    state.storeResult(entry.identity(), amount);
                }
                log.info("[NÓMINA] [{}/{}] {} {} PERIOD → {} (q={} r={})",
                        step, plan.size(), conceptCode, entry.calculationType(), amount,
                        quantityOf(entry, periodState), rateOf(entry, periodState));
                addPayslipRow(payslipRows, engineConcept, entry, periodState, amount);
            }
        }

        if (payrollLaunchExecutionProperties.isCollapseSegmentRows()) {
            int before = payslipRows.size();
            payslipRows = collapsePayslipRows(payslipRows);
            log.info("[NÓMINA] Colapso de segmentos: {} → {} lineas", before, payslipRows.size());
        } else {
            log.info("[NÓMINA] Colapso desactivado (collapse-segment-rows=false): {} lineas sin colapsar", payslipRows.size());
        }

        payslipRows.sort(Comparator.comparingInt(ConceptRow::displayOrder));

        log.info("[NÓMINA] Filtro recibo | {} lineas en recibo → [{}]",
                payslipRows.size(),
                payslipRows.stream().map(ConceptRow::conceptCode).collect(Collectors.joining(", ")));

        List<PayrollConcept> payrollConcepts = new ArrayList<>();
        for (int i = 0; i < payslipRows.size(); i++) {
            ConceptRow r = payslipRows.get(i);
            payrollConcepts.add(new PayrollConcept(
                    i + 1,
                    r.conceptCode(),
                    r.mnemonic(),
                    r.amount(),
                    r.quantity(),
                    r.rate(),
                    r.nature(),
                    command.payrollPeriodCode(),
                    r.displayOrder()
            ));
        }

        LocalDate presenceStart = input.presenceStartDate();
        LocalDate presenceEnd = input.presenceEndDate();
        List<PayrollSegment> payrollSegments = segments.stream()
                .filter(s -> presenceStart == null || !s.segmentEnd().isBefore(presenceStart))
                .filter(s -> presenceEnd == null || !s.segmentStart().isAfter(presenceEnd))
                .map(s -> {
                    LocalDate start = (presenceStart != null && presenceStart.isAfter(s.segmentStart()))
                            ? presenceStart : s.segmentStart();
                    return new PayrollSegment(start);
                })
                .toList();

        Payroll result = calculatePayrollUseCase.calculate(new CalculatePayrollCommand(
                command.ruleSystemCode(),
                command.employeeTypeCode(),
                command.employeeNumber(),
                command.payrollPeriodCode(),
                command.payrollTypeCode(),
                command.presenceNumber(),
                PayrollStatus.CALCULATED,
                null,
                LocalDateTime.now(),
                command.calculationEngineCode(),
                command.calculationEngineVersion(),
                List.of(eligibleRealWarning(command, input)),
                payrollConcepts,
                buildSnapshots(command, input),
                payrollSegments
        ));
        log.info("[NÓMINA] ✓ Cálculo completado | empleado={} periodo={} → {} líneas en recibo",
                command.employeeNumber(), command.payrollPeriodCode(), payrollConcepts.size());
        return result;
    }

    private record ConceptRow(
            String conceptCode,
            String mnemonic,
            BigDecimal amount,
            BigDecimal quantity,
            BigDecimal rate,
            String nature,
            int displayOrder
    ) {}

    private record SegmentSpec(
            LocalDate segmentStart,
            LocalDate segmentEnd,
            long daysInSegment,
            BigDecimal workingTimePercentage
    ) {}

    private List<SegmentSpec> buildSegments(
            PayrollLaunchEligibleInputContext input,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        LocalDate presenceStart = input.presenceStartDate() != null && input.presenceStartDate().isAfter(periodStart)
                ? input.presenceStartDate() : periodStart;
        LocalDate presenceEnd = input.presenceEndDate() != null && input.presenceEndDate().isBefore(periodEnd)
                ? input.presenceEndDate() : periodEnd;

        if (input.workingTimeWindows() == null || input.workingTimeWindows().isEmpty()) {
            long days = ChronoUnit.DAYS.between(presenceStart, presenceEnd) + 1;
            return List.of(new SegmentSpec(presenceStart, presenceEnd, days, BigDecimal.valueOf(100)));
        }

        List<SegmentSpec> segments = new ArrayList<>();
        for (var window : input.workingTimeWindows()) {
            LocalDate windowStart = window.startDate() != null && window.startDate().isAfter(presenceStart)
                    ? window.startDate() : presenceStart;
            LocalDate windowEnd = window.endDate() != null && window.endDate().isBefore(presenceEnd)
                    ? window.endDate() : presenceEnd;
            if (!windowStart.isAfter(windowEnd)) {
                long days = ChronoUnit.DAYS.between(windowStart, windowEnd) + 1;
                segments.add(new SegmentSpec(windowStart, windowEnd, days, window.workingTimePercentage()));
            }
        }
        return segments.isEmpty()
                ? List.of(new SegmentSpec(presenceStart, presenceEnd,
                        ChronoUnit.DAYS.between(presenceStart, presenceEnd) + 1,
                        BigDecimal.valueOf(100)))
                : segments;
    }

    /**
     * The period seen as a single stretch: from the first covered day to the last, with every
     * covered day counted and the working time weighted by days. A PERIOD concept has its rule
     * defined over the whole period and is not split into sub-periods (ADR-058); when such a rule
     * reads the working time and the month has two, this is the only period value consistent
     * with the sum of the segments, and it equals the segment value whenever they all agree.
     */
    private SegmentCalculationContext periodContext(
            CalculatePayrollUnitCommand command,
            List<SegmentSpec> segments,
            long daysInPeriod,
            BigDecimal monthlySalary,
            Map<String, BigDecimal> employeeInputsForPeriod,
            String grupoCotizacionCode,
            String tipoNomina,
            Map<String, BigDecimal> precomputedDirectAmounts
    ) {
        long daysCovered = segments.stream().mapToLong(SegmentSpec::daysInSegment).sum();
        BigDecimal weightedWorkingTime = BigDecimal.ZERO;
        for (SegmentSpec seg : segments) {
            weightedWorkingTime = weightedWorkingTime.add(
                    seg.workingTimePercentage().multiply(BigDecimal.valueOf(seg.daysInSegment())));
        }
        weightedWorkingTime = weightedWorkingTime.divide(BigDecimal.valueOf(daysCovered), 8, RoundingMode.HALF_UP);
        return new SegmentCalculationContext(
                command.ruleSystemCode(),
                command.employeeTypeCode(),
                command.employeeNumber(),
                command.periodStart(),
                command.periodEnd(),
                segments.getFirst().segmentStart(),
                segments.getLast().segmentEnd(),
                true,
                true,
                daysInPeriod,
                daysCovered,
                weightedWorkingTime,
                monthlySalary,
                employeeInputsForPeriod,
                grupoCotizacionCode,
                tipoNomina,
                precomputedDirectAmounts
        );
    }

    private void addPayslipRow(
            List<ConceptRow> payslipRows,
            com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept engineConcept,
            ConceptExecutionPlanEntry entry,
            SegmentExecutionState state,
            BigDecimal amount
    ) {
        if (engineConcept.getPayslipOrderCode() == null) {
            return;
        }
        int displayOrder = Integer.parseInt(engineConcept.getPayslipOrderCode());
        payslipRows.add(new ConceptRow(
                engineConcept.getConceptCode(),
                engineConcept.getConceptMnemonic(),
                amount,
                quantityOf(entry, state),
                rateOf(entry, state),
                engineConcept.getFunctionalNature().name(),
                displayOrder));
    }

    /** The payslip "quantity": the QUANTITY of a RATE_BY_QUANTITY, the BASE of a PERCENTAGE. */
    private BigDecimal quantityOf(ConceptExecutionPlanEntry entry, SegmentExecutionState state) {
        return switch (entry.calculationType()) {
            case RATE_BY_QUANTITY -> operandAmount(entry, state, OperandRole.QUANTITY);
            case PERCENTAGE -> operandAmount(entry, state, OperandRole.BASE);
            default -> null;
        };
    }

    /** The payslip "rate": the RATE of a RATE_BY_QUANTITY, the PERCENTAGE of a PERCENTAGE. */
    private BigDecimal rateOf(ConceptExecutionPlanEntry entry, SegmentExecutionState state) {
        return switch (entry.calculationType()) {
            case RATE_BY_QUANTITY -> operandAmount(entry, state, OperandRole.RATE);
            case PERCENTAGE -> operandAmount(entry, state, OperandRole.PERCENTAGE);
            default -> null;
        };
    }

    private BigDecimal operandAmount(ConceptExecutionPlanEntry entry, SegmentExecutionState state, OperandRole role) {
        ConceptNodeIdentity source = entry.operands().get(role);
        return source == null ? null : state.getOptionalAmount(source).orElse(null);
    }

    private List<ConceptRow> collapsePayslipRows(List<ConceptRow> rows) {
        LinkedHashMap<String, ConceptRow> collapsed = new LinkedHashMap<>();
        for (ConceptRow row : rows) {
            String key = row.conceptCode() + "|" + (row.rate() != null
                    ? row.rate().stripTrailingZeros().toPlainString() : "null");
            collapsed.merge(key, row, (existing, incoming) -> new ConceptRow(
                    existing.conceptCode(),
                    existing.mnemonic(),
                    existing.amount().add(incoming.amount()),
                    existing.quantity() != null && incoming.quantity() != null
                            ? existing.quantity().add(incoming.quantity())
                            : existing.quantity(),
                    existing.rate(),
                    existing.nature(),
                    existing.displayOrder()
            ));
        }
        return new ArrayList<>(collapsed.values());
    }

    private PayrollWarning eligibleRealWarning(
            CalculatePayrollUnitCommand command,
            PayrollLaunchEligibleInputContext input
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("executionMode", "ELIGIBLE_REAL");
        details.put("employeeTypeCode", command.employeeTypeCode());
        details.put("employeeNumber", command.employeeNumber());
        details.put("agreementCode", input.agreementCode());
        details.put("agreementCategoryCode", input.agreementCategoryCode());
        return new PayrollWarning(
                null,
                null,
                "ELIGIBLE_REAL_EXECUTION",
                "INFO",
                "Payroll generated by eligible real minimal concept execution",
                toJson(details)
        );
    }

    private List<PayrollContextSnapshot> buildSnapshots(
            CalculatePayrollUnitCommand command,
            PayrollLaunchEligibleInputContext input
    ) {
        List<PayrollContextSnapshot> snapshots = new ArrayList<>();
        snapshots.add(eligibleRealSnapshot(command, input));

        if (input.companyCode() != null) {
            companyProfileLookupPort
                    .findByRuleSystemAndCode(command.ruleSystemCode(), input.companyCode())
                    .map(cp -> buildCompanySnapshot(command, input.companyCode(), cp))
                    .ifPresent(snapshots::add);
        }

        employeePersonalDataLookupPort
                .findByBusinessKey(command.ruleSystemCode(), command.employeeTypeCode(),
                        command.employeeNumber(), command.periodEnd())
                .map(ep -> buildEmployeeSnapshot(command, ep))
                .ifPresent(snapshots::add);

        if (input.agreementCode() != null) {
            agreementProfileLookupPort
                    .findByRuleSystemAndCode(command.ruleSystemCode(), input.agreementCode())
                    .map(ap -> buildAgreementSnapshot(command, input.agreementCode(),
                            input.agreementCategoryCode(), ap))
                    .ifPresent(snapshots::add);
        }

        if (input.workCenterCode() != null) {
            workCenterProfileLookupPort
                    .findByRuleSystemAndCode(command.ruleSystemCode(), input.workCenterCode())
                    .map(wc -> buildWorkCenterSnapshot(command, wc))
                    .ifPresent(snapshots::add);
        }

        // Tax info is always captured; ofDefault() applies when no record exists (single, no dependants, territory COMUN)
        snapshots.add(buildTaxInfoSnapshot(command, input));

        return List.copyOf(snapshots);
    }

    private PayrollContextSnapshot buildTaxInfoSnapshot(
            CalculatePayrollUnitCommand command,
            PayrollLaunchEligibleInputContext input) {
        LocalDate referenceDate = input.presenceStartDate() != null
            ? input.presenceStartDate()
            : command.periodStart();

        EmployeeTaxInfoContext ctx = employeeTaxInfoLookupPort.findLatestOnOrBefore(
            command.ruleSystemCode(), command.employeeTypeCode(), command.employeeNumber(), referenceDate);

        Map<String, Object> sourceKey = new LinkedHashMap<>();
        sourceKey.put("ruleSystemCode", command.ruleSystemCode());
        sourceKey.put("employeeTypeCode", command.employeeTypeCode());
        sourceKey.put("employeeNumber", command.employeeNumber());
        sourceKey.put("referenceDate", referenceDate.toString());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("familySituation", ctx.familySituation());
        payload.put("descendantsCount", ctx.descendantsCount());
        payload.put("ascendantsCount", ctx.ascendantsCount());
        payload.put("disabilityDegree", ctx.disabilityDegree());
        payload.put("pensionCompensatoria", ctx.pensionCompensatoria());
        payload.put("geographicMobility", ctx.geographicMobility());
        payload.put("habitualResidenceLoan", ctx.habitualResidenceLoan());
        payload.put("taxTerritory", ctx.taxTerritory());

        return new PayrollContextSnapshot("EMPLOYEE_TAX_INFORMATION", "EMPLOYEE",
            toJson(sourceKey), toJson(payload));
    }

    private PayrollContextSnapshot buildCompanySnapshot(
            CalculatePayrollUnitCommand command,
            String companyCode,
            CompanyProfileContext cp
    ) {
        Map<String, Object> sourceKey = new LinkedHashMap<>();
        sourceKey.put("ruleSystemCode", command.ruleSystemCode());
        sourceKey.put("entityTypeCode", "COMPANY");
        sourceKey.put("entityCode", companyCode);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("legalName", cp.legalName());
        payload.put("taxIdentifier", cp.taxIdentifier());
        payload.put("street", cp.street());
        payload.put("city", cp.city());
        payload.put("postalCode", cp.postalCode());

        return new PayrollContextSnapshot("COMPANY_DATA", "RULESYSTEM", toJson(sourceKey), toJson(payload));
    }

    private PayrollContextSnapshot buildEmployeeSnapshot(
            CalculatePayrollUnitCommand command,
            EmployeePersonalDataContext ep
    ) {
        Map<String, Object> sourceKey = new LinkedHashMap<>();
        sourceKey.put("ruleSystemCode", command.ruleSystemCode());
        sourceKey.put("employeeTypeCode", command.employeeTypeCode());
        sourceKey.put("employeeNumber", command.employeeNumber());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fullName", ep.fullName());
        payload.put("nif", ep.nif());
        payload.put("street", ep.street());
        payload.put("city", ep.city());
        payload.put("postalCode", ep.postalCode());

        return new PayrollContextSnapshot("EMPLOYEE_DATA", "EMPLOYEE", toJson(sourceKey), toJson(payload));
    }

    private PayrollContextSnapshot buildAgreementSnapshot(
            CalculatePayrollUnitCommand command,
            String agreementCode,
            String agreementCategoryCode,
            AgreementProfileContext ap
    ) {
        Map<String, Object> sourceKey = new LinkedHashMap<>();
        sourceKey.put("ruleSystemCode", command.ruleSystemCode());
        sourceKey.put("entityTypeCode", "AGREEMENT");
        sourceKey.put("entityCode", agreementCode);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("officialAgreementNumber", ap.officialAgreementNumber());
        payload.put("displayName", ap.displayName());
        payload.put("shortName", ap.shortName());
        payload.put("annualHours", ap.annualHours() != null ? ap.annualHours().toPlainString() : null);
        payload.put("agreementCategoryCode", agreementCategoryCode);

        return new PayrollContextSnapshot("AGREEMENT_DATA", "RULESYSTEM", toJson(sourceKey), toJson(payload));
    }

    private PayrollContextSnapshot buildWorkCenterSnapshot(
            CalculatePayrollUnitCommand command,
            WorkCenterProfileContext wc
    ) {
        Map<String, Object> sourceKey = new LinkedHashMap<>();
        sourceKey.put("ruleSystemCode", command.ruleSystemCode());
        sourceKey.put("workCenterCode", wc.workCenterCode());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("workCenterCode", wc.workCenterCode());
        payload.put("workCenterName", wc.workCenterName());

        return new PayrollContextSnapshot("WORK_CENTER_DATA", "RULESYSTEM", toJson(sourceKey), toJson(payload));
    }

    private PayrollContextSnapshot eligibleRealSnapshot(
            CalculatePayrollUnitCommand command,
            PayrollLaunchEligibleInputContext input
    ) {
        Map<String, Object> sourceKey = new LinkedHashMap<>();
        sourceKey.put("ruleSystemCode", command.ruleSystemCode());
        sourceKey.put("employeeTypeCode", command.employeeTypeCode());
        sourceKey.put("employeeNumber", command.employeeNumber());
        sourceKey.put("payrollPeriodCode", command.payrollPeriodCode());
        sourceKey.put("payrollTypeCode", command.payrollTypeCode());
        sourceKey.put("presenceNumber", command.presenceNumber());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("executionMode", "ELIGIBLE_REAL");
        payload.put("agreementCode", input.agreementCode());
        payload.put("agreementCategoryCode", input.agreementCategoryCode());
        payload.put("presenceStartDate", input.presenceStartDate() != null ? input.presenceStartDate().toString() : null);
        payload.put("presenceEndDate", input.presenceEndDate() != null ? input.presenceEndDate().toString() : null);

        return new PayrollContextSnapshot(
                "EMPLOYEE_PAYROLL_CONTEXT",
                "PAYROLL_LAUNCH",
                toJson(sourceKey),
                toJson(payload)
        );
    }

    private String toJson(Map<String, Object> values) {
        StringBuilder out = new StringBuilder();
        out.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (!first) {
                out.append(",");
            }
            first = false;
            out.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value == null) {
                out.append("null");
            } else if (value instanceof Number || value instanceof Boolean) {
                out.append(value);
            } else {
                out.append("\"").append(String.valueOf(value).replace("\"", "\\\"")).append("\"");
            }
        }
        out.append("}");
        return out.toString();
    }
}

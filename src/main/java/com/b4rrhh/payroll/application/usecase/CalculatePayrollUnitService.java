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
import com.b4rrhh.payroll.application.port.PayrollCalculationStep;
import com.b4rrhh.payroll.application.port.PayrollCalculationStepWritePort;
import com.b4rrhh.payroll.application.port.PayrollLaunchAbsenceWindowContext;
import com.b4rrhh.payroll.application.port.PayrollLaunchAgreementWindowContext;
import com.b4rrhh.payroll.application.port.PayrollLaunchContractWindowContext;
import com.b4rrhh.payroll.application.port.PayrollLaunchEligibleInputContext;
import com.b4rrhh.payroll.application.port.PayrollLaunchEligibleInputLookupPort;
import com.b4rrhh.payroll.application.port.PayrollLaunchExtraPaymentRegimeWindowContext;
import com.b4rrhh.payroll.application.port.PayrollLaunchWorkingTimeWindowContext;
import com.b4rrhh.payroll.application.port.PreviousPeriodContributionBase;
import com.b4rrhh.payroll.application.port.PreviousPeriodContributionBaseLookupPort;
import com.b4rrhh.payroll.application.port.TableRowOrigin;
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
import com.b4rrhh.payroll_engine.concept.domain.model.ConceptLabelLanguage;
import com.b4rrhh.payroll_engine.concept.domain.model.ExecutionScope;
import com.b4rrhh.payroll_engine.concept.domain.model.OperandRole;
import com.b4rrhh.payroll_engine.concept.domain.port.ConceptLabelRepository;
import com.b4rrhh.payroll_engine.concept.domain.port.PayslipSectionRepository;
import com.b4rrhh.payroll_engine.dependency.domain.model.ConceptNodeIdentity;
import com.b4rrhh.payroll_engine.eligibility.domain.model.EmployeeAssignmentContext;
import com.b4rrhh.payroll_engine.execution.application.service.SegmentExecutionEngine;
import com.b4rrhh.payroll_engine.execution.domain.model.ConceptExecutionPlanEntry;
import com.b4rrhh.payroll_engine.execution.domain.model.SegmentExecutionState;
import com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodel;
import com.b4rrhh.payroll_engine.planning.application.service.BuildEligibleExecutionPlanUseCase;
import com.b4rrhh.payroll_engine.planning.domain.model.EligibleExecutionPlanResult;
import com.b4rrhh.payroll_engine.segment.domain.model.SegmentAbsence;
import com.b4rrhh.payroll_engine.segment.domain.model.SegmentCalculationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CalculatePayrollUnitService implements CalculatePayrollUnitUseCase {

    private static final Logger log = LoggerFactory.getLogger(CalculatePayrollUnitService.class);

    /**
     * Las ausencias que llegan al calculo, y son las que <b>no se pagan</b> (ADR-073,
     * {@code backend#127}).
     *
     * <p>Estas dos parten el periodo y en su tramo no se devengan dias. Las demas —vacaciones,
     * permiso retribuido, fuerza mayor— se cobran enteras y <b>no parten</b>: un tramo para ellas
     * seria un paso de calculo mas que no cambia ningun numero.
     *
     * <p>{@code IT_WORK_ACCIDENT} y {@code PARENTAL_LEAVE} tampoco estan, y eso es el alcance de la
     * v1 y no un olvido: el accidente de trabajo tiene otra base reguladora —la de profesionales, con
     * las horas extra del ano anterior— y el 75 % desde el dia siguiente. Hasta que se implemente,
     * siguen cobrando entero, y el ADR-073 lo dice con palabras.
     *
     * <p>Es una constante de Java y no una columna del catalogo a proposito: hoy es una decision de
     * alcance del producto, la misma para todos los sistemas de reglas. El dia que un sistema de
     * reglas necesite otra lista, esto se convierte en una propiedad del tipo de ausencia; mientras
     * no haga falta, una tabla de un solo valor esconderia la decision en vez de declararla.
     */
    private static final Set<String> AUSENCIAS_QUE_NO_SE_PAGAN =
            Set.of(SegmentAbsence.IT_COMMON, SegmentAbsence.UNPAID_LEAVE);

    /** Como se escribe un periodo de nomina: {@code 202609}. */
    private static final DateTimeFormatter PERIODO = DateTimeFormatter.ofPattern("yyyyMM");

    /**
     * Los dias entre los que se reparte la base del mes anterior para dar la base reguladora diaria
     * ({@code backend#128}).
     *
     * <p>Treinta y no los dias naturales del mes: para un empleado de nomina mensual la base del mes
     * es siempre de un mes de treinta dias, dijera lo que dijera el calendario, que es la misma
     * convencion que el {@code D01}. Para nomina diaria se reparte entre los dias reales de aquel
     * mes, que son los que se cotizaron.
     */
    private static final BigDecimal DIAS_DEL_MES_NOMINA = BigDecimal.valueOf(30);

    /** Nomina diaria, que es la que no reparte entre treinta. */
    private static final String TIPO_NOMINA_DIARIA = "DIARIO";

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
    private final PayrollCalculationStepWritePort payrollCalculationStepWritePort;
    private final ConceptLabelRepository conceptLabelRepository;
    private final PayslipSectionRepository payslipSectionRepository;
    private final PreviousPeriodContributionBaseLookupPort previousPeriodContributionBaseLookupPort;

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
            EmployeeTaxInfoPayrollLookupPort employeeTaxInfoLookupPort,
            PayrollCalculationStepWritePort payrollCalculationStepWritePort,
            ConceptLabelRepository conceptLabelRepository,
            PayslipSectionRepository payslipSectionRepository,
            PreviousPeriodContributionBaseLookupPort previousPeriodContributionBaseLookupPort
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
        this.payrollCalculationStepWritePort = payrollCalculationStepWritePort;
        this.conceptLabelRepository = conceptLabelRepository;
        this.payslipSectionRepository = payslipSectionRepository;
        this.previousPeriodContributionBaseLookupPort = previousPeriodContributionBaseLookupPort;
    }

    /**
     * <p>Con {@code @Transactional} desde el {@code backend#93}: el recibo y sus pasos se guardan
     * en dos escrituras y tienen que ir o no ir juntas. El lanzador no abre transaccion a
     * proposito —sus contadores se guardan unidad a unidad— asi que sin esto la segunda escritura
     * caeria en su propia transaccion y un fallo entre medias dejaria un recibo sin pasos.
     */
    @Override
    @Transactional
    public Payroll calculate(CalculatePayrollUnitCommand command) {
        return calculateEligibleReal(command);
    }

    private Payroll calculateEligibleReal(CalculatePayrollUnitCommand command) {
        if (command.metamodel() == null) {
            throw new IllegalArgumentException(
                    "La unidad se calcula contra la reglamentación de su ejecución: el metamodelo "
                            + "no puede faltar. Quien lanza la ejecución lo carga una vez (backend#87).");
        }
        command.metamodel().requireSameRuleSystem(command.ruleSystemCode());

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

        // La actividad economica de la empresa, una vez por unidad y no una por tramo
        // (backend#122). De ella sale el tipo de la cuota de accidentes de trabajo, que es el
        // unico de este catalogo que no es el mismo para todo el mundo. Nula si la empresa no la
        // declara: quien la necesita es quien tiene que decir que falta.
        String cnaeCode = input.companyCode() == null ? null
                : companyProfileLookupPort
                        .findByRuleSystemAndCode(command.ruleSystemCode(), input.companyCode())
                        .map(CompanyProfileContext::cnaeCode)
                        .orElse(null);

        // La base reguladora del mes anterior, UNA VEZ por unidad y antes de calcular nada
        // (backend#128, ADR-074). Es la primera vez que un calculo mira fuera de su periodo, y mirar
        // puede terminar en que este recibo NO SE CALCULE: eso tiene que decidirse antes de que exista
        // un solo paso, no a mitad del grafo.
        BaseReguladoraDelMesAnterior baseReguladora = resolverBaseReguladora(command, input, tipoNomina);
        if (baseReguladora.noSeCalcula()) {
            return recibioNoCalculado(command, input, baseReguladora);
        }

        EmployeeAssignmentContext assignmentContext = new EmployeeAssignmentContext(
                command.ruleSystemCode(),
                input.companyCode(),
                input.agreementCode(),
                command.employeeTypeCode()
        );

        log.debug("[ENGINE] Construyendo plan de ejecución | RS={} convenio={} ref={}",
                command.ruleSystemCode(), input.agreementCode(), command.periodEnd());
        EligibleExecutionPlanResult planResult =
                buildEligibleExecutionPlanUseCase.build(assignmentContext, command.metamodel());

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

        // Los nombres del catalogo, una vez y enteros (backend#109). Lo que se lee aqui es lo que
        // se congela en las lineas de ESTE recibo: leerlo al pintar convertiria el documento en
        // una vista del catalogo, y un recibo entregado no puede cambiar porque alguien renombre
        // un concepto. Un concepto sin nombre no esta en el mapa y la linea se queda con su
        // mnemonico, que es una ausencia que se ve.
        Map<String, String> conceptLabels = conceptLabelRepository.findLabelsByRuleSystemCode(
                command.ruleSystemCode(), ConceptLabelLanguage.DEFAULT);

        // Y en que bloque del modelo oficial va cada naturaleza, tambien declarado y tambien una
        // vez (backend#109). Lo que coloca una linea no es el rango de su codigo —que 101 sea
        // devengo y 700 deduccion es una costumbre de numeracion, no una regla— sino su
        // naturaleza, que es lo unico que dice lo que el concepto ES.
        Map<String, String> sectionByNature =
                payslipSectionRepository.findSectionCodeByNature(command.ruleSystemCode());
        Map<String, String> subsectionByConcept =
                payslipSectionRepository.findSubsectionCodeByConcept(command.ruleSystemCode());

        // Los segmentos ya no son «de jornada»: salen de la union de los puntos de cambio de las
        // verticales que afectan al calculo —jornada, clasificacion laboral y contrato— (backend#47).
        List<SegmentSpec> segments = buildSegments(input, command.periodStart(), command.periodEnd());
        log.info("[NÓMINA] Segmentos del periodo: {} → {}", segments.size(),
                segments.stream().map(SegmentSpec::describe).collect(Collectors.joining(" | ")));

        int period = command.periodStart().getYear() * 100 + command.periodStart().getMonthValue();
        Map<String, BigDecimal> employeeInputsForPeriod = employeePayrollInputLookupPort.findInputsByPeriod(
                command.ruleSystemCode(),
                command.employeeTypeCode(),
                command.employeeNumber(),
                period
        );

        // Los DIRECT_AMOUNT se precalculan UNA VEZ POR CONTEXTO DE CONVENIO, no una para todo el
        // periodo (backend#47). Su valor no depende del tramo, pero si de la categoria, que es la
        // clave con la que se busca la fila de tabla: dos segmentos con categorias distintas tienen
        // precios distintos, y resolverlos una sola vez ponia el precio del ultimo tramo en los dias
        // del primero. Dos segmentos con la misma categoria comparten el precalculo, porque la
        // busqueda es la misma y la respuesta tambien.
        Map<String, PrecalculoDirecto> precalculoPorContexto = new HashMap<>();
        for (SegmentSpec seg : segments) {
            precalculoPorContexto.computeIfAbsent(
                    claveDePrecalculo(seg),
                    clave -> precalculoDirecto(plan, contextoDe(command, seg), command.metamodel()));
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
            log.info("[NOMINA] Segmento {} de {} ({} dias, {})",
                    seg.segmentStart(), seg.segmentEnd(), seg.daysInSegment(), seg.describe());
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
                    precalculoPorContexto.get(claveDePrecalculo(seg)).importes(),
                    seg.vigencias().extraPaymentsProrated(),
                    cnaeCode,
                    seg.vigencias().contractCode(),
                    seg.vigencias().absence(),
                    baseReguladora.diaria()
            ));
            segmentStates.add(new SegmentExecutionState());
        }
        // Lo que es del PERIODO se resuelve con el contexto del ULTIMO segmento, que es el ultimo
        // dia que el empleado estuvo presente. Es lo que se hacia antes para todo, y aqui sigue
        // siendo lo correcto: un concepto de ambito PERIOD tiene su regla definida sobre el periodo
        // entero (ADR-058), asi que no hay un tramo suyo al que preguntarle.
        PrecalculoDirecto precalculoDelPeriodo =
                precalculoPorContexto.get(claveDePrecalculo(segments.getLast()));
        SegmentCalculationContext periodContext = periodContext(
                command, segments, daysInPeriod, monthlySalary, employeeInputsForPeriod,
                grupoCotizacionCode, tipoNomina, precalculoDelPeriodo.importes(), cnaeCode);
        SegmentExecutionState periodState = new SegmentExecutionState();

        // Una travesia y una proyeccion (backend#93). El recorrido arma un paso por evaluacion
        // —un concepto de ambito SEGMENT se evalua una vez por segmento, asi que un mes partido
        // deja mas pasos que conceptos— y las lineas del recibo salen despues, filtrando por
        // payslip_order_code. No hay dos construcciones en paralelo que puedan divergir: hay una,
        // y el recibo es una vista suya con la regla de presentacion encima.
        List<PayrollCalculationStep> calculationSteps = new ArrayList<>();
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
                    calculationSteps.add(calculationStep(
                            calculationSteps.size() + 1, engineConcept, entry, state, amount,
                            seg.segmentStart(), seg.segmentEnd(),
                            precalculoPorContexto.get(claveDePrecalculo(seg)).filas()));
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
                calculationSteps.add(calculationStep(
                        calculationSteps.size() + 1, engineConcept, entry, periodState, amount,
                        null, null, precalculoDelPeriodo.filas()));
            }
        }

        log.info("[NÓMINA] Pasos de cálculo: {} ({} conceptos, {} segmento(s))",
                calculationSteps.size(), plan.size(), segments.size());

        List<ConceptRow> payslipRows = calculationSteps.stream()
                .filter(PayrollCalculationStep::isPayslipLine)
                .map(calculado -> toPayslipRow(calculado, conceptLabels))
                .collect(Collectors.toCollection(ArrayList::new));

        if (payrollLaunchExecutionProperties.isCollapseSegmentRows()) {
            int before = payslipRows.size();
            payslipRows = collapsePayslipRows(payslipRows);
            log.info("[NÓMINA] Colapso de segmentos: {} → {} lineas", before, payslipRows.size());
        } else {
            log.info("[NÓMINA] Colapso desactivado (collapse-segment-rows=false): {} lineas sin colapsar", payslipRows.size());
        }

        int antesDelCero = payslipRows.size();
        payslipRows.removeIf(CalculatePayrollUnitService::noSeImprimePorValerCero);
        if (payslipRows.size() != antesDelCero) {
            log.info("[NÓMINA] Regla del cero: {} → {} lineas", antesDelCero, payslipRows.size());
        }

        payslipRows.sort(Comparator.comparingInt(ConceptRow::displayOrder));

        log.info("[NÓMINA] Filtro recibo | {} lineas en recibo → [{}]",
                payslipRows.size(),
                payslipRows.stream().map(ConceptRow::conceptCode).collect(Collectors.joining(", ")));

        // El numero de linea se decide aqui, y aqui es donde hay que devolverselo a los pasos que
        // la componen: la relacion linea<->paso la conoce la proyeccion en este instante y en
        // ningun otro (backend#103, ADR-062 §1).
        List<PayrollConcept> payrollConcepts = new ArrayList<>();
        Map<Integer, Integer> lineaPorPaso = new HashMap<>();
        for (int i = 0; i < payslipRows.size(); i++) {
            ConceptRow r = payslipRows.get(i);
            int lineNumber = i + 1;
            payrollConcepts.add(new PayrollConcept(
                    lineNumber,
                    r.conceptCode(),
                    r.mnemonic(),
                    r.label(),
                    r.amount(),
                    r.quantity(),
                    r.rate(),
                    r.nature(),
                    command.payrollPeriodCode(),
                    r.displayOrder(),
                    r.sourceExecutionOrders().size(),
                    // Nulo si la naturaleza no tiene seccion declarada, y eso se ve. Colocarla
                    // por defecto en un bloque cualquiera seria esconderlo.
                    sectionByNature.get(r.nature()),
                    // Y el apartado dentro del bloque, cuando el bloque los tiene (backend#121).
                    // Aqui nulo NO es una ausencia que mirar: es el caso normal —una linea se
                    // imprime en su bloque— y solo las diez del recuadro de bases lo llevan.
                    subsectionByConcept.get(r.conceptCode())
            ));
            for (Integer executionOrder : r.sourceExecutionOrders()) {
                lineaPorPaso.put(executionOrder, lineNumber);
            }
        }
        calculationSteps.replaceAll(calculado -> {
            Integer lineNumber = lineaPorPaso.get(calculado.executionOrder());
            return lineNumber == null ? calculado : calculado.enLinea(lineNumber);
        });

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
                // Instant y no LocalDateTime.now(): el recibo guarda el INSTANTE en que se
                // calculo, no la hora de pared de la maquina que lo calculo (backend#116).
                Instant.now(),
                command.calculationEngineCode(),
                command.calculationEngineVersion(),
                command.runId(),
                avisos(command, input, baseReguladora),
                payrollConcepts,
                buildSnapshots(command, input),
                payrollSegments
        ));

        // Detras del recibo y no dentro: los pasos no son lineas de recibo y no cuelgan del
        // agregado. Van en la misma transaccion, que es lo que impide que un recibo nuevo se quede
        // con pasos viejos o sin ninguno (backend#93).
        payrollCalculationStepWritePort.writeStepsOf(result.getId(), calculationSteps);

        log.info("[NÓMINA] ✓ Cálculo completado | empleado={} periodo={} → {} líneas en recibo, {} pasos",
                command.employeeNumber(), command.payrollPeriodCode(),
                payrollConcepts.size(), calculationSteps.size());
        return result;
    }

    /**
     * Una linea del folio, y de que pasos viene ({@code backend#103}).
     *
     * <p>{@code sourceExecutionOrders} no es informacion de mas: una linea puede ser la suma de
     * varios pasos —el folio agrupa por {@code concepto|tarifa}— y hasta el backend#103 nada lo
     * decia. Con dos tramos al mismo precio, aunque no sean contiguos, el folio ensena una linea
     * donde el calculo dio dos, y la pestana «Calculo» ensena las dos: las dos pantallas
     * discrepaban en el numero de filas sin que nada explicara por que.
     */
    private record ConceptRow(
            String conceptCode,
            String mnemonic,
            /** Como se llama el concepto, ya resuelto y listo para congelarse (backend#109). */
            String label,
            BigDecimal amount,
            BigDecimal quantity,
            BigDecimal rate,
            String nature,
            int displayOrder,
            List<Integer> sourceExecutionOrders
    ) {}

    /**
     * Un tramo del periodo, con lo que estaba vigente en el ({@code backend#47}).
     *
     * <p>Antes esto llevaba el porcentaje de jornada como campo propio, y eso era decir que la
     * jornada es la unica causa por la que un periodo se parte. Ahora lleva las vigencias de todas
     * las verticales que parten, y la jornada es una de ellas: anadir una mas es anadir un campo
     * aqui, no volver a escribir la particion. La ausencia del {@code backend#127} entro asi.
     */
    private record SegmentSpec(
            LocalDate segmentStart,
            LocalDate segmentEnd,
            long daysInSegment,
            Vigencias vigencias
    ) {
        BigDecimal workingTimePercentage() {
            return vigencias.workingTimePercentage();
        }

        String describe() {
            return segmentStart + ".." + segmentEnd
                    + " jornada=" + vigencias.workingTimePercentage() + "%"
                    + " categoria=" + vigencias.agreementCategoryCode()
                    + " contrato=" + vigencias.contractCode()
                    + " prorrateadas=" + vigencias.extraPaymentsProrated()
                    + (vigencias.absence() == null ? ""
                       : " ausencia=" + vigencias.absence().absenceTypeCode()
                         + "(+" + vigencias.absence().daysBeforeSegment() + "d)");
        }
    }

    /**
     * Lo que cada vertical tenia vigente durante un segmento.
     *
     * <p>Son campos y no un mapa a proposito: quien calcula pregunta por la categoria, no por «la
     * vertical numero dos». Lo que hace que esto escale no es la forma de este registro sino que la
     * particion no lo mire: parte por fechas y no sabe que hay aqui dentro.
     */
    private record Vigencias(
            BigDecimal workingTimePercentage,
            String agreementCode,
            String agreementCategoryCode,
            String contractCode,
            String contractSubtypeCode,
            boolean extraPaymentsProrated,
            /** La ausencia que no se paga vigente en el tramo, o {@code null} (backend#127). */
            SegmentAbsence absence
    ) {}

    /**
     * Los importes de los conceptos {@code DIRECT_AMOUNT} de un contexto, y de que fila salio cada
     * uno.
     *
     * <p>Los dos mapas van juntos porque se llenan en la misma pasada y describen lo mismo: el
     * importe, y de donde se leyo ({@code backend#107}). La mayoria de los conceptos no aparecen en
     * el segundo, y eso es lo que significa que su paso no venga de ninguna fila.
     */
    private record PrecalculoDirecto(
            Map<String, BigDecimal> importes,
            Map<String, TableRowOrigin> filas
    ) {}

    /**
     * Parte el periodo por los puntos de cambio de las verticales que afectan al calculo
     * ({@code backend#47}).
     *
     * <p>Cada vertical aporta <b>sus fechas de corte</b>, se unen, se ordenan, y los segmentos son
     * los intervalos entre cortes consecutivos recortados contra la presencia. Este metodo ya no
     * recorre las ventanas de una vertical: la particion es de {@link PayrollPeriodSegmentation} y
     * solo sabe de fechas, asi que <b>anadir una cuarta vertical no es volver a tocarla</b>.
     *
     * <h4>Que verticales rompen, y por que estas</h4>
     *
     * <p>La lista es una decision de negocio y va creciendo por decisiones escritas: jornada,
     * clasificacion laboral y contrato (ADR-068), el regimen de pagas extras (ADR-070) y la ausencia
     * que no se paga (ADR-073). El centro de trabajo y la distribucion de coste se quedan fuera
     * <b>hasta que alguien decida que entran</b>, no porque no quepan: caben con una linea aqui y
     * otra en {@code vigenciasEn}.
     *
     * <p>Y hay un candado que lo vigila:
     * {@code TheUnionOfCutsCoversEveryVerticalThatBreaksThePeriodTest} falla si alguien quita una
     * causa. Hace falta porque quitar la del contrato no pondria en rojo ningun importe —hoy no lo
     * lee nadie, y el ADR-068 §2 dice que rompe aun asi—.
     *
     * <h4>La jornada sin ventanas</h4>
     *
     * <p>Un empleado sin ventanas de jornada se calcula al 100 %, que es lo que se hacia antes y lo
     * que hace que esto no mueva ni un recibo de los que ya salian bien.
     */
    private List<SegmentSpec> buildSegments(
            PayrollLaunchEligibleInputContext input,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        LocalDate presenceStart = input.presenceStartDate() != null && input.presenceStartDate().isAfter(periodStart)
                ? input.presenceStartDate() : periodStart;
        LocalDate presenceEnd = input.presenceEndDate() != null && input.presenceEndDate().isBefore(periodEnd)
                ? input.presenceEndDate() : periodEnd;

        List<LocalDate> cortes = new ArrayList<>();
        cortes.addAll(PayrollPeriodSegmentation.cutsOf(input.workingTimeWindows()));
        cortes.addAll(PayrollPeriodSegmentation.cutsOf(input.agreementWindows()));
        cortes.addAll(PayrollPeriodSegmentation.cutsOf(input.contractWindows()));
        cortes.addAll(PayrollPeriodSegmentation.cutsOf(input.extraPaymentRegimeWindows()));
        cortes.addAll(PayrollPeriodSegmentation.cutsOf(ausenciasQueParten(input)));

        return PayrollPeriodSegmentation.split(presenceStart, presenceEnd, cortes).stream()
                .map(tramo -> new SegmentSpec(
                        tramo.start(), tramo.end(), tramo.days(), vigenciasEn(input, tramo.start())))
                .toList();
    }

    /**
     * Lo que cada vertical tenia vigente el primer dia del segmento.
     *
     * <p>El primer dia y no cualquiera: dentro de un segmento no hay cambios <b>por construccion</b>
     * —si los hubiera, ese dia seria un corte y habria dos segmentos—, asi que preguntar por el
     * primero contesta por todos.
     */
    private Vigencias vigenciasEn(PayrollLaunchEligibleInputContext input, LocalDate dia) {
        PayrollLaunchWorkingTimeWindowContext jornada = enVigor(input.workingTimeWindows(), dia);
        PayrollLaunchAgreementWindowContext convenio = enVigor(input.agreementWindows(), dia);
        PayrollLaunchContractWindowContext contrato = enVigor(input.contractWindows(), dia);
        PayrollLaunchExtraPaymentRegimeWindowContext regimen = enVigor(input.extraPaymentRegimeWindows(), dia);
        PayrollLaunchAbsenceWindowContext ausencia = enVigor(ausenciasQueParten(input), dia);

        return new Vigencias(
                jornada != null ? jornada.workingTimePercentage() : BigDecimal.valueOf(100),
                // Sin tramo de clasificacion se cae en lo que el lanzador resolvio para el periodo,
                // que es lo unico que hay. No es lo mismo que no haber preguntado: un empleado sin
                // clasificacion ninguna no llega hasta aqui, lo para la comprobacion de entradas.
                convenio != null ? convenio.agreementCode() : input.agreementCode(),
                convenio != null ? convenio.agreementCategoryCode() : input.agreementCategoryCode(),
                contrato != null ? contrato.contractCode() : null,
                contrato != null ? contrato.contractSubtypeCode() : null,
                // Sin tramo de regimen se supone que no se prorratea, que es lo que significa no
                // tener la vertical: los empleados anteriores al backend#118 no la tienen, y sus
                // recibos salen como salian.
                regimen != null && regimen.prorated(),
                // Los dias de ausencia ya transcurridos se cuentan desde su inicio, que puede estar
                // en otro mes: una baja que empezo el 20 de agosto llega al 1 de septiembre con doce
                // dias detras. No es leer otro mes, es una fecha (backend#127).
                ausencia == null ? null : new SegmentAbsence(
                        ausencia.absenceTypeCode(),
                        ChronoUnit.DAYS.between(ausencia.startDate(), dia),
                        ausencia.benefitEntitled()));
    }

    /**
     * Las ausencias del empleado que parten el periodo: las que no se pagan ({@code backend#127}).
     *
     * <p>El lanzador trae todas las que solapan el periodo, del tipo que sean, y el filtro esta aqui
     * y solo aqui. Que la consulta las trajera ya filtradas repartiria la decision entre dos capas, y
     * la decision —<b>parte la ausencia que cambia lo que se paga</b>— es una (ADR-073).
     */
    private List<PayrollLaunchAbsenceWindowContext> ausenciasQueParten(
            PayrollLaunchEligibleInputContext input
    ) {
        if (input.absenceWindows() == null) return List.of();
        return input.absenceWindows().stream()
                .filter(a -> AUSENCIAS_QUE_NO_SE_PAGAN.contains(a.absenceTypeCode()))
                .toList();
    }

    /** El ultimo tramo que cubre ese dia, o {@code null} si ninguno lo cubre. */
    private <T extends PayrollPeriodSegmentation.DatedWindow> T enVigor(List<T> windows, LocalDate dia) {
        if (windows == null) return null;
        T vigente = null;
        for (T window : windows) {
            boolean empezado = window.startDate() == null || !window.startDate().isAfter(dia);
            boolean sinCerrar = window.endDate() == null || !window.endDate().isBefore(dia);
            if (empezado && sinCerrar) vigente = window;
        }
        return vigente;
    }

    /**
     * Que segmentos comparten precalculo de {@code DIRECT_AMOUNT}.
     *
     * <p>La categoria y el convenio, que son las dos partes con las que se busca la fila de tabla, y
     * el ultimo dia del segmento, que es la fecha a la que se busca. La jornada no entra: no se usa
     * para buscar nada, se aplica despues.
     */
    private String claveDePrecalculo(SegmentSpec seg) {
        return seg.vigencias().agreementCode() + "|" + seg.vigencias().agreementCategoryCode()
                + "|" + seg.segmentEnd();
    }

    private PayrollConceptExecutionContext contextoDe(CalculatePayrollUnitCommand command, SegmentSpec seg) {
        return new PayrollConceptExecutionContext(
                command.ruleSystemCode(),
                seg.vigencias().agreementCode(),
                seg.vigencias().agreementCategoryCode(),
                seg.segmentEnd());
    }

    private PrecalculoDirecto precalculoDirecto(
            List<ConceptExecutionPlanEntry> plan,
            PayrollConceptExecutionContext context,
            RuleSystemMetamodel metamodel
    ) {
        Map<String, BigDecimal> importes = new HashMap<>();
        Map<String, TableRowOrigin> filas = new HashMap<>();
        for (ConceptExecutionPlanEntry entry : plan) {
            if (entry.calculationType() != CalculationType.DIRECT_AMOUNT) continue;
            String conceptCode = entry.identity().getConceptCode();
            PayrollConceptExecutionResult resultado =
                    payrollConceptGraphCalculator.calculateConceptResult(conceptCode, context, metamodel);
            importes.put(conceptCode, resultado.amount());
            if (resultado.sourceTableRow() != null) {
                filas.put(conceptCode, resultado.sourceTableRow());
                log.debug("[NOMINA] {} leido de la tabla {}, fila {}", conceptCode,
                        resultado.sourceTableRow().tableCode(), resultado.sourceTableRow().rowId());
            }
            log.debug("[NOMINA] Pre-calculado DIRECT_AMOUNT {} = {} (categoria {})",
                    conceptCode, resultado.amount(), context.categoryCode());
        }
        return new PrecalculoDirecto(importes, filas);
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
            Map<String, BigDecimal> precomputedDirectAmounts,
            String cnaeCode
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
                precomputedDirectAmounts,
                // Lo que es del PERIODO se resuelve con el ultimo tramo, como todo lo demas. La
                // prorrata no lo usa —sus dos conceptos son de tramo, que es donde el regimen
                // significa algo—, pero el contexto no puede quedarse sin contestar.
                segments.getLast().vigencias().extraPaymentsProrated(),
                cnaeCode,
                // Y el contrato, por lo mismo: el tipo de desempleo es de tramo y no lo mira desde
                // aqui, pero el contexto del periodo no puede quedarse sin contestar (backend#124).
                segments.getLast().vigencias().contractCode()
        );
    }

    /**
     * Un paso del recorrido: lo que el motor calculo, con su ambito y, si lo tiene, su segmento.
     * Se guarda tenga o no sitio en el folio, que es de lo que va este issue.
     */
    private PayrollCalculationStep calculationStep(
            int executionOrder,
            com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept engineConcept,
            ConceptExecutionPlanEntry entry,
            SegmentExecutionState state,
            BigDecimal amount,
            LocalDate segmentStart,
            LocalDate segmentEnd,
            Map<String, TableRowOrigin> filaLeidaPorConcepto
    ) {
        return new PayrollCalculationStep(
                executionOrder,
                engineConcept.getConceptCode(),
                engineConcept.getConceptMnemonic(),
                entry.calculationType().name(),
                engineConcept.getFunctionalNature().name(),
                engineConcept.getExecutionScope().name(),
                segmentStart,
                segmentEnd,
                amount,
                quantityOf(entry, state),
                rateOf(entry, state),
                engineConcept.getPayslipOrderCode(),
                filaLeidaPorConcepto.get(engineConcept.getConceptCode()));
    }

    /**
     * La proyeccion: de paso calculado a linea de recibo.
     *
     * <p>Solo la hacen los pasos con {@code payslipOrderCode}, y no son copias. El
     * {@code quantity} de una linea es una decision de presentacion —la CANTIDAD de un
     * RATE_BY_QUANTITY, la BASE de un PERCENTAGE—, y esa regla vive aqui, en un sitio, en vez de
     * estar implicita en que dos recorridos hagan lo mismo de dos maneras (backend#93).
     */
    private ConceptRow toPayslipRow(PayrollCalculationStep step, Map<String, String> conceptLabels) {
        return new ConceptRow(
                step.conceptCode(),
                step.conceptMnemonic(),
                // El nombre del concepto, o su mnemonico si nadie se lo ha puesto (backend#109).
                // El dia que alguien anada un concepto y se olvide del literal, el recibo no se
                // rompe y no ensena un hueco: ensena la clave y se nota que falta.
                conceptLabels.getOrDefault(step.conceptCode(), step.conceptMnemonic()),
                step.amount(),
                step.quantity(),
                step.rate(),
                step.functionalNature(),
                Integer.parseInt(step.payslipOrderCode()),
                List.of(step.executionOrder()));
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

    /**
     * La regla del cero ({@code backend#104}).
     *
     * <p><b>Una linea de concepto a cero no se imprime. Un total a cero, si.</b>
     *
     * <p>Un concepto que no aplica no sale en la nomina: nadie cobra una linea de cero euros. Sin
     * esto, declarar un concepto ocasional —unas horas extra— estrenaria una primera linea de
     * {@code 0,00} en los ~873 recibos de quien no las tenga, porque
     * {@code payroll_engine.concept_assignment} acota por sociedad, convenio y tipo de empleado y
     * <b>no por empleado</b>.
     *
     * <p>La excepcion de los totales hay que escribirla o alguien la rompera al «limpiar los ceros»:
     * un liquido de {@code 0,00} es un dato —dice que ese mes no se cobro— mientras que un concepto
     * a {@code 0,00} es ruido. {@code TOTAL_EARNING}, {@code TOTAL_DEDUCTION} y {@code NET_PAY} se
     * imprimen siempre.
     *
     * <p><b>Y el paso sigue existiendo.</b> Esto vive en la proyeccion y no en el motor: un paso a
     * cero es un calculo que ocurrio y dio cero, y borrarlo del motor seria mentir en la explicacion
     * para arreglar el documento. La pestana «Calculo» lo sigue ensenando con su cero; lo que cambia
     * es lo que el folio pinta, que es de lo que habla el ADR-062 §1.
     *
     * <p>Se aplica <b>despues</b> del colapso a proposito: lo que decide es el importe de la linea,
     * no el de cada tramo. Un concepto con dos tramos que se compensan vale cero y no se imprime;
     * filtrando antes, se habria comido un tramo y dejado la linea con la mitad.
     */
    private static boolean noSeImprimePorValerCero(ConceptRow row) {
        if (esTotal(row.nature())) {
            return false;
        }
        return row.amount() != null && row.amount().signum() == 0;
    }

    private static boolean esTotal(String nature) {
        return "TOTAL_EARNING".equals(nature)
                || "TOTAL_DEDUCTION".equals(nature)
                || "NET_PAY".equals(nature);
    }

    private List<ConceptRow> collapsePayslipRows(List<ConceptRow> rows) {
        LinkedHashMap<String, ConceptRow> collapsed = new LinkedHashMap<>();
        for (ConceptRow row : rows) {
            String key = row.conceptCode() + "|" + (row.rate() != null
                    ? row.rate().stripTrailingZeros().toPlainString() : "null");
            collapsed.merge(key, row, (existing, incoming) -> new ConceptRow(
                    existing.conceptCode(),
                    existing.mnemonic(),
                    existing.label(),
                    existing.amount().add(incoming.amount()),
                    existing.quantity() != null && incoming.quantity() != null
                            ? existing.quantity().add(incoming.quantity())
                            : existing.quantity(),
                    existing.rate(),
                    existing.nature(),
                    existing.displayOrder(),
                    // Los pasos que esta linea funde, en orden de ejecucion. Es lo unico que hay
                    // que llevarse de la fusion: el resto ya lo dice la suma.
                    Stream.concat(existing.sourceExecutionOrders().stream(),
                                  incoming.sourceExecutionOrders().stream()).toList()
            ));
        }
        return new ArrayList<>(collapsed.values());
    }

    /**
     * Lo que se supo del mes anterior, y lo que hay que hacer con ello ({@code backend#128}).
     *
     * @param diaria la base reguladora diaria leida del recibo cerrado del mes anterior, o
     *        {@code null} si no hay recibo cerrado o si esta unidad no necesita base reguladora. En
     *        los dos casos el motor hace lo mismo: usar la base teorica de este mes, que la calcula
     *        el grafo
     * @param aviso lo que el recibo tiene que decir de esto, o {@code null} si no hay nada que decir
     * @param motivoNoCalculada el codigo de estado con el que el recibo queda {@code NOT_VALID}, o
     *        {@code null} si se calcula
     */
    private record BaseReguladoraDelMesAnterior(
            BigDecimal diaria,
            PayrollWarning aviso,
            String motivoNoCalculada
    ) {
        boolean noSeCalcula() {
            return motivoNoCalculada != null;
        }
    }

    /**
     * La base reguladora diaria de este empleado, o el motivo por el que su recibo no se calcula
     * ({@code backend#128}, ADR-074).
     *
     * <h4>Se pregunta solo si hace falta</h4>
     *
     * <p>Si el empleado no tiene ninguna baja por enfermedad comun en el periodo, <b>no se lee
     * nada</b>: no hay prestacion que calcular ni base durante la baja, asi que no hay ninguna razon
     * para que el mes anterior de este empleado tenga que estar cerrado. Lo contrario seria dejar sin
     * calcular los ochocientos sesenta recibos de una empresa porque el mes pasado quedo uno a medio
     * revisar.
     *
     * <h4>Las tres respuestas</h4>
     *
     * <ol>
     *   <li><b>Recibo cerrado</b> - su base de contingencias comunes entre los dias del mes.</li>
     *   <li><b>Recibo que todavia puede cambiar</b> - este recibo no se calcula y queda
     *       {@code NOT_VALID} con su motivo, como cuando falta una entrada. Un numero que puede
     *       cambiar no se lee.</li>
     *   <li><b>No hay recibo</b> - la base teorica de este mes, que la calcula el grafo. Con aviso,
     *       salvo que el empleado entrara este mes: entonces es lo que dice la ley y no hay nada de
     *       lo que avisar.</li>
     * </ol>
     */
    private BaseReguladoraDelMesAnterior resolverBaseReguladora(
            CalculatePayrollUnitCommand command,
            PayrollLaunchEligibleInputContext input,
            String tipoNomina
    ) {
        if (!tieneBajaPorEnfermedadComun(input)) {
            return new BaseReguladoraDelMesAnterior(null, null, null);
        }

        String periodoAnterior = periodoAnterior(command.payrollPeriodCode());
        PreviousPeriodContributionBase anterior =
                previousPeriodContributionBaseLookupPort.findByEmployeeAndPeriod(
                        command.ruleSystemCode(),
                        command.employeeTypeCode(),
                        command.employeeNumber(),
                        command.payrollTypeCode(),
                        periodoAnterior);

        log.info("[NOMINA] Base reguladora | periodo anterior={} -> {}",
                periodoAnterior, anterior.outcome());

        return switch (anterior.outcome()) {
            case DEFINITIVE -> new BaseReguladoraDelMesAnterior(
                    baseDiaria(anterior.contributionBase(), tipoNomina, periodoAnterior), null, null);

            case NOT_DEFINITIVE -> new BaseReguladoraDelMesAnterior(
                    null,
                    aviso("PREVIOUS_PAYROLL_NOT_DEFINITIVE", "ERROR",
                            "El recibo de " + periodoAnterior + " existe y no es definitivo: la base"
                                    + " reguladora saldria de un numero que todavia puede cambiar,"
                                    + " asi que este recibo no se calcula",
                            command, input, periodoAnterior),
                    "PREVIOUS_PAYROLL_NOT_DEFINITIVE");

            case NONE -> entroEnEstePeriodo(command, input)
                    ? new BaseReguladoraDelMesAnterior(null, null, null)
                    : new BaseReguladoraDelMesAnterior(
                            null,
                            aviso("REGULATORY_BASE_FROM_CURRENT_PERIOD", "WARNING",
                                    "Base reguladora tomada del mes en curso: no hay recibo"
                                            + " definitivo de " + periodoAnterior,
                                    command, input, periodoAnterior),
                            null);
        };
    }

    /**
     * Si esta unidad necesita base reguladora: tiene alguna baja por enfermedad comun en el periodo.
     *
     * <p>El permiso no retribuido <b>no</b> cuenta, aunque tambien quite dias: no paga prestacion y
     * no cotiza, asi que no hay ninguna base que regular.
     */
    private boolean tieneBajaPorEnfermedadComun(PayrollLaunchEligibleInputContext input) {
        if (input.absenceWindows() == null) return false;
        return input.absenceWindows().stream()
                .anyMatch(a -> SegmentAbsence.IT_COMMON.equals(a.absenceTypeCode()));
    }

    /** Si el empleado entro en este mismo periodo, que es el caso que la ley resuelve sin aviso. */
    private boolean entroEnEstePeriodo(
            CalculatePayrollUnitCommand command,
            PayrollLaunchEligibleInputContext input
    ) {
        return input.presenceStartDate() != null
                && !input.presenceStartDate().isBefore(command.periodStart());
    }

    /**
     * El periodo anterior. Que sea el mes de antes es una regla de negocio y por eso se decide aqui y
     * no en la consulta.
     */
    private String periodoAnterior(String payrollPeriodCode) {
        return YearMonth.parse(payrollPeriodCode, PERIODO).minusMonths(1).format(PERIODO);
    }

    /**
     * La base del mes anterior repartida entre sus dias.
     *
     * <p>Se divide con seis decimales y <b>no se redondea aqui</b>: el redondeo es del concepto que
     * recibe el numero, y se aplica una sola vez (ADR-066).
     */
    private BigDecimal baseDiaria(BigDecimal baseDelMes, String tipoNomina, String periodoAnterior) {
        BigDecimal dias = TIPO_NOMINA_DIARIA.equals(tipoNomina)
                ? BigDecimal.valueOf(YearMonth.parse(periodoAnterior, PERIODO).lengthOfMonth())
                : DIAS_DEL_MES_NOMINA;
        return baseDelMes.divide(dias, 6, RoundingMode.HALF_UP);
    }

    /**
     * El recibo que no se calcula ({@code backend#128}).
     *
     * <p>Se guarda, y por eso: un recibo {@code NOT_VALID} con su motivo es lo que hace visible que
     * este empleado falta, y ademas es lo unico desde donde se puede recalcular cuando el mes
     * anterior se cierre. No guardar nada dejaria el hueco sin explicacion, que es la forma de fallo
     * que este proyecto no admite.
     *
     * <p>Sin lineas y sin pasos, porque no se ha calculado nada. Lo que lleva es el aviso, que es
     * donde esta escrito con palabras que le pasa.
     */
    private Payroll recibioNoCalculado(
            CalculatePayrollUnitCommand command,
            PayrollLaunchEligibleInputContext input,
            BaseReguladoraDelMesAnterior baseReguladora
    ) {
        log.warn("[NOMINA] Recibo NO CALCULADO | empleado={} periodo={} motivo={}",
                command.employeeNumber(), command.payrollPeriodCode(),
                baseReguladora.motivoNoCalculada());

        return calculatePayrollUseCase.calculate(new CalculatePayrollCommand(
                command.ruleSystemCode(),
                command.employeeTypeCode(),
                command.employeeNumber(),
                command.payrollPeriodCode(),
                command.payrollTypeCode(),
                command.presenceNumber(),
                PayrollStatus.NOT_VALID,
                baseReguladora.motivoNoCalculada(),
                Instant.now(),
                command.calculationEngineCode(),
                command.calculationEngineVersion(),
                command.runId(),
                avisos(command, input, baseReguladora),
                List.of(),
                buildSnapshots(command, input),
                List.of()
        ));
    }

    /** Los avisos del recibo: el de siempre, y el de la base reguladora cuando hay algo que decir. */
    private List<PayrollWarning> avisos(
            CalculatePayrollUnitCommand command,
            PayrollLaunchEligibleInputContext input,
            BaseReguladoraDelMesAnterior baseReguladora
    ) {
        PayrollWarning deSiempre = eligibleRealWarning(command, input);
        return baseReguladora.aviso() == null
                ? List.of(deSiempre)
                : List.of(deSiempre, baseReguladora.aviso());
    }

    private PayrollWarning aviso(
            String codigo,
            String severidad,
            String mensaje,
            CalculatePayrollUnitCommand command,
            PayrollLaunchEligibleInputContext input,
            String periodoAnterior
    ) {
        Map<String, Object> detalles = new LinkedHashMap<>();
        detalles.put("previousPeriodCode", periodoAnterior);
        detalles.put("employeeTypeCode", command.employeeTypeCode());
        detalles.put("employeeNumber", command.employeeNumber());
        detalles.put("presenceStartDate",
                input.presenceStartDate() == null ? null : input.presenceStartDate().toString());
        return new PayrollWarning(null, null, codigo, severidad, mensaje, toJson(detalles));
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

        // La foto fiscal se guarda siempre, haya declaracion o no, y por eso es la unica que no
        // usa ifPresent: las otras cuatro se omiten cuando no hay dato, que es una ausencia que
        // se ve. Esta no puede omitirse porque el calculo si tuvo una situacion delante, asi que
        // lo que dice de donde salio es el campo "source" del payload (backend#92).
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
        // Primero, y dentro del payload y no al lado: quien lea los campos de abajo tiene que
        // leer antes de donde salen. DEFAULT_NO_DECLARATION dice que esta situacion no la
        // declaro nadie (backend#92).
        payload.put("source", ctx.source().name());
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
        // Va en la foto y no se recalcula al leer: un recibo dice lo que valia al calcularlo,
        // tambien si manana alguien corrige la primera presencia (backend#91).
        payload.put("seniorityDate", input.seniorityDate() != null ? input.seniorityDate().toString() : null);

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

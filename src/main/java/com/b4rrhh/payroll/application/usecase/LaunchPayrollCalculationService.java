package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.application.port.PayrollLaunchPresenceContext;
import com.b4rrhh.payroll.application.port.PayrollLaunchPresenceLookupPort;
import com.b4rrhh.payroll.application.port.PayrollLaunchWorkerPort;
import com.b4rrhh.payroll.application.port.PayrollLaunchEmployeeContext;
import com.b4rrhh.payroll.domain.exception.InvalidPayrollArgumentException;
import com.b4rrhh.payroll.domain.exception.PayrollRecalculationNotAllowedException;
import com.b4rrhh.payroll.domain.model.CalculationClaim;
import com.b4rrhh.payroll.domain.model.CalculationRun;
import com.b4rrhh.payroll.domain.model.CalculationRunMessage;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.port.CalculationClaimRepository;
import com.b4rrhh.payroll.domain.port.CalculationRunMessageRepository;
import com.b4rrhh.payroll.domain.port.CalculationRunRepository;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodel;
import com.b4rrhh.payroll_engine.metamodel.domain.port.RuleSystemMetamodelRepository;
import com.b4rrhh.payroll_engine.planning.application.service.UnreachableConceptFinder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;

@Service
public class LaunchPayrollCalculationService implements LaunchPayrollCalculationUseCase {

    private static final Logger log = LoggerFactory.getLogger(LaunchPayrollCalculationService.class);

    private static final DateTimeFormatter PAYROLL_PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    /** Cuantos codigos caben en el texto del aviso antes de remitir al detalle. */
    private static final int CONCEPTOS_INALCANZABLES_EN_EL_TEXTO = 10;

    private final CalculationRunRepository calculationRunRepository;
    private final CalculationClaimRepository calculationClaimRepository;
    private final CalculationRunMessageRepository calculationRunMessageRepository;
    private final PayrollRepository payrollRepository;
    private final PayrollLaunchPresenceLookupPort payrollLaunchPresenceLookupPort;
    private final CalculatePayrollUnitUseCase calculatePayrollUnitUseCase;
    private final PayrollLaunchWorkerPort payrollLaunchWorkerPort;
    private final RuleSystemMetamodelRepository ruleSystemMetamodelRepository;
    private final UnreachableConceptFinder unreachableConceptFinder;
    private final ObjectMapper objectMapper;

    public LaunchPayrollCalculationService(
            CalculationRunRepository calculationRunRepository,
            CalculationClaimRepository calculationClaimRepository,
            CalculationRunMessageRepository calculationRunMessageRepository,
            PayrollRepository payrollRepository,
            PayrollLaunchPresenceLookupPort payrollLaunchPresenceLookupPort,
            CalculatePayrollUnitUseCase calculatePayrollUnitUseCase,
            PayrollLaunchWorkerPort payrollLaunchWorkerPort,
            RuleSystemMetamodelRepository ruleSystemMetamodelRepository,
            UnreachableConceptFinder unreachableConceptFinder,
            ObjectMapper objectMapper
    ) {
        this.calculationRunRepository = calculationRunRepository;
        this.calculationClaimRepository = calculationClaimRepository;
        this.calculationRunMessageRepository = calculationRunMessageRepository;
        this.payrollRepository = payrollRepository;
        this.payrollLaunchPresenceLookupPort = payrollLaunchPresenceLookupPort;
        this.calculatePayrollUnitUseCase = calculatePayrollUnitUseCase;
        this.payrollLaunchWorkerPort = payrollLaunchWorkerPort;
        this.ruleSystemMetamodelRepository = ruleSystemMetamodelRepository;
        this.unreachableConceptFinder = unreachableConceptFinder;
        this.objectMapper = objectMapper;
    }

    /**
     * Crea la ejecucion, la encola y vuelve. Es el camino de la API (#75).
     *
     * <p>La validacion del encargo sigue siendo sincrona: una peticion mal formada se
     * contesta con un 400 y no deja ejecucion ninguna. Lo que deja de ser sincrono es el
     * calculo, que son cinco minutos para mil empleados y ningun intermediario aguanta esa
     * espera.
     */
    @Override
    public CalculationRun requestLaunch(LaunchPayrollCalculationCommand command) {
        NormalizedLaunch normalizedLaunch = normalize(command);
        CalculationRun run = createRequestedRun(normalizedLaunch);

        try {
            payrollLaunchWorkerPort.submit(() -> execute(run, normalizedLaunch));
        } catch (RejectedExecutionException ex) {
            // Aceptada y perdida es el peor final posible: si no cabe en la cola, la
            // ejecucion se cierra aqui mismo y lo dice.
            saveRunMessage(run, "LAUNCH_REJECTED", "ERROR",
                    "Payroll launch was rejected because the launch queue is full",
                    Map.of("exceptionType", ex.getClass().getSimpleName()), null);
            return calculationRunRepository.save(run.withFinishedExecutionEvenIfNeverStarted(
                    CalculationRunStatuses.FAILED,
                    LocalDateTime.now(),
                    buildSummaryJson(run)
            ));
        }

        return run;
    }

    /**
     * Lanza y espera. Es el camino en proceso: escenarios y tests, que corren dentro de su
     * propia transaccion y necesitan el resultado en la misma llamada. No lo usa la API.
     */
    @Override
    public CalculationRun launch(LaunchPayrollCalculationCommand command) {
        NormalizedLaunch normalizedLaunch = normalize(command);
        return execute(createRequestedRun(normalizedLaunch), normalizedLaunch);
    }

    private NormalizedLaunch normalize(LaunchPayrollCalculationCommand command) {
        String payrollPeriodCode = normalizeCode(command.payrollPeriodCode(), "payrollPeriodCode", 30);
        LocalDate[] periodBounds = parsePayrollPeriodBounds(payrollPeriodCode);

        return new NormalizedLaunch(
                normalizeCode(command.ruleSystemCode(), "ruleSystemCode", 5),
                payrollPeriodCode,
                normalizeCode(command.payrollTypeCode(), "payrollTypeCode", 30),
                normalizeText(command.calculationEngineCode(), "calculationEngineCode", 50),
                normalizeText(command.calculationEngineVersion(), "calculationEngineVersion", 50),
                normalizeTargetSelection(command.targetSelection()),
                normalizeOptionalText(command.requestedBy(), "requestedBy", 100),
                periodBounds[0],
                periodBounds[1]
        );
    }

    private CalculationRun createRequestedRun(NormalizedLaunch normalizedLaunch) {
        return calculationRunRepository.save(new CalculationRun(
                null,
                normalizedLaunch.ruleSystemCode(),
                normalizedLaunch.payrollPeriodCode(),
                normalizedLaunch.payrollTypeCode(),
                normalizedLaunch.calculationEngineCode(),
                normalizedLaunch.calculationEngineVersion(),
                LocalDateTime.now(),
                normalizedLaunch.requestedBy(),
                CalculationRunStatuses.REQUESTED,
                toJson(normalizedLaunch.targetSelection(), "targetSelection"),
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                null,
                null,
                null,
                null,
                null
        ));
    }

    /**
     * El trabajo. Corre en el hilo de la peticion cuando se llama a {@link #launch}, y en
     * el hilo del worker cuando se llama a {@link #requestLaunch}.
     *
     * <p>Sin {@code @Transactional}, y no por descuido: los contadores se guardan unidad
     * por unidad para que la ejecucion se vea avanzar desde fuera. En una sola transaccion
     * no se veria nada hasta el final, que es justo lo contrario de lo que hace falta.
     *
     * <p><b>La reglamentacion se lee aqui, una vez, y no la vuelve a leer nadie.</b> El
     * metamodelo del motor —los conceptos con sus operandos, sus alimentaciones y sus
     * asignaciones— no cambia mientras la ejecucion corre, y ahora eso esta garantizado en
     * vez de ser una casualidad: lo que se cargue en esta linea es lo que van a usar la
     * unidad 1 y la 873. Antes lo releia cada unidad, y un cambio en el grafo a mitad de una
     * corrida de minutos dejaba dos nominas distintas con los mismos datos sin que nada lo
     * dijera (backend#87). La vida de lo cargado es esta ejecucion: la siguiente vuelve a
     * leer, y por eso ve los cambios.
     */
    private CalculationRun execute(CalculationRun requestedRun, NormalizedLaunch normalizedLaunch) {
        CalculationRun run = requestedRun;
        try {
            run = calculationRunRepository.save(run.withStatus(CalculationRunStatuses.RUNNING).withStartedAt(LocalDateTime.now()));

            RuleSystemMetamodel metamodel = ruleSystemMetamodelRepository.load(
                    normalizedLaunch.ruleSystemCode(),
                    normalizedLaunch.periodEnd()
            );

            avisarDeConceptosInalcanzables(run, metamodel);

            List<PayrollCalculationUnit> units = expandUnits(
                    run,
                    normalizedLaunch.targetSelection(),
                    normalizedLaunch.ruleSystemCode(),
                    normalizedLaunch.payrollPeriodCode(),
                    normalizedLaunch.payrollTypeCode(),
                    normalizedLaunch.periodStart(),
                    normalizedLaunch.periodEnd()
            );
                    // totalCandidates counts expanded calculation units after presence overlap resolution, not raw target employees.
            run = calculationRunRepository.save(run.withTotalCandidates(units.size()));

            // Secuencial a proposito: los ocho contadores de CalculationRun se suman leyendo el
            // objeto, sumando uno y guardando la fila entera, asi que un parallelStream aqui los
            // dejaria mintiendo sin un solo error en los registros (backend#83).
            for (PayrollCalculationUnit unit : units) {
                run = processUnit(run, unit, normalizedLaunch.calculationEngineCode(),
                        normalizedLaunch.calculationEngineVersion(), metamodel);
            }

            String finalStatus = run.totalErrors() > 0
                    ? CalculationRunStatuses.COMPLETED_WITH_ERRORS
                    : CalculationRunStatuses.COMPLETED;
            run = calculationRunRepository.save(run.withFinishedExecution(
                    finalStatus,
                    LocalDateTime.now(),
                    buildSummaryJson(run)
            ));
                cleanupClaimsByRunId(run.id());
            return run;
        } catch (RuntimeException ex) {
            saveRunMessage(run, "LAUNCH_ABORTED", "ERROR", ex.getMessage(),
                    Map.of("exceptionType", ex.getClass().getSimpleName()), null);
            CalculationRun failedRun = calculationRunRepository.save(run.withFinishedExecutionEvenIfNeverStarted(
                    CalculationRunStatuses.FAILED,
                    LocalDateTime.now(),
                    buildSummaryJson(run.incrementTotalErrors())
            ).incrementTotalErrors());
                cleanupClaimsByRunId(failedRun.id());
            return failedRun;
        }
    }

    /**
     * El encargo ya validado y normalizado, con el mes resuelto a fechas. Existe para que
     * pedir y ejecutar puedan ser dos momentos distintos sin normalizar dos veces.
     */
    private record NormalizedLaunch(
            String ruleSystemCode,
            String payrollPeriodCode,
            String payrollTypeCode,
            String calculationEngineCode,
            String calculationEngineVersion,
            PayrollLaunchTargetSelection targetSelection,
            String requestedBy,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
    }

    private List<PayrollCalculationUnit> expandUnits(
            CalculationRun run,
            PayrollLaunchTargetSelection targetSelection,
            String ruleSystemCode,
            String payrollPeriodCode,
            String payrollTypeCode,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        List<PayrollCalculationUnit> units = new ArrayList<>();
        for (PayrollLaunchEmployeeTarget employeeTarget : resolveEmployees(targetSelection, ruleSystemCode, periodStart, periodEnd)) {
            List<PayrollLaunchPresenceContext> presences = payrollLaunchPresenceLookupPort.findRelevantPresences(
                    ruleSystemCode,
                    employeeTarget.employeeTypeCode(),
                    employeeTarget.employeeNumber(),
                    periodStart,
                    periodEnd
            );
            if (presences.isEmpty()) {
                saveRunMessage(
                        run,
                        "NO_RELEVANT_PRESENCE",
                        "WARNING",
                        "No relevant employee presence was found for payroll launch target",
                        Map.of(
                                "periodStart", periodStart.toString(),
                                "periodEnd", periodEnd.toString()
                        ),
                        new PayrollCalculationUnit(
                                ruleSystemCode,
                                employeeTarget.employeeTypeCode(),
                                employeeTarget.employeeNumber(),
                                payrollPeriodCode,
                                payrollTypeCode,
                                null
                        )
                );
                continue;
            }

            for (PayrollLaunchPresenceContext presence : presences) {
                units.add(new PayrollCalculationUnit(
                        ruleSystemCode,
                        presence.employeeTypeCode(),
                        presence.employeeNumber(),
                        payrollPeriodCode,
                        payrollTypeCode,
                        presence.presenceNumber()
                ));
            }
        }
        return units;
    }

    private CalculationRun processUnit(
            CalculationRun run,
            PayrollCalculationUnit unit,
            String calculationEngineCode,
            String calculationEngineVersion,
            RuleSystemMetamodel metamodel
    ) {
        Optional<Payroll> existingPayroll = payrollRepository.findByBusinessKey(
                unit.ruleSystemCode(),
                unit.employeeTypeCode(),
                unit.employeeNumber(),
                unit.payrollPeriodCode(),
                unit.payrollTypeCode(),
                unit.presenceNumber()
        );

        if (existingPayroll.isPresent() && existingPayroll.get().getStatus() != PayrollStatus.NOT_VALID) {
            saveRunMessage(
                    run,
                    "UNIT_NOT_ELIGIBLE",
                    "WARNING",
                    "Payroll calculation unit is not eligible because an immutable payroll already exists",
                    Map.of("existingStatus", existingPayroll.get().getStatus().name()),
                    unit
            );
            return calculationRunRepository.save(run.incrementTotalSkippedNotEligible());
        }

        run = calculationRunRepository.save(run.incrementTotalEligible());

        LocalDate[] unitPeriodBounds = parsePayrollPeriodBounds(unit.payrollPeriodCode());

        CalculationClaim claim;
        try {
            claim = calculationClaimRepository.save(new CalculationClaim(
                    null,
                    run.id(),
                    unit.ruleSystemCode(),
                    unit.employeeTypeCode(),
                    unit.employeeNumber(),
                    unit.payrollPeriodCode(),
                    unit.payrollTypeCode(),
                    unit.presenceNumber(),
                    LocalDateTime.now(),
                    null
            ));
            run = calculationRunRepository.save(run.incrementTotalClaimed());
        } catch (DataIntegrityViolationException ex) {
            saveRunMessage(
                    run,
                    "UNIT_ALREADY_CLAIMED",
                    "WARNING",
                    "Payroll calculation unit is already claimed by another run",
                    Map.of("reason", ex.getClass().getSimpleName()),
                    unit
            );
            return calculationRunRepository.save(run.incrementTotalSkippedAlreadyClaimed());
        }

        try {
            Payroll payroll = calculatePayrollUnitUseCase.calculate(new CalculatePayrollUnitCommand(
                    unit.ruleSystemCode(),
                    unit.employeeTypeCode(),
                    unit.employeeNumber(),
                    unit.payrollPeriodCode(),
                    unit.payrollTypeCode(),
                    unit.presenceNumber(),
                    unitPeriodBounds[0],
                    unitPeriodBounds[1],
                    calculationEngineCode,
                    calculationEngineVersion,
                    run.id(),
                    metamodel
            ));
            saveEligibleRealSuccessMessageIfPresent(run, unit, payroll);
            if (payroll.getStatus() == PayrollStatus.NOT_VALID) {
                return calculationRunRepository.save(run.incrementTotalNotValid());
            }
            return calculationRunRepository.save(run.incrementTotalCalculated());
        } catch (PayrollLaunchInputMissingException ex) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("reasonCode", ex.getReasonCode());
            details.putAll(ex.getDetails());
            saveRunMessage(
                    run,
                    "UNIT_ELIGIBLE_REAL_SKIPPED_MISSING_INPUT",
                    "WARNING",
                    ex.getMessage(),
                    details,
                    unit
            );
            // Su propio contador desde el backend#85. Sumaba a total_skipped_not_eligible, que
            // es la lectura contraria: «ya estaba hecho» no pide nada de nadie y esto siempre
            // pide que alguien mire. El literal ya decia ELIGIBLE y sumaba a NOT_ELIGIBLE.
            return calculationRunRepository.save(run.incrementTotalSkippedMissingInput());
        } catch (PayrollRecalculationNotAllowedException ex) {
            // La unidad se la llevo otro: entre el filtro de elegibilidad de arriba y este
            // calculo, alguien la dejo calculada. Eso no es un fallo de calculo —ahi no fallo
            // nada— y contarlo como tal ponia en rojo una corrida de mil empleados por un clic
            // en «Recalcular», con una explicacion falsa escrita al lado (backend#101).
            //
            // Y la reserva no lo evita, aunque desde el backend#101 la tomen los dos caminos:
            // el recalculo la suelta al confirmar su transaccion, asi que esta unidad puede
            // conseguirla justo despues y encontrarse el recibo ya CALCULATED. El contador de
            // este caso es el mismo, porque el caso es el mismo: la unidad estaba cogida.
            saveRunMessage(
                    run,
                    "UNIT_ALREADY_CLAIMED",
                    "WARNING",
                    "Payroll calculation unit was calculated by another path while this run held it",
                    Map.of("reason", ex.getClass().getSimpleName()),
                    unit
            );
            return calculationRunRepository.save(run.incrementTotalSkippedAlreadyClaimed());
        } catch (RuntimeException ex) {
            saveRunMessage(
                    run,
                    "UNIT_CALCULATION_ERROR",
                    "ERROR",
                    ex.getMessage(),
                    Map.of("exceptionType", ex.getClass().getSimpleName()),
                    unit
            );
            return calculationRunRepository.save(run.incrementTotalErrors());
        } finally {
            calculationClaimRepository.deleteById(claim.id());
        }
    }

    private void saveEligibleRealSuccessMessageIfPresent(
            CalculationRun run,
            PayrollCalculationUnit unit,
            Payroll payroll
    ) {
        payroll.getWarnings().stream()
                .filter(w -> "ELIGIBLE_REAL_EXECUTION".equals(w.warningCode()))
                .findFirst()
                .ifPresent(warning -> {
                    LinkedHashMap<String, Object> details = new LinkedHashMap<>();
                    if (warning.detailsJson() != null && !warning.detailsJson().isBlank()) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> parsed = objectMapper.readValue(warning.detailsJson(), Map.class);
                            details.putAll(parsed);
                        } catch (Exception ignored) {
                            details.put("rawDetails", warning.detailsJson());
                        }
                    }
                    saveRunMessage(
                            run,
                            "UNIT_ELIGIBLE_REAL_EXECUTED",
                            "INFO",
                            "Eligible real payroll execution completed",
                            details,
                            unit
                    );
                });
    }

    private List<PayrollLaunchEmployeeTarget> resolveEmployees(
            PayrollLaunchTargetSelection targetSelection,
            String ruleSystemCode,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        List<PayrollLaunchEmployeeTarget> rawTargets = switch (targetSelection.selectionType()) {
            case SINGLE_EMPLOYEE -> List.of(targetSelection.employee());
            case EMPLOYEE_LIST -> targetSelection.employees();
            case ALL_EMPLOYEES_WITH_PRESENCE_IN_PERIOD -> payrollLaunchPresenceLookupPort
                    .findEmployeesWithPresenceInPeriod(ruleSystemCode, periodStart, periodEnd)
                    .stream()
                    .map(employee -> new PayrollLaunchEmployeeTarget(employee.employeeTypeCode(), employee.employeeNumber()))
                    .toList();
        };

        LinkedHashMap<String, PayrollLaunchEmployeeTarget> uniqueTargets = new LinkedHashMap<>();
        for (PayrollLaunchEmployeeTarget rawTarget : rawTargets) {
            String employeeTypeCode = normalizeCode(rawTarget.employeeTypeCode(), "targetSelection.employeeTypeCode", 30);
            String employeeNumber = normalizeText(rawTarget.employeeNumber(), "targetSelection.employeeNumber", 15);
            PayrollLaunchEmployeeTarget normalizedTarget = new PayrollLaunchEmployeeTarget(employeeTypeCode, employeeNumber);
            uniqueTargets.put(employeeTypeCode + "|" + employeeNumber, normalizedTarget);
        }
        return List.copyOf(uniqueTargets.values());
    }

    private PayrollLaunchTargetSelection normalizeTargetSelection(PayrollLaunchTargetSelection targetSelection) {
        if (targetSelection == null || targetSelection.selectionType() == null) {
            throw new InvalidPayrollArgumentException("targetSelection.selectionType is required");
        }

        return switch (targetSelection.selectionType()) {
            case SINGLE_EMPLOYEE -> {
                if (targetSelection.employee() == null) {
                    throw new InvalidPayrollArgumentException("targetSelection.employee is required for SINGLE_EMPLOYEE");
                }
                yield new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        targetSelection.employee(),
                        null
                );
            }
            case EMPLOYEE_LIST -> {
                if (targetSelection.employees() == null || targetSelection.employees().isEmpty()) {
                    throw new InvalidPayrollArgumentException("targetSelection.employees is required for EMPLOYEE_LIST");
                }
                yield new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.EMPLOYEE_LIST,
                        null,
                        List.copyOf(targetSelection.employees())
                );
            }
            case ALL_EMPLOYEES_WITH_PRESENCE_IN_PERIOD -> {
                if (targetSelection.employee() != null || targetSelection.employees() != null) {
                    throw new InvalidPayrollArgumentException(
                            "targetSelection.employee and targetSelection.employees must be null for ALL_EMPLOYEES_WITH_PRESENCE_IN_PERIOD"
                    );
                }
                yield new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.ALL_EMPLOYEES_WITH_PRESENCE_IN_PERIOD,
                        null,
                        null
                );
            }
        };
    }

    private LocalDate[] parsePayrollPeriodBounds(String payrollPeriodCode) {
        try {
            YearMonth yearMonth = YearMonth.parse(payrollPeriodCode, PAYROLL_PERIOD_FORMATTER);
            return new LocalDate[]{yearMonth.atDay(1), yearMonth.atEndOfMonth()};
        } catch (DateTimeParseException ex) {
            throw new InvalidPayrollArgumentException(
                    "payroll launch V1 requires payrollPeriodCode in YYYYMM format"
            );
        }
    }

    /**
     * Deja dicho, si los hay, qué conceptos de la reglamentación no alcanza ninguna asignación
     * ({@code backend#110}).
     *
     * <h3>Por qué aquí y no al sembrar ni en un {@code lint} del catálogo</h3>
     *
     * <p>Porque es aquí donde duele. El caso que esto arregla es: alguien añade conceptos, lanza,
     * la corrida contesta 202, termina {@code COMPLETED}, salen los 873 recibos y <b>ni uno solo
     * cambia</b>. El momento en que esa persona va a mirar algo es el de la ejecución, y la
     * ejecución ya tiene dónde decirlo —sus mensajes— y una pantalla que los pinta.
     *
     * <p>Al sembrar no sirve: media reglamentación se escribe desde el diseñador y no pasa por
     * ninguna migración. Como comprobación aparte del catálogo tampoco basta por sí sola: sería
     * un sitio más al que hay que acordarse de ir, y el problema es justamente que nadie sospecha
     * que hay algo que mirar. La consulta queda hecha y reutilizable
     * ({@link UnreachableConceptFinder}), así que ponerla además en un {@code lint} el día que
     * haya uno es una línea.
     *
     * <h3>Avisa y no tumba</h3>
     *
     * <p>{@code WARNING}, no {@code ERROR}, y la ejecución sigue. Un concepto inalcanzable puede
     * ser transitorio —se declara hoy y se asigna mañana—, y parar la nómina de 873 personas por
     * eso sería cambiar un silencio por un portazo.
     *
     * <p>Por lo mismo, si la comprobación misma revienta, revienta ella sola. La expansión falla
     * cuando un operando apunta a un concepto que no está declarado, y esa avería la denuncia el
     * cálculo de la primera unidad que la necesite: que un <b>aviso</b> se lleve por delante una
     * corrida entera sería peor que el silencio que vino a quitar. Queda en el registro, que no
     * es lo mismo que callarse.
     */
    private void avisarDeConceptosInalcanzables(CalculationRun run, RuleSystemMetamodel metamodel) {
        List<String> inalcanzables;
        try {
            inalcanzables = unreachableConceptFinder.unreachableConceptsIn(metamodel);
        } catch (RuntimeException ex) {
            log.warn("[ENGINE] No se pudo comprobar si hay conceptos inalcanzables en {} | {}: {}",
                    metamodel.ruleSystemCode(), ex.getClass().getSimpleName(), ex.getMessage());
            return;
        }

        if (inalcanzables.isEmpty()) {
            return;
        }

        // El texto lleva los primeros y el detalle los lleva todos: `message` es varchar(500) y
        // un catalogo recien montado puede dejar veinte inalcanzables de una vez. Recortar el
        // texto y guardar la lista entera en el JSON deja las dos cosas: un mensaje que se lee y
        // un dato que no miente.
        List<String> primeros = inalcanzables.stream().limit(CONCEPTOS_INALCANZABLES_EN_EL_TEXTO).toList();
        String resto = inalcanzables.size() > primeros.size()
                ? " (y " + (inalcanzables.size() - primeros.size()) + " mas, en el detalle)"
                : "";

        saveRunMessage(run, "UNREACHABLE_CONCEPTS", "WARNING",
                "Estos conceptos de " + metamodel.ruleSystemCode() + " no los alcanza ninguna "
                        + "asignacion, asi que no se han ejecutado: "
                        + String.join(", ", primeros) + resto,
                Map.of(
                        "ruleSystemCode", metamodel.ruleSystemCode(),
                        "conceptCodes", inalcanzables,
                        "totalUnreachable", inalcanzables.size()
                ),
                null);
    }

    private void saveRunMessage(
            CalculationRun run,
            String messageCode,
            String severityCode,
            String message,
            Map<String, ?> details,
            PayrollCalculationUnit unit
    ) {
        calculationRunMessageRepository.save(new CalculationRunMessage(
                null,
                run.id(),
                messageCode,
                severityCode,
                message,
                details == null || details.isEmpty() ? null : toJson(details, "runMessage.details"),
                unit == null ? null : unit.ruleSystemCode(),
                unit == null ? null : unit.employeeTypeCode(),
                unit == null ? null : unit.employeeNumber(),
                unit == null ? null : unit.payrollPeriodCode(),
                unit == null ? null : unit.payrollTypeCode(),
                unit == null ? null : unit.presenceNumber(),
                LocalDateTime.now()
        ));
    }

    private String buildSummaryJson(CalculationRun run) {
        return toJson(Map.of(
                "candidateSemantics", "expanded presence-based calculation units overlapping the payroll month",
                "totalCandidates", run.totalCandidates(),
                "totalEligible", run.totalEligible(),
                "totalClaimed", run.totalClaimed(),
                "totalSkippedNotEligible", run.totalSkippedNotEligible(),
                "totalSkippedAlreadyClaimed", run.totalSkippedAlreadyClaimed(),
                "totalCalculated", run.totalCalculated(),
                "totalNotValid", run.totalNotValid(),
                "totalErrors", run.totalErrors()
        ), "summaryJson");
    }

    private void cleanupClaimsByRunId(Long runId) {
        if (runId == null) {
            return;
        }
        try {
            calculationClaimRepository.deleteByRunId(runId);
        } catch (RuntimeException ignored) {
            // Best-effort cleanup only. Hard crash and housekeeping scenarios remain deferred.
        }
    }

    private String toJson(Object value, String fieldName) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize " + fieldName, ex);
        }
    }

    private String normalizeCode(String value, String fieldName, int maxLength) {
        return normalizeText(value, fieldName, maxLength).toUpperCase();
    }

    /**
     * Texto opcional: ausente es un valor legitimo, demasiado largo no. Un lanzamiento no
     * se cae por no saber quien lo pidio, pero tampoco se trunca en silencio un sujeto que
     * no cabe en la columna.
     */
    private String normalizeOptionalText(String value, String fieldName, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return normalizeText(value, fieldName, maxLength);
    }

    private String normalizeText(String value, String fieldName, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidPayrollArgumentException(fieldName + " is required");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new InvalidPayrollArgumentException(fieldName + " exceeds max length " + maxLength);
        }
        return normalized;
    }
}
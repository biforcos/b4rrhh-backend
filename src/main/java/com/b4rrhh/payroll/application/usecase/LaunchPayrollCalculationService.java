package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.application.port.PayrollLaunchPresenceContext;
import com.b4rrhh.payroll.application.port.PayrollLaunchPresenceLookupPort;
import com.b4rrhh.payroll.application.port.PayrollLaunchWorkerPort;
import com.b4rrhh.payroll.application.port.PayrollLaunchEmployeeContext;
import com.b4rrhh.payroll.domain.exception.InvalidPayrollArgumentException;
import com.b4rrhh.payroll.domain.model.CalculationClaim;
import com.b4rrhh.payroll.domain.model.CalculationRun;
import com.b4rrhh.payroll.domain.model.CalculationRunMessage;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.port.CalculationClaimRepository;
import com.b4rrhh.payroll.domain.port.CalculationRunMessageRepository;
import com.b4rrhh.payroll.domain.port.CalculationRunRepository;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final DateTimeFormatter PAYROLL_PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final CalculationRunRepository calculationRunRepository;
    private final CalculationClaimRepository calculationClaimRepository;
    private final CalculationRunMessageRepository calculationRunMessageRepository;
    private final PayrollRepository payrollRepository;
    private final PayrollLaunchPresenceLookupPort payrollLaunchPresenceLookupPort;
    private final CalculatePayrollUnitUseCase calculatePayrollUnitUseCase;
    private final PayrollLaunchWorkerPort payrollLaunchWorkerPort;
    private final ObjectMapper objectMapper;

    public LaunchPayrollCalculationService(
            CalculationRunRepository calculationRunRepository,
            CalculationClaimRepository calculationClaimRepository,
            CalculationRunMessageRepository calculationRunMessageRepository,
            PayrollRepository payrollRepository,
            PayrollLaunchPresenceLookupPort payrollLaunchPresenceLookupPort,
            CalculatePayrollUnitUseCase calculatePayrollUnitUseCase,
            PayrollLaunchWorkerPort payrollLaunchWorkerPort,
            ObjectMapper objectMapper
    ) {
        this.calculationRunRepository = calculationRunRepository;
        this.calculationClaimRepository = calculationClaimRepository;
        this.calculationRunMessageRepository = calculationRunMessageRepository;
        this.payrollRepository = payrollRepository;
        this.payrollLaunchPresenceLookupPort = payrollLaunchPresenceLookupPort;
        this.calculatePayrollUnitUseCase = calculatePayrollUnitUseCase;
        this.payrollLaunchWorkerPort = payrollLaunchWorkerPort;
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
     */
    private CalculationRun execute(CalculationRun requestedRun, NormalizedLaunch normalizedLaunch) {
        CalculationRun run = requestedRun;
        try {
            run = calculationRunRepository.save(run.withStatus(CalculationRunStatuses.RUNNING).withStartedAt(LocalDateTime.now()));

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

            for (PayrollCalculationUnit unit : units) {
                run = processUnit(run, unit, normalizedLaunch.calculationEngineCode(), normalizedLaunch.calculationEngineVersion());
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
            String calculationEngineVersion
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
                    run.id()
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
            return calculationRunRepository.save(run.incrementTotalSkippedNotEligible());
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
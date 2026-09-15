package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.domain.exception.InvalidPayrollArgumentException;
import com.b4rrhh.payroll.domain.exception.PayrollBusinessKeyConflictException;
import com.b4rrhh.payroll.domain.exception.PayrollCalculationFailedException;
import com.b4rrhh.payroll.domain.exception.PayrollEmployeePresenceNotFoundException;
import com.b4rrhh.payroll.domain.exception.PayrollInvalidStateTransitionException;
import com.b4rrhh.payroll.domain.exception.PayrollNotFoundException;
import com.b4rrhh.payroll.domain.exception.PayrollRecalculationNotAllowedException;
import com.b4rrhh.payroll.domain.exception.PayrollTypeInvalidException;
import com.b4rrhh.payroll.domain.model.CalculationRun;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.port.CalculationRunRepository;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import com.b4rrhh.payroll_engine.metamodel.domain.port.RuleSystemMetamodelRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class RecalculatePayrollService implements RecalculatePayrollUseCase {

    private static final DateTimeFormatter PAYROLL_PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    /**
     * Lo que no se disfraza de fallo de calculo, porque el manejador de /payrolls ya sabe
     * contestarlo con su codigo de estado y su cuerpo: un recibo que no esta, una presencia que no
     * esta, un estado que no admite el recalculo, un argumento invalido, y la unidad elegible a la
     * que le falta un dato de entrada, que tiene su propio codigo desde el backend#85.
     *
     * <p>Que esta lista siga siendo la del manejador no se confia a nadie: lo cruza
     * {@code RecalculateOnlyDisguisesWhatNobodyAnswersTest}. Si manana el manejador aprende a
     * contestar otra excepcion y aqui no se anade, ese test se pone rojo — que es lo unico que
     * impide que un error con respuesta propia acabe saliendo como «el calculo fallo».
     */
    static final List<Class<? extends RuntimeException>> YA_TIENE_RESPUESTA = List.of(
            PayrollNotFoundException.class,
            PayrollEmployeePresenceNotFoundException.class,
            InvalidPayrollArgumentException.class,
            IllegalArgumentException.class,
            PayrollTypeInvalidException.class,
            PayrollInvalidStateTransitionException.class,
            PayrollRecalculationNotAllowedException.class,
            PayrollBusinessKeyConflictException.class,
            PayrollLaunchInputMissingException.class
    );

    /**
     * Como se llama en la ejecucion al encargo de un recalculo. No es ninguno de los tres tipos
     * del lanzamiento: {@code SINGLE_EMPLOYEE} expande a <b>todas</b> las unidades del empleado en
     * el periodo, y un recalculo toca una. Quien lea la ejecucion tiene que poder ver cual.
     */
    static final String SELECTION_TYPE = "SINGLE_CALCULATION_UNIT";

    private final PayrollRepository payrollRepository;
    private final CalculatePayrollUnitUseCase calculatePayrollUnitUseCase;
    private final RuleSystemMetamodelRepository ruleSystemMetamodelRepository;
    private final CalculationRunRepository calculationRunRepository;
    private final ObjectMapper objectMapper;

    public RecalculatePayrollService(
            PayrollRepository payrollRepository,
            CalculatePayrollUnitUseCase calculatePayrollUnitUseCase,
            RuleSystemMetamodelRepository ruleSystemMetamodelRepository,
            CalculationRunRepository calculationRunRepository,
            ObjectMapper objectMapper
    ) {
        this.payrollRepository = payrollRepository;
        this.calculatePayrollUnitUseCase = calculatePayrollUnitUseCase;
        this.ruleSystemMetamodelRepository = ruleSystemMetamodelRepository;
        this.calculationRunRepository = calculationRunRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Payroll recalculate(RecalculatePayrollCommand command) {
        Payroll payroll = payrollRepository.findByBusinessKey(
                command.ruleSystemCode(),
                command.employeeTypeCode(),
                command.employeeNumber(),
                command.payrollPeriodCode(),
                command.payrollTypeCode(),
                command.presenceNumber()
        ).orElseThrow(() -> new PayrollNotFoundException(
                command.ruleSystemCode(), command.employeeTypeCode(), command.employeeNumber(),
                command.payrollPeriodCode(), command.payrollTypeCode(), command.presenceNumber()
        ));

        if (!payroll.canBeRecalculated()) {
            throw new PayrollRecalculationNotAllowedException(
                    command.ruleSystemCode(), command.employeeTypeCode(), command.employeeNumber(),
                    command.payrollPeriodCode(), command.payrollTypeCode(), command.presenceNumber(),
                    payroll.getStatus()
            );
        }

        LocalDate periodStart = parsePeriodStart(command.payrollPeriodCode());
        LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());

        CalculationRun run = abrirEjecucionDeUnaUnidad(command, payroll);

        // Un recalculo puntual tambien es una ejecucion, de una sola unidad: lee su
        // reglamentacion aqui y calcula contra ella. Por eso ve los cambios del grafo que
        // haya habido desde la corrida que produjo el recibo anterior (backend#87).
        //
        // Y si el motor no puede calcular, el fallo sale con forma. El lanzamiento masivo ya
        // hace esto —captura la RuntimeException de la unidad y la guarda como mensaje de
        // ejecucion UNIT_CALCULATION_ERROR—; por esta puerta se iba a la calle y el cliente
        // recibia un 500 que no podia ensenar (#100).
        Payroll recalculado;
        try {
            recalculado = calculatePayrollUnitUseCase.calculate(new CalculatePayrollUnitCommand(
                    command.ruleSystemCode(),
                    command.employeeTypeCode(),
                    command.employeeNumber(),
                    command.payrollPeriodCode(),
                    command.payrollTypeCode(),
                    command.presenceNumber(),
                    periodStart,
                    periodEnd,
                    payroll.getCalculationEngineCode(),
                    payroll.getCalculationEngineVersion(),
                    run.id(),
                    ruleSystemMetamodelRepository.load(command.ruleSystemCode(), periodEnd)
            ));
        } catch (RuntimeException ex) {
            if (YA_TIENE_RESPUESTA.stream().anyMatch(tipo -> tipo.isInstance(ex))) {
                throw ex;
            }
            throw new PayrollCalculationFailedException(ex);
        }

        cerrarEjecucionDeUnaUnidad(run, recalculado);
        return recalculado;
    }

    /**
     * La ejecucion del recalculo, que es de una unidad y por eso no se parece a un lanzamiento.
     *
     * <p>Se abre <b>antes</b> de calcular porque el recibo nuevo nace con su {@code run_id}
     * dentro: es un dato de la unidad, no una anotacion posterior.
     *
     * <p>No pasa por la cola del lanzamiento —{@code PayrollLaunchWorkerPort}, un hilo y de una en
     * una— a proposito. Aquella cola existe para que dos corridas masivas no se peleen por las
     * reservas; hacer esperar el recalculo de un recibo detras de una nomina de mil empleados
     * seria pagar el precio de un problema que no se tiene. Y tampoco toma reserva en
     * {@code calculation_claim}: eso cambiaria lo que una corrida masiva hace con esta unidad, que
     * es una decision aparte y este issue no la pidio.
     */
    private CalculationRun abrirEjecucionDeUnaUnidad(RecalculatePayrollCommand command, Payroll payroll) {
        LocalDateTime ahora = LocalDateTime.now();
        return calculationRunRepository.save(new CalculationRun(
                null,
                command.ruleSystemCode(),
                command.payrollPeriodCode(),
                command.payrollTypeCode(),
                payroll.getCalculationEngineCode(),
                payroll.getCalculationEngineVersion(),
                ahora,
                command.requestedBy(),
                CalculationRunStatuses.RUNNING,
                targetSelectionJson(command),
                // Un candidato, elegible —el recibo estaba NOT_VALID, que es lo unico desde lo que
                // se recalcula— y sin reserva: el 0 de totalClaimed no es un hueco, es que no se
                // tomo ninguna.
                1,
                1,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                ahora,
                null,
                null,
                null,
                null
        ));
    }

    /**
     * Y se cierra con el desenlace de su unica unidad.
     *
     * <p>Solo se cierra cuando hay recibo. Si el calculo falla, la transaccion del recalculo se
     * deshace entera y la ejecucion se va con ella: no queda una fila apuntando a un recibo que no
     * existe. Lo que cuenta el fallo es el 422 del {@code #100}, que lleva el codigo y el mensaje;
     * una ejecucion FAILED de una unidad no la enseñaria nadie, porque no hay ninguna pantalla que
     * liste ejecuciones — a una ejecucion solo se llega por el {@code runId} que te dieron.
     */
    private void cerrarEjecucionDeUnaUnidad(CalculationRun run, Payroll recalculado) {
        boolean invalido = recalculado.getStatus() == PayrollStatus.NOT_VALID;
        CalculationRun contado = invalido ? run.incrementTotalNotValid() : run.incrementTotalCalculated();
        calculationRunRepository.save(contado.withFinishedExecution(
                CalculationRunStatuses.COMPLETED,
                LocalDateTime.now(),
                summaryJson(contado)
        ));
    }

    private String targetSelectionJson(RecalculatePayrollCommand command) {
        try {
            return objectMapper.writeValueAsString(new RecalculationTarget(
                    SELECTION_TYPE,
                    new RecalculationUnit(
                            command.employeeTypeCode(),
                            command.employeeNumber(),
                            command.presenceNumber()
                    )
            ));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize the recalculation target selection", ex);
        }
    }

    private String summaryJson(CalculationRun run) {
        try {
            return objectMapper.writeValueAsString(new RecalculationSummary(
                    run.totalCandidates(),
                    run.totalCalculated(),
                    run.totalNotValid()
            ));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize the recalculation run summary", ex);
        }
    }

    private record RecalculationTarget(String selectionType, RecalculationUnit unit) {
    }

    private record RecalculationUnit(String employeeTypeCode, String employeeNumber, Integer presenceNumber) {
    }

    private record RecalculationSummary(Integer totalCandidates, Integer totalCalculated, Integer totalNotValid) {
    }

    private LocalDate parsePeriodStart(String periodCode) {
        try {
            return YearMonth.parse(periodCode, PAYROLL_PERIOD_FORMATTER).atDay(1);
        } catch (DateTimeParseException ex) {
            throw new InvalidPayrollArgumentException("payrollPeriodCode must be in yyyyMM format, got: " + periodCode);
        }
    }
}

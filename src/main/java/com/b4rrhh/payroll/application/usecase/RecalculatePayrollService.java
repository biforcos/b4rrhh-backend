package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.document.domain.exception.PayslipDocumentNotArchivedException;
import com.b4rrhh.payroll.document.domain.exception.PayslipDocumentStorageUnavailableException;
import com.b4rrhh.payroll.domain.exception.InvalidPayrollArgumentException;
import com.b4rrhh.payroll.domain.exception.PayrollBusinessKeyConflictException;
import com.b4rrhh.payroll.domain.exception.PayrollCalculationFailedException;
import com.b4rrhh.payroll.domain.exception.PayrollEmployeePresenceNotFoundException;
import com.b4rrhh.payroll.domain.exception.PayrollInvalidStateTransitionException;
import com.b4rrhh.payroll.domain.exception.PayrollNotFoundException;
import com.b4rrhh.payroll.domain.exception.PayrollRecalculationNotAllowedException;
import com.b4rrhh.payroll.domain.exception.PayrollTypeInvalidException;
import com.b4rrhh.payroll.domain.exception.PayrollUnitAlreadyClaimedException;
import com.b4rrhh.payroll.domain.model.CalculationClaim;
import com.b4rrhh.payroll.domain.model.CalculationRun;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.domain.port.CalculationClaimRepository;
import com.b4rrhh.payroll.domain.port.CalculationRunRepository;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import com.b4rrhh.payroll_engine.metamodel.domain.port.RuleSystemMetamodelRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
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
     *
     * <p>Las dos del documento ({@code backend#112}) entran por ese cruce y no porque el recalculo
     * pueda producirlas: recalcular no archiva nada, asi que hoy no llegan hasta aqui. La lista no
     * es «lo que el recalculo lanza», es «lo que no se puede disfrazar», y el dia que recalcular
     * toque el almacen —o que otra pieza de este camino lo toque— disfrazar un 503 reintentable de
     * 422 «el calculo fallo» seria exactamente el error que este cruce existe para evitar.
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
            PayrollUnitAlreadyClaimedException.class,
            PayrollLaunchInputMissingException.class,
            PayslipDocumentStorageUnavailableException.class,
            PayslipDocumentNotArchivedException.class
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
    private final CalculationClaimRepository calculationClaimRepository;
    private final ObjectMapper objectMapper;

    public RecalculatePayrollService(
            PayrollRepository payrollRepository,
            CalculatePayrollUnitUseCase calculatePayrollUnitUseCase,
            RuleSystemMetamodelRepository ruleSystemMetamodelRepository,
            CalculationRunRepository calculationRunRepository,
            CalculationClaimRepository calculationClaimRepository,
            ObjectMapper objectMapper
    ) {
        this.payrollRepository = payrollRepository;
        this.calculatePayrollUnitUseCase = calculatePayrollUnitUseCase;
        this.ruleSystemMetamodelRepository = ruleSystemMetamodelRepository;
        this.calculationRunRepository = calculationRunRepository;
        this.calculationClaimRepository = calculationClaimRepository;
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
        CalculationClaim claim = reservarLaUnidad(command, run);
        run = calculationRunRepository.save(run.incrementTotalClaimed());

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

        calculationClaimRepository.deleteById(claim.id());
        cerrarEjecucionDeUnaUnidad(run, recalculado);
        return recalculado;
    }

    /**
     * La reserva, que es lo unico que decide quien llego antes.
     *
     * <p>Hasta el backend#101 el recalculo no la tomaba, y el lanzamiento masivo si: o sea que la
     * reserva se tomaba y no servia de nada, porque el otro camino no la consultaba. Los dos
     * caminos escribian el mismo recibo a la vez. Eso <b>no</b> dejaba recibos mezclados —un
     * calculo reemplaza el recibo entero en vez de editarlo, y {@code uk_payroll_business} admite
     * una fila— pero dejaba al perdedor perdiendo por donde no era.
     *
     * <p>La politica es primero el que llegue, y no se espera: si la unidad esta cogida esto sale
     * con un 409 en vez de quedarse esperando a que termine una nomina de mil empleados. La
     * decision y su motivo estan en el backend#101; lo descartado fue dar prioridad a uno de los
     * dos, que obliga a inventar un concepto de prioridad que no existe en el modelo.
     *
     * <p>Se toma <b>dentro</b> de la transaccion del recalculo, que es lo que le da el alcance
     * justo: dura exactamente lo que dura el calculo y desaparece con el, gane o pierda. No hace
     * falta recuperarla al arrancar como las del lanzamiento, porque no puede sobrevivir a nadie.
     *
     * <p>Y de ahi sale una asimetria que conviene saber, porque es la que se mide en
     * {@code TwoWritersOnOnePayrollIntegrationTest}: esta fila no se ve desde fuera hasta que la
     * transaccion confirma, asi que una corrida masiva que llegue a la misma unidad <b>espera</b>
     * en su propio {@code insert} lo que tarde este calculo —una unidad, no una nomina— en vez de
     * fallar en el acto. Al soltarse consigue la reserva y se encuentra el recibo ya
     * {@code CALCULATED}, que es el otro sitio donde la corrida cuenta la unidad como cogida. Al
     * reves no pasa: la reserva del lanzamiento ya esta confirmada, y esto sale con su 409
     * inmediatamente, que es lo que la decision pedia.
     */
    private CalculationClaim reservarLaUnidad(RecalculatePayrollCommand command, CalculationRun run) {
        try {
            return calculationClaimRepository.save(new CalculationClaim(
                    null,
                    run.id(),
                    command.ruleSystemCode(),
                    command.employeeTypeCode(),
                    command.employeeNumber(),
                    command.payrollPeriodCode(),
                    command.payrollTypeCode(),
                    command.presenceNumber(),
                    LocalDateTime.now(),
                    command.requestedBy()
            ));
        } catch (DataIntegrityViolationException ex) {
            throw new PayrollUnitAlreadyClaimedException(
                    command.ruleSystemCode(), command.employeeTypeCode(), command.employeeNumber(),
                    command.payrollPeriodCode(), command.payrollTypeCode(), command.presenceNumber()
            );
        }
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
     * seria pagar el precio de un problema que no se tiene. Lo que si toma, desde el backend#101,
     * es la reserva de {@code calculation_claim}: ver {@link #reservarLaUnidad}.
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
                // Un candidato y elegible: el recibo estaba NOT_VALID, que es lo unico desde lo
                // que se recalcula. La reserva se cuenta despues, cuando se consigue, porque
                // conseguirla es justo lo que puede no pasar (backend#101).
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

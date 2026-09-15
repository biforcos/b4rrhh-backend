package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.domain.exception.InvalidPayrollArgumentException;
import com.b4rrhh.payroll.domain.exception.PayrollBusinessKeyConflictException;
import com.b4rrhh.payroll.domain.exception.PayrollCalculationFailedException;
import com.b4rrhh.payroll.domain.exception.PayrollEmployeePresenceNotFoundException;
import com.b4rrhh.payroll.domain.exception.PayrollInvalidStateTransitionException;
import com.b4rrhh.payroll.domain.exception.PayrollNotFoundException;
import com.b4rrhh.payroll.domain.exception.PayrollRecalculationNotAllowedException;
import com.b4rrhh.payroll.domain.exception.PayrollTypeInvalidException;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import com.b4rrhh.payroll_engine.metamodel.domain.port.RuleSystemMetamodelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    private final PayrollRepository payrollRepository;
    private final CalculatePayrollUnitUseCase calculatePayrollUnitUseCase;
    private final RuleSystemMetamodelRepository ruleSystemMetamodelRepository;

    public RecalculatePayrollService(
            PayrollRepository payrollRepository,
            CalculatePayrollUnitUseCase calculatePayrollUnitUseCase,
            RuleSystemMetamodelRepository ruleSystemMetamodelRepository
    ) {
        this.payrollRepository = payrollRepository;
        this.calculatePayrollUnitUseCase = calculatePayrollUnitUseCase;
        this.ruleSystemMetamodelRepository = ruleSystemMetamodelRepository;
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

        // Un recalculo puntual tambien es una ejecucion, de una sola unidad: lee su
        // reglamentacion aqui y calcula contra ella. Por eso ve los cambios del grafo que
        // haya habido desde la corrida que produjo el recibo anterior (backend#87).
        //
        // Y si el motor no puede calcular, el fallo sale con forma. El lanzamiento masivo ya
        // hace esto —captura la RuntimeException de la unidad y la guarda como mensaje de
        // ejecucion UNIT_CALCULATION_ERROR—; por esta puerta se iba a la calle y el cliente
        // recibia un 500 que no podia ensenar (#100).
        try {
            return calculatePayrollUnitUseCase.calculate(new CalculatePayrollUnitCommand(
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
                    // El recalculo puntual no nace de un lanzamiento: no hay ejecucion que anotar. No se
                    // arrastra la del recibo anterior, porque no es la que produjo este (backend#62).
                    null,
                    ruleSystemMetamodelRepository.load(command.ruleSystemCode(), periodEnd)
            ));
        } catch (RuntimeException ex) {
            if (YA_TIENE_RESPUESTA.stream().anyMatch(tipo -> tipo.isInstance(ex))) {
                throw ex;
            }
            throw new PayrollCalculationFailedException(ex);
        }
    }

    private LocalDate parsePeriodStart(String periodCode) {
        try {
            return YearMonth.parse(periodCode, PAYROLL_PERIOD_FORMATTER).atDay(1);
        } catch (DateTimeParseException ex) {
            throw new InvalidPayrollArgumentException("payrollPeriodCode must be in yyyyMM format, got: " + periodCode);
        }
    }
}

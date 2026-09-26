package com.b4rrhh.payroll.retro.application.usecase;

import com.b4rrhh.payroll.application.usecase.CalculatePayrollUnitCommand;
import com.b4rrhh.payroll.application.usecase.CalculatePayrollUnitUseCase;
import com.b4rrhh.payroll.retro.domain.model.CurrentCalculation;
import com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodel;
import com.b4rrhh.payroll_engine.metamodel.domain.port.RuleSystemMetamodelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Recalcula el tramo de meses cerrados de un empleado, <b>hacia delante y en orden</b>
 * ({@code backend#131}, ADR-076 §2).
 *
 * <h2>Por que hacia delante, y por que todo el tramo</h2>
 *
 * <p>Porque los meses se leen unos a otros. Unas horas metidas a julio mueven la base de cotizacion de
 * julio, y la base reguladora de agosto <b>lee julio</b> ({@code backend#128}): recalcular solo el mes
 * tocado dejaria agosto diciendo un numero que ya no sale de ninguna parte.
 *
 * <p>Y en orden: si agosto se calculara antes que julio, leeria el julio viejo. El orden no es una
 * preferencia, es la condicion que hace que el resultado sea el mismo que si nunca hubiera habido un
 * error — que es lo unico que un atraso puede prometer.
 *
 * <h2>Las reglas de cada mes son las de aquel mes</h2>
 *
 * <p>El metamodelo se carga <b>una vez por mes</b> y con la fecha de ese mes, no con la del lanzamiento.
 * Eso ya lo sabia hacer el motor ({@code backend#105}); aqui es donde importa hacia atras: un mes de
 * 2025 recalculado hoy coge los tipos de 2025, porque lo que se corrige es lo que aquel mes debio ser.
 *
 * <p>Cargar el metamodelo por mes cuesta una consulta por mes y por empleado. Se paga a proposito: la
 * alternativa —cargarlo una vez con la fecha del lanzamiento— calcularia enero con los tipos de
 * septiembre, y el numero saldria, y estaria mal.
 */
@Service
public class RecalculateClosedPeriodsService implements RecalculateClosedPeriodsUseCase {

    private static final Logger log = LoggerFactory.getLogger(RecalculateClosedPeriodsService.class);
    private static final DateTimeFormatter PERIODO = DateTimeFormatter.ofPattern("yyyyMM");

    private final CalculatePayrollUnitUseCase calculatePayrollUnitUseCase;
    private final RuleSystemMetamodelRepository ruleSystemMetamodelRepository;

    public RecalculateClosedPeriodsService(
            CalculatePayrollUnitUseCase calculatePayrollUnitUseCase,
            RuleSystemMetamodelRepository ruleSystemMetamodelRepository
    ) {
        this.calculatePayrollUnitUseCase = calculatePayrollUnitUseCase;
        this.ruleSystemMetamodelRepository = ruleSystemMetamodelRepository;
    }

    /**
     * Sin {@code @Transactional}, y no por descuido.
     *
     * <p>Cada mes se escribe en la transaccion de su propia unidad ({@code calculateCurrent} la abre).
     * Una transaccion sobre el tramo entero significaria que un fallo en agosto deshace el vigente de
     * julio que ya estaba bien calculado, y el siguiente intento tendria que volver a hacerlo todo.
     * Cada mes vale por si solo: el vigente de julio es cierto aunque agosto no se haya podido calcular.
     */
    @Override
    public RecalculateClosedPeriodsResult recalculate(RecalculateClosedPeriodsCommand command) {
        YearMonth desde = YearMonth.parse(command.fromPeriodCode(), PERIODO);
        YearMonth hasta = YearMonth.parse(command.toPeriodCode(), PERIODO);

        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException(
                    "El tramo de la retro va hacia delante: " + command.fromPeriodCode()
                            + " es posterior a " + command.toPeriodCode());
        }

        List<CurrentCalculation> escritos = new ArrayList<>();
        List<RecalculateClosedPeriodsResult.NotCalculatedMonth> sinCalcular = new ArrayList<>();

        log.info("[RETRO] ▶ Tramo de {} a {} | empleado={} presencia={}",
                command.fromPeriodCode(), command.toPeriodCode(),
                command.employeeNumber(), command.presenceNumber());

        for (YearMonth mes = desde; !mes.isAfter(hasta); mes = mes.plusMonths(1)) {
            String periodo = mes.format(PERIODO);
            LocalDate inicio = mes.atDay(1);
            LocalDate fin = mes.atEndOfMonth();

            // Las reglas de ESE mes, no las del lanzamiento. Es lo que hace que un mes de otro
            // ejercicio se recalcule con sus tipos y no con los de hoy.
            RuleSystemMetamodel metamodelo = ruleSystemMetamodelRepository.load(
                    command.ruleSystemCode(), fin);

            CalculatePayrollUnitUseCase.CurrentCalculationOutcome resultado =
                    calculatePayrollUnitUseCase.calculateCurrent(new CalculatePayrollUnitCommand(
                            command.ruleSystemCode(),
                            command.employeeTypeCode(),
                            command.employeeNumber(),
                            periodo,
                            command.payrollTypeCode(),
                            command.presenceNumber(),
                            inicio,
                            fin,
                            command.calculationEngineCode(),
                            command.calculationEngineVersion(),
                            command.runId(),
                            metamodelo));

            if (resultado.wasCalculated()) {
                escritos.add(resultado.calculation());
            } else {
                sinCalcular.add(new RecalculateClosedPeriodsResult.NotCalculatedMonth(
                        periodo, resultado.notCalculatedReason()));
                log.warn("[RETRO] {} no se ha podido recalcular: {}", periodo, resultado.notCalculatedReason());
            }
        }

        log.info("[RETRO] ✓ Tramo terminado | {} vigentes escritos, {} meses sin calcular",
                escritos.size(), sinCalcular.size());

        return new RecalculateClosedPeriodsResult(List.copyOf(escritos), List.copyOf(sinCalcular));
    }
}

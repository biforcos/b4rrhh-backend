package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.payroll.application.port.PreviousPeriodContributionBase;
import com.b4rrhh.payroll.application.port.PreviousPeriodContributionBaseLookupPort;
import com.b4rrhh.payroll.application.port.PreviousPeriodSource;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.retro.infrastructure.persistence.SpringDataCurrentCalculationRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lee la base de contingencias comunes del mes anterior, y solo de recibos cerrados
 * ({@code backend#128}, ADR-074).
 *
 * <h2>Dos preguntas y no una</h2>
 *
 * <p>Primero <b>que hay</b> —en que estado esta cada recibo que el empleado tiene de aquel mes— y
 * despues, solo si esta cerrado, <b>cuanto</b>. Se podria hacer en una consulta, y seria peor: la
 * respuesta «hay uno y no esta cerrado» es la que decide que este mes no se calcule, asi que tiene
 * que llegar como respuesta y no como la ausencia de una.
 *
 * <p>Un recibo {@code CALCULATED} del mes anterior <b>no es «nada»</b>. Es un numero que todavia
 * puede cambiar, y leerlo seria calcular una prestacion sobre una base que manana es otra. Por eso la
 * primera consulta no filtra por estado: si filtrara, el caso se volveria indistinguible de no tener
 * recibo, que es exactamente el que se disfrazaria.
 *
 * <h2>El concepto que se lee</h2>
 *
 * <p>{@code B_CC}, la base de contingencias comunes despues de topes, que es la que la ley llama base
 * de cotizacion del mes. Se lee de la <b>linea del recibo</b> y no de los pasos de calculo: el recibo
 * es el documento (ADR-062), y «la base de cotizacion de agosto» es lo que el recibo de agosto dice
 * que fue.
 *
 * <h2>Y en modo retro, del vigente</h2>
 *
 * <p>Desde el {@code backend#131} hay dos fuentes y una sola de ellas se usa en cada llamada
 * ({@code PreviousPeriodSource}). En una retro se recalcula hacia delante desde el mes mas antiguo
 * tocado, asi que cuando le toca a agosto <b>el vigente de julio ya existe</b> y es lo que julio vale
 * ahora; leer el recibo de julio seria calcular agosto con el julio que ya sabemos que ha cambiado.
 *
 * <p>Y en modo retro no hay tercera respuesta que valga {@code NOT_DEFINITIVE} por un vigente: un
 * vigente no tiene estados. Si hay vigente, se lee; si no hay vigente, la pregunta vuelve a ser la de
 * siempre sobre el recibo, con sus tres respuestas intactas — porque si julio no tiene vigente es que
 * julio no entraba en el tramo, y entonces su recibo cerrado <b>es</b> lo que julio vale.
 */
@Component
public class PreviousPeriodContributionBaseLookupAdapter implements PreviousPeriodContributionBaseLookupPort {

    /** La base de contingencias comunes tras topes, que es la del modelo oficial (V148). */
    static final String CONTRIBUTION_BASE_CONCEPT = "B_CC";

    private final SpringDataPayrollRepository payrollRepository;
    private final SpringDataCurrentCalculationRepository currentCalculationRepository;

    public PreviousPeriodContributionBaseLookupAdapter(
            SpringDataPayrollRepository payrollRepository,
            SpringDataCurrentCalculationRepository currentCalculationRepository
    ) {
        this.payrollRepository = payrollRepository;
        this.currentCalculationRepository = currentCalculationRepository;
    }

    @Override
    public PreviousPeriodContributionBase findByEmployeeAndPeriod(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String payrollTypeCode,
            String previousPeriodCode,
            PreviousPeriodSource source
    ) {
        if (source == PreviousPeriodSource.CURRENT_CALCULATION_FIRST) {
            PreviousPeriodContributionBase delVigente = leerDelVigente(
                    ruleSystemCode, employeeTypeCode, employeeNumber, payrollTypeCode, previousPeriodCode);
            if (delVigente != null) {
                return delVigente;
            }
            // No hay vigente de aquel mes: no entraba en el tramo de la retro, asi que su recibo
            // cerrado es lo que vale. Se sigue por el camino de siempre, con sus tres respuestas.
        }

        List<PayrollStatus> estados = payrollRepository.findStatusesByEmployeeAndPeriod(
                ruleSystemCode, employeeTypeCode, employeeNumber, previousPeriodCode, payrollTypeCode);

        if (estados.isEmpty()) {
            return PreviousPeriodContributionBase.none();
        }

        // Todos, y no «alguno»: con dos recibos del mismo mes —cese y readmision— la base del mes es
        // la suma de los dos, asi que si uno de ellos todavia puede cambiar, la suma tambien.
        if (!estados.stream().allMatch(estado -> estado == PayrollStatus.DEFINITIVE)) {
            return PreviousPeriodContributionBase.notDefinitive();
        }

        BigDecimal base = payrollRepository.sumDefinitiveConceptAmount(
                ruleSystemCode, employeeTypeCode, employeeNumber, previousPeriodCode, payrollTypeCode,
                CONTRIBUTION_BASE_CONCEPT);

        // Cerrado y sin linea de base: la regla del cero no imprime una linea que valga cero
        // (backend#104), asi que «no hay linea» es «la base fue cero». No es un hueco.
        return PreviousPeriodContributionBase.definitive(
                base == null ? BigDecimal.ZERO : base);
    }

    /**
     * La base del <b>vigente</b> de aquel mes, o {@code null} si no hay vigente ({@code backend#131}).
     *
     * <p>Dos preguntas y no una, por lo mismo que arriba: primero <b>si hay</b> vigente y despues
     * <b>cuanto</b>. Si se hiciera en una, un vigente cuya base valga cero —la regla del cero no
     * imprime una linea que valga cero— seria indistinguible de no tener vigente, y la retro de agosto
     * se calcularia con el recibo viejo de julio sin que nada lo dijera.
     */
    private PreviousPeriodContributionBase leerDelVigente(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String payrollTypeCode,
            String previousPeriodCode
    ) {
        long cuantos = currentCalculationRepository.countByEmployeeAndPeriod(
                ruleSystemCode, employeeTypeCode, employeeNumber, previousPeriodCode, payrollTypeCode);
        if (cuantos == 0) {
            return null;
        }

        BigDecimal base = currentCalculationRepository.sumConceptAmount(
                ruleSystemCode, employeeTypeCode, employeeNumber, previousPeriodCode, payrollTypeCode,
                CONTRIBUTION_BASE_CONCEPT);

        // Un vigente NO tiene estados: existe o no existe. Asi que la respuesta es la primera de las
        // tres —hay un numero y se lee— y no hay forma de que sea NOT_DEFINITIVE.
        return PreviousPeriodContributionBase.definitive(
                base == null ? BigDecimal.ZERO : base);
    }
}

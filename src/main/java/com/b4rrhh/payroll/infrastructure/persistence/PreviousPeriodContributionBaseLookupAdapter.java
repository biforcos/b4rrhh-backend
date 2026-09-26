package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.payroll.application.port.PreviousPeriodContributionBase;
import com.b4rrhh.payroll.application.port.PreviousPeriodContributionBaseLookupPort;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
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
 */
@Component
public class PreviousPeriodContributionBaseLookupAdapter implements PreviousPeriodContributionBaseLookupPort {

    /** La base de contingencias comunes tras topes, que es la del modelo oficial (V148). */
    static final String CONTRIBUTION_BASE_CONCEPT = "B_CC";

    private final SpringDataPayrollRepository payrollRepository;

    public PreviousPeriodContributionBaseLookupAdapter(SpringDataPayrollRepository payrollRepository) {
        this.payrollRepository = payrollRepository;
    }

    @Override
    public PreviousPeriodContributionBase findByEmployeeAndPeriod(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String payrollTypeCode,
            String previousPeriodCode
    ) {
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
}

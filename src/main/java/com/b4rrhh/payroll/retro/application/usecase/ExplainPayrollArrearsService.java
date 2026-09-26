package com.b4rrhh.payroll.retro.application.usecase;

import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollConcept;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import com.b4rrhh.payroll.infrastructure.persistence.SpringDataPayrollRepository;
import com.b4rrhh.payroll.retro.domain.model.CurrentCalculation;
import com.b4rrhh.payroll.retro.domain.model.CurrentCalculationConcept;
import com.b4rrhh.payroll.retro.domain.port.CurrentCalculationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Los tres números de cada línea de atraso de un recibo ({@code backend#134}).
 *
 * <p>Se lee del recibo y del vigente, y no se recalcula nada: la explicación es una <b>lectura</b>. Si
 * recalculara, podría decir una cosa distinta de la que la línea dice, y entonces la explicación sería
 * otra opinión en vez de la del documento.
 */
@Service
public class ExplainPayrollArrearsService implements ExplainPayrollArrearsUseCase {

    private final PayrollRepository payrollRepository;
    private final CurrentCalculationRepository currentCalculations;
    private final SpringDataPayrollRepository paidBreakdown;

    public ExplainPayrollArrearsService(
            PayrollRepository payrollRepository,
            CurrentCalculationRepository currentCalculations,
            SpringDataPayrollRepository paidBreakdown
    ) {
        this.payrollRepository = payrollRepository;
        this.currentCalculations = currentCalculations;
        this.paidBreakdown = paidBreakdown;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArrearExplanation> explain(ExplainPayrollArrearsCommand command) {
        Optional<Payroll> recibo = payrollRepository.findByBusinessKey(
                command.ruleSystemCode(),
                command.employeeTypeCode(),
                command.employeeNumber(),
                command.payrollPeriodCode(),
                command.payrollTypeCode(),
                command.presenceNumber());
        if (recibo.isEmpty()) {
            return List.of();
        }

        List<ArrearExplanation> explicaciones = new ArrayList<>();
        for (PayrollConcept linea : recibo.get().getConcepts()) {
            // Una linea de atraso es la que pertenece a OTRO mes. Toda linea lleva su periodo desde
            // siempre; lo que el backend#133 anadio es que pueda ser distinto del del recibo.
            if (linea.getOriginPeriodCode() == null
                    || linea.getOriginPeriodCode().equals(command.payrollPeriodCode())) {
                continue;
            }
            explicaciones.add(explicar(command, linea));
        }
        return List.copyOf(explicaciones);
    }

    private ArrearExplanation explicar(ExplainPayrollArrearsCommand command, PayrollConcept linea) {
        String origen = linea.getOriginPeriodCode();

        Optional<CurrentCalculation> vigente = currentCalculations.findByBusinessKey(
                command.ruleSystemCode(), command.employeeTypeCode(), command.employeeNumber(),
                origen, command.payrollTypeCode(), command.presenceNumber());

        BigDecimal valeHoy = vigente
                .map(v -> v.getConcepts().stream()
                        .filter(c -> c.conceptCode().equals(linea.getConceptCode()))
                        .map(CurrentCalculationConcept::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .orElse(BigDecimal.ZERO);

        List<PaidIn> desglose = new ArrayList<>();
        BigDecimal pagado = BigDecimal.ZERO;
        // Sin el propio recibo: lo que hace falta es lo pagado ANTES de esta linea. Con el dentro, un
        // recibo ya cerrado se contaria a si mismo y los tres numeros no sumarian su importe.
        for (Object[] fila : paidBreakdown.sumPaidByPayingPeriodForPeriod(
                command.ruleSystemCode(), command.employeeTypeCode(), command.employeeNumber(),
                command.payrollTypeCode(), origen, command.payrollPeriodCode())) {
            String periodoQuePago = (String) fila[0];
            String concepto = (String) fila[1];
            BigDecimal importe = (BigDecimal) fila[2];
            if (!concepto.equals(linea.getConceptCode())) {
                continue;
            }
            desglose.add(new PaidIn(periodoQuePago, importe));
            pagado = pagado.add(importe);
        }

        return new ArrearExplanation(
                origen,
                linea.getConceptCode(),
                linea.getConceptLabel(),
                linea.getAmount(),
                valeHoy,
                vigente.map(CurrentCalculation::getCalculatedAt).orElse(null),
                pagado,
                List.copyOf(desglose),
                valeHoy.subtract(pagado));
    }
}

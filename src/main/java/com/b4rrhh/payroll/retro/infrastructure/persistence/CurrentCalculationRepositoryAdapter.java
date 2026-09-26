package com.b4rrhh.payroll.retro.infrastructure.persistence;

import com.b4rrhh.payroll.retro.domain.model.CurrentCalculation;
import com.b4rrhh.payroll.retro.domain.model.CurrentCalculationConcept;
import com.b4rrhh.payroll.retro.domain.port.CurrentCalculationRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Guarda el vigente <b>pisando</b> el que hubiera ({@code backend#131}).
 *
 * <p>Pisar y no acumular no es una comodidad: el vigente responde «cuanto vale este mes hoy», y dos
 * respuestas a esa pregunta serian dos numeros sin forma de saber cual manda. La historia son los
 * recibos, cada linea de atraso guardada en su mes.
 */
@Component
public class CurrentCalculationRepositoryAdapter implements CurrentCalculationRepository {

    private final SpringDataCurrentCalculationRepository jpa;

    public CurrentCalculationRepositoryAdapter(SpringDataCurrentCalculationRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public CurrentCalculation save(CurrentCalculation calculation) {
        CurrentCalculationEntity entity = jpa
                .findByRuleSystemCodeAndEmployeeTypeCodeAndEmployeeNumberAndPayrollPeriodCodeAndPayrollTypeCodeAndPresenceNumber(
                        calculation.getRuleSystemCode(),
                        calculation.getEmployeeTypeCode(),
                        calculation.getEmployeeNumber(),
                        calculation.getPayrollPeriodCode(),
                        calculation.getPayrollTypeCode(),
                        calculation.getPresenceNumber())
                .orElseGet(CurrentCalculationEntity::new);

        entity.setRuleSystemCode(calculation.getRuleSystemCode());
        entity.setEmployeeTypeCode(calculation.getEmployeeTypeCode());
        entity.setEmployeeNumber(calculation.getEmployeeNumber());
        entity.setPayrollPeriodCode(calculation.getPayrollPeriodCode());
        entity.setPayrollTypeCode(calculation.getPayrollTypeCode());
        entity.setPresenceNumber(calculation.getPresenceNumber());
        entity.setCalculatedAt(calculation.getCalculatedAt());
        entity.setRunId(calculation.getRunId());
        entity.replaceConcepts(calculation.getConcepts().stream()
                .map(CurrentCalculationRepositoryAdapter::aEntidad).toList());

        return aDominio(jpa.save(entity));
    }

    @Override
    public Optional<CurrentCalculation> findByBusinessKey(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String payrollPeriodCode,
            String payrollTypeCode,
            Integer presenceNumber
    ) {
        return jpa
                .findByRuleSystemCodeAndEmployeeTypeCodeAndEmployeeNumberAndPayrollPeriodCodeAndPayrollTypeCodeAndPresenceNumber(
                        ruleSystemCode, employeeTypeCode, employeeNumber,
                        payrollPeriodCode, payrollTypeCode, presenceNumber)
                .map(CurrentCalculationRepositoryAdapter::aDominio);
    }

    private static CurrentCalculationConceptEntity aEntidad(CurrentCalculationConcept c) {
        CurrentCalculationConceptEntity e = new CurrentCalculationConceptEntity();
        e.setLineNumber(c.lineNumber());
        e.setConceptCode(c.conceptCode());
        e.setConceptMnemonic(c.conceptMnemonic());
        e.setConceptLabel(c.conceptLabel());
        e.setAmount(c.amount());
        e.setQuantity(c.quantity());
        e.setRate(c.rate());
        e.setConceptNatureCode(c.conceptNatureCode());
        e.setDisplayOrder(c.displayOrder());
        e.setPayslipSectionCode(c.payslipSectionCode());
        e.setPayslipSubsectionCode(c.payslipSubsectionCode());
        return e;
    }

    private static CurrentCalculation aDominio(CurrentCalculationEntity e) {
        List<CurrentCalculationConcept> conceptos = e.getConcepts().stream()
                .map(c -> new CurrentCalculationConcept(
                        c.getLineNumber(),
                        c.getConceptCode(),
                        c.getConceptMnemonic(),
                        c.getConceptLabel(),
                        c.getAmount(),
                        c.getQuantity(),
                        c.getRate(),
                        c.getConceptNatureCode(),
                        c.getDisplayOrder(),
                        c.getPayslipSectionCode(),
                        c.getPayslipSubsectionCode()))
                .toList();
        return CurrentCalculation.rehydrate(
                e.getId(),
                e.getRuleSystemCode(),
                e.getEmployeeTypeCode(),
                e.getEmployeeNumber(),
                e.getPayrollPeriodCode(),
                e.getPayrollTypeCode(),
                e.getPresenceNumber(),
                e.getCalculatedAt(),
                e.getRunId(),
                conceptos);
    }
}

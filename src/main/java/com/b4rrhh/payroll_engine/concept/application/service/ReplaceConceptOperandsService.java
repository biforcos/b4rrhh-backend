package com.b4rrhh.payroll_engine.concept.application.service;

import com.b4rrhh.payroll_engine.concept.application.usecase.ReplaceConceptOperandsCommand;
import com.b4rrhh.payroll_engine.concept.application.usecase.ReplaceConceptOperandsUseCase;
import com.b4rrhh.payroll_engine.concept.domain.exception.PayrollConceptNotFoundException;
import com.b4rrhh.payroll_engine.concept.domain.model.OperandScopeInvariant;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptOperand;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptOperandRepository;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptRepository;
import com.b4rrhh.payroll_engine.object.domain.exception.PayrollObjectNotFoundException;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObjectTypeCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Replaces the operand definition of a concept atomically: every existing operand row
 * for the target concept is removed and the supplied items are persisted in order.
 *
 * <p>Validation rules enforced by this service, all of them before anything is deleted:
 * <ul>
 *   <li>The target concept must exist (404 otherwise).</li>
 *   <li>Every {@code sourceObjectCode} must resolve to an existing concept in the same
 *       rule system (the operand domain model itself enforces both target and source
 *       being CONCEPT-typed).</li>
 *   <li>Source and target cannot be the same concept (enforced by the domain model).</li>
 *   <li>No operand crosses from SEGMENT to PERIOD: a PERIOD target cannot read a SEGMENT
 *       source ({@link OperandScopeInvariant}, ADR-058).</li>
 * </ul>
 */
@Service
public class ReplaceConceptOperandsService implements ReplaceConceptOperandsUseCase {

    private final PayrollConceptOperandRepository operandRepository;
    private final PayrollConceptRepository conceptRepository;

    public ReplaceConceptOperandsService(
            PayrollConceptOperandRepository operandRepository,
            PayrollConceptRepository conceptRepository
    ) {
        this.operandRepository = operandRepository;
        this.conceptRepository = conceptRepository;
    }

    @Override
    @Transactional
    public List<PayrollConceptOperand> replace(ReplaceConceptOperandsCommand command) {
        String ruleSystemCode = command.ruleSystemCode();
        String conceptCode = command.conceptCode();

        PayrollConcept target = conceptRepository
                .findByBusinessKey(ruleSystemCode, conceptCode)
                .orElseThrow(() -> new PayrollConceptNotFoundException(ruleSystemCode, conceptCode));

        List<ReplaceConceptOperandsCommand.Item> items =
                command.items() == null ? List.of() : command.items();
        Map<String, PayrollConcept> sources = resolveSources(ruleSystemCode, items);
        for (ReplaceConceptOperandsCommand.Item item : items) {
            OperandScopeInvariant.check(target, item.operandRole(), sources.get(item.sourceObjectCode()));
        }

        operandRepository.deleteAllByRuleSystemCodeAndConceptCode(ruleSystemCode, conceptCode);

        List<PayrollConceptOperand> persisted = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (ReplaceConceptOperandsCommand.Item item : items) {
            PayrollConceptOperand operand = new PayrollConceptOperand(
                    null,
                    target.getObject(),
                    item.operandRole(),
                    sources.get(item.sourceObjectCode()).getObject(),
                    now,
                    now
            );
            persisted.add(operandRepository.save(operand));
        }
        return persisted;
    }

    private Map<String, PayrollConcept> resolveSources(
            String ruleSystemCode, List<ReplaceConceptOperandsCommand.Item> items) {
        Set<String> codes = items.stream()
                .map(ReplaceConceptOperandsCommand.Item::sourceObjectCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, PayrollConcept> found = conceptRepository.findAllByCodes(ruleSystemCode, codes)
                .stream()
                .collect(Collectors.toMap(PayrollConcept::getConceptCode, Function.identity()));
        for (String code : codes) {
            if (!found.containsKey(code)) {
                throw new PayrollObjectNotFoundException(
                        ruleSystemCode, PayrollObjectTypeCode.CONCEPT.name(), code);
            }
        }
        return found;
    }
}

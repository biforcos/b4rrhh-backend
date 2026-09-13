package com.b4rrhh.payroll.application.service;

import com.b4rrhh.payroll.basesalary.domain.PayrollObjectBindingLookupPort;
import com.b4rrhh.payroll.basesalary.domain.PayrollTableRowLookupPort;
import com.b4rrhh.payroll_engine.concept.domain.model.CalculationType;
import com.b4rrhh.payroll_engine.concept.domain.model.OperandRole;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptFeedRelation;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptOperand;
import com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PayrollConceptGraphCalculatorService implements PayrollConceptGraphCalculator {

    private static final String BINDING_OWNER_TYPE = "AGREEMENT";

    private final PayrollObjectBindingLookupPort bindingLookup;
    private final PayrollTableRowLookupPort tableRowLookup;

    public PayrollConceptGraphCalculatorService(
            PayrollObjectBindingLookupPort bindingLookup,
            PayrollTableRowLookupPort tableRowLookup
    ) {
        this.bindingLookup = bindingLookup;
        this.tableRowLookup = tableRowLookup;
    }

    @Override
        public PayrollConceptExecutionResult calculateConceptResult(
                        String conceptCode,
                        PayrollConceptExecutionContext context,
                        RuleSystemMetamodel metamodel
        ) {
                metamodel.requireSameRuleSystem(context.ruleSystemCode());
                return calculateConceptResult(conceptCode, context, metamodel, new HashMap<>());
    }

        private PayrollConceptExecutionResult calculateConceptResult(
            String conceptCode,
            PayrollConceptExecutionContext context,
                        RuleSystemMetamodel metamodel,
                        Map<String, PayrollConceptExecutionResult> memo
    ) {
                PayrollConceptExecutionResult cached = memo.get(conceptCode);
        if (cached != null) {
            return cached;
        }

        PayrollConcept concept = metamodel.findConcept(conceptCode)
                .orElseThrow(() -> new IllegalStateException("Configuration error: Payroll concept not found: " + conceptCode));

                PayrollConceptExecutionResult result = switch (concept.getCalculationType()) {
                        case DIRECT_AMOUNT -> calculateDirectAmount(concept, context, metamodel, memo);
                        case RATE_BY_QUANTITY -> calculateRateByQuantity(conceptCode, context, metamodel, memo);
            default -> throw new IllegalStateException(
                    "Configuration error: Unsupported calculation type " + concept.getCalculationType()
                            + " for concept " + conceptCode);
        };

        memo.put(conceptCode, result);
        return result;
    }

    private PayrollConceptExecutionResult calculateDirectAmount(
            PayrollConcept concept,
            PayrollConceptExecutionContext context,
            RuleSystemMetamodel metamodel,
            Map<String, PayrollConceptExecutionResult> memo
    ) {
        Long conceptObjectId = concept.getObject().getId();
        if (conceptObjectId == null) {
            throw new IllegalStateException("Configuration error: Concept object id is required for DIRECT_AMOUNT resolution");
        }

        List<PayrollConceptFeedRelation> activeRelations = metamodel.activeFeedsOf(conceptObjectId);

        if (activeRelations.isEmpty()) {
            throw new IllegalStateException(
                    "Configuration error: Missing active feed relation for DIRECT_AMOUNT concept "
                            + concept.getObject().getObjectCode());
        }
        if (activeRelations.size() > 1) {
            throw new IllegalStateException(
                    "Configuration error: Multiple active feed relations for DIRECT_AMOUNT concept "
                            + concept.getObject().getObjectCode());
        }

        PayrollConceptFeedRelation relation = activeRelations.getFirst();
        return switch (relation.getSourceObject().getObjectTypeCode()) {
            case CONCEPT -> calculateConceptResult(relation.getSourceObject().getObjectCode(), context, metamodel, memo);
            case CONSTANT -> directAmountResult(
                    concept.getObject().getObjectCode(),
                    resolveConstantValue(relation, concept.getObject().getObjectCode())
            );
            case TABLE -> directAmountResult(
                    concept.getObject().getObjectCode(),
                    resolveTableValue(relation.getSourceObject().getObjectCode(), context)
            );
            default -> throw new IllegalStateException(
                    "Configuration error: Unsupported DIRECT_AMOUNT source type "
                            + relation.getSourceObject().getObjectTypeCode()
                            + " for concept " + concept.getObject().getObjectCode());
        };
    }

    private BigDecimal resolveConstantValue(PayrollConceptFeedRelation relation, String conceptCode) {
        if (relation.getFeedValue() == null) {
            throw new IllegalStateException(
                    "Configuration error: Missing feedValue for CONSTANT source in concept " + conceptCode);
        }
        return relation.getFeedValue();
    }

    private BigDecimal resolveTableValue(
            String bindingRoleCode,
            PayrollConceptExecutionContext context
    ) {
        String tableCode = bindingLookup.resolveBoundObjectCode(
                context.ruleSystemCode(),
                BINDING_OWNER_TYPE,
                context.agreementCode(),
                bindingRoleCode
        ).orElseThrow(() -> new IllegalStateException(
                "Configuration error: No table binding found for agreement "
                        + context.agreementCode() + " and role " + bindingRoleCode
        ));

        return tableRowLookup.resolveDailyValue(
                context.ruleSystemCode(),
                tableCode,
                context.categoryCode(),
                context.referenceDate()
        ).orElseThrow(() -> new IllegalStateException(
                "Configuration error: No active table row found for table "
                        + tableCode + ", category " + context.categoryCode()
                        + " and date " + context.referenceDate()
        ));
    }

        private PayrollConceptExecutionResult directAmountResult(String conceptCode, BigDecimal amount) {
                return new PayrollConceptExecutionResult(conceptCode, amount, null, null);
        }

        private PayrollConceptExecutionResult calculateRateByQuantity(
            String conceptCode,
            PayrollConceptExecutionContext context,
                        RuleSystemMetamodel metamodel,
                        Map<String, PayrollConceptExecutionResult> memo
    ) {
        List<PayrollConceptOperand> operands = metamodel.operandsOf(conceptCode);
        PayrollConceptOperand quantityOperand = findSingleOperand(conceptCode, operands, OperandRole.QUANTITY);
        PayrollConceptOperand rateOperand = findSingleOperand(conceptCode, operands, OperandRole.RATE);

                BigDecimal quantity = calculateConceptResult(quantityOperand.getSourceObject().getObjectCode(), context, metamodel, memo).amount();
                BigDecimal rate = calculateConceptResult(rateOperand.getSourceObject().getObjectCode(), context, metamodel, memo).amount();

                BigDecimal result = quantity.multiply(rate);
                if (result.scale() > 6) {
                        result = result.setScale(6, RoundingMode.HALF_UP);
                }

                return new PayrollConceptExecutionResult(
                                conceptCode,
                                result.stripTrailingZeros(),
                                quantity,
                                rate
                );
    }

    private PayrollConceptOperand findSingleOperand(
            String conceptCode,
            List<PayrollConceptOperand> operands,
            OperandRole role
    ) {
        List<PayrollConceptOperand> matching = operands.stream()
                .filter(operand -> operand.getOperandRole() == role)
                .toList();
        if (matching.isEmpty()) {
            throw new IllegalStateException(
                    "Configuration error: Missing operand " + role + " for concept " + conceptCode);
        }
        if (matching.size() > 1) {
            throw new IllegalStateException(
                    "Configuration error: Duplicate operand " + role + " for concept " + conceptCode);
        }
        return matching.getFirst();
    }
}
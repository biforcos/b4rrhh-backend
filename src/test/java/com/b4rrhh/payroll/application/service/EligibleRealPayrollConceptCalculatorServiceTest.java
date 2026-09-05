package com.b4rrhh.payroll.application.service;

import com.b4rrhh.payroll.basesalary.domain.PayrollObjectBindingLookupPort;
import com.b4rrhh.payroll.basesalary.domain.PayrollTableRowLookupPort;
import com.b4rrhh.payroll_engine.concept.domain.model.CalculationType;
import com.b4rrhh.payroll_engine.concept.domain.model.ExecutionScope;
import com.b4rrhh.payroll_engine.concept.domain.model.FeedMode;
import com.b4rrhh.payroll_engine.concept.domain.model.FunctionalNature;
import com.b4rrhh.payroll_engine.concept.domain.model.OperandRole;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptFeedRelation;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptOperand;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptFeedRelationRepository;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptOperandRepository;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptRepository;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObject;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObjectTypeCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollConceptGraphCalculatorServiceTest {

    @Mock
    private PayrollConceptRepository conceptRepository;
    @Mock
    private PayrollConceptOperandRepository operandRepository;
    @Mock
    private PayrollConceptFeedRelationRepository feedRelationRepository;
    @Mock
    private PayrollObjectBindingLookupPort bindingLookup;
    @Mock
    private PayrollTableRowLookupPort tableRowLookup;

    @Test
    void calculateConcept_returnsBaseSalaryFromFixedDaysAndDailyPrice() {
        PayrollConceptGraphCalculatorService service = new PayrollConceptGraphCalculatorService(
                conceptRepository,
                operandRepository,
                feedRelationRepository,
                bindingLookup,
                tableRowLookup
        );

        PayrollConceptExecutionContext context = new PayrollConceptExecutionContext(
                "ESP",
                "99002405011982",
                "99002405-G2",
                LocalDate.of(2025, 1, 31)
        );

        when(conceptRepository.findByBusinessKey("ESP", "101")).thenReturn(Optional.of(concept(101L, "ESP", "101", CalculationType.RATE_BY_QUANTITY)));
        when(conceptRepository.findByBusinessKey("ESP", "D01")).thenReturn(Optional.of(concept(102L, "ESP", "D01", CalculationType.DIRECT_AMOUNT)));
        when(conceptRepository.findByBusinessKey("ESP", "P01")).thenReturn(Optional.of(concept(103L, "ESP", "P01", CalculationType.DIRECT_AMOUNT)));
        when(operandRepository.findByTarget("ESP", "101")).thenReturn(List.of(
                operand("ESP", "101", OperandRole.QUANTITY, "D01"),
                operand("ESP", "101", OperandRole.RATE, "P01")
        ));
        when(feedRelationRepository.findActiveByTargetObjectId(102L, LocalDate.of(2025, 1, 31)))
                .thenReturn(List.of(constantSourceRelation("ESP", "D01_FIXED_30", "D01", new BigDecimal("30"))));
        when(feedRelationRepository.findActiveByTargetObjectId(103L, LocalDate.of(2025, 1, 31)))
                .thenReturn(List.of(tableSourceRelation("ESP", "P01_DAILY_AMOUNT_TABLE", "P01")));
        when(bindingLookup.resolveBoundObjectCode("ESP", "AGREEMENT", "99002405011982", "P01_DAILY_AMOUNT_TABLE"))
                .thenReturn(Optional.of("P01_99002405011982"));
        when(tableRowLookup.resolveDailyValue("ESP", "P01_99002405011982", "99002405-G2", LocalDate.of(2025, 1, 31)))
                .thenReturn(Optional.of(new BigDecimal("47.50")));

        PayrollConceptExecutionResult concept101 = service.calculateConceptResult("101", context);

        assertEquals(0, new BigDecimal("1425.00").compareTo(concept101.amount()));
        assertEquals(0, new BigDecimal("30").compareTo(concept101.quantity()));
        assertEquals(0, new BigDecimal("47.50").compareTo(concept101.rate()));
    }

        private PayrollConcept concept(Long objectId, String ruleSystemCode, String conceptCode, CalculationType calculationType) {
        return new PayrollConcept(
                                new PayrollObject(objectId, ruleSystemCode, PayrollObjectTypeCode.CONCEPT, conceptCode, null, null),
                conceptCode,
                calculationType,
                FunctionalNature.EARNING,
                conceptCode,
                ExecutionScope.PERIOD,
                null,
                null
        );
    }

    private PayrollConceptOperand operand(String ruleSystemCode, String targetCode, OperandRole role, String sourceCode) {
        return new PayrollConceptOperand(
                null,
                new PayrollObject(null, ruleSystemCode, PayrollObjectTypeCode.CONCEPT, targetCode, null, null),
                role,
                new PayrollObject(1L, ruleSystemCode, PayrollObjectTypeCode.CONCEPT, sourceCode, null, null),
                null,
                null
        );
    }

    private PayrollConceptFeedRelation constantSourceRelation(
            String ruleSystemCode,
            String sourceCode,
            String targetCode,
            BigDecimal value
    ) {
        return new PayrollConceptFeedRelation(
                null,
                new PayrollObject(1001L, ruleSystemCode, PayrollObjectTypeCode.CONSTANT, sourceCode, null, null),
                new PayrollObject(102L, ruleSystemCode, PayrollObjectTypeCode.CONCEPT, targetCode, null, null),
                FeedMode.FEED_BY_SOURCE,
                value,
                false,
                LocalDate.of(2025, 1, 1),
                null,
                null,
                null
        );
    }

    private PayrollConceptFeedRelation tableSourceRelation(
            String ruleSystemCode,
            String sourceCode,
            String targetCode
    ) {
        return new PayrollConceptFeedRelation(
                null,
                new PayrollObject(1002L, ruleSystemCode, PayrollObjectTypeCode.TABLE, sourceCode, null, null),
                new PayrollObject(103L, ruleSystemCode, PayrollObjectTypeCode.CONCEPT, targetCode, null, null),
                FeedMode.FEED_BY_SOURCE,
                null,
                false,
                LocalDate.of(2025, 1, 1),
                null,
                null,
                null
        );
    }
}

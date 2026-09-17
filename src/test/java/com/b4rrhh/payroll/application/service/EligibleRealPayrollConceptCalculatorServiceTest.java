package com.b4rrhh.payroll.application.service;

import com.b4rrhh.payroll.basesalary.domain.PayrollObjectBindingLookupPort;
import com.b4rrhh.payroll.basesalary.domain.PayrollTableRowLookupPort;
import com.b4rrhh.payroll.basesalary.domain.PayrollTableRowRead;
import com.b4rrhh.payroll_engine.concept.domain.model.CalculationType;
import com.b4rrhh.payroll_engine.concept.domain.model.ExecutionScope;
import com.b4rrhh.payroll_engine.concept.domain.model.FeedMode;
import com.b4rrhh.payroll_engine.concept.domain.model.FunctionalNature;
import com.b4rrhh.payroll_engine.concept.domain.model.OperandRole;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptFeedRelation;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptOperand;
import com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodel;
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

import static com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodelFixtures.metamodel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollConceptGraphCalculatorServiceTest {

    @Mock
    private PayrollObjectBindingLookupPort bindingLookup;
    @Mock
    private PayrollTableRowLookupPort tableRowLookup;

    @Test
    void calculateConcept_returnsBaseSalaryFromFixedDaysAndDailyPrice() {
        PayrollConceptGraphCalculatorService service = new PayrollConceptGraphCalculatorService(
                bindingLookup,
                tableRowLookup
        );

        PayrollConceptExecutionContext context = new PayrollConceptExecutionContext(
                "ESP",
                "99002405011982",
                "99002405-G2",
                LocalDate.of(2025, 1, 31)
        );

        // La reglamentación de la ejecución: los tres conceptos, sus operandos y las
        // alimentaciones vigentes el 31/01. No hay repositorio que consultar.
        RuleSystemMetamodel metamodel = metamodel("ESP", LocalDate.of(2025, 1, 31))
                .withConcepts(
                        concept(101L, "ESP", "101", CalculationType.RATE_BY_QUANTITY),
                        concept(102L, "ESP", "D01", CalculationType.DIRECT_AMOUNT),
                        concept(103L, "ESP", "P01", CalculationType.DIRECT_AMOUNT))
                .withOperands(
                        operand("ESP", "101", OperandRole.QUANTITY, "D01"),
                        operand("ESP", "101", OperandRole.RATE, "P01"))
                .withFeeds(
                        constantSourceRelation("ESP", "D01_FIXED_30", "D01", new BigDecimal("30")),
                        tableSourceRelation("ESP", "P01_DAILY_AMOUNT_TABLE", "P01"))
                .build();

        when(bindingLookup.resolveBoundObjectCode("ESP", "AGREEMENT", "99002405011982", "P01_DAILY_AMOUNT_TABLE"))
                .thenReturn(Optional.of("P01_99002405011982"));
        when(tableRowLookup.findApplicableRow("ESP", "P01_99002405011982", "99002405-G2", LocalDate.of(2025, 1, 31)))
                .thenReturn(Optional.of(new PayrollTableRowRead(
                        4242L, "P01_99002405011982", null, new BigDecimal("47.50"))));

        PayrollConceptExecutionResult concept101 =
                service.calculateConceptResult("101", context, metamodel);

        assertEquals(0, new BigDecimal("1425.00").compareTo(concept101.amount()));
        assertEquals(0, new BigDecimal("30").compareTo(concept101.quantity()));
        assertEquals(0, new BigDecimal("47.50").compareTo(concept101.rate()));
    }

    /**
     * El precio por dia sale de una fila de tabla, y el resultado dice de cual ({@code backend#107}).
     *
     * <p>Lo que se comprueba no es que el numero sea correcto —eso ya lo hace el test de arriba—
     * sino que la procedencia sobrevive al calculo. Sin esto, la unica forma de contestar «de que
     * fila salio este 47,50» seria repetir la busqueda, que es lo que no vale: es por vigencia y
     * por categoria, y contesta donde estaria hoy el valor.
     */
    @Test
    void aDirectAmountReadFromATableSaysWhichRowItCameFrom() {
        PayrollConceptGraphCalculatorService service = new PayrollConceptGraphCalculatorService(
                bindingLookup,
                tableRowLookup
        );

        PayrollConceptExecutionContext context = new PayrollConceptExecutionContext(
                "ESP",
                "99002405011982",
                "99002405-G2",
                LocalDate.of(2025, 1, 31)
        );

        RuleSystemMetamodel metamodel = metamodel("ESP", LocalDate.of(2025, 1, 31))
                .withConcepts(
                        concept(101L, "ESP", "101", CalculationType.RATE_BY_QUANTITY),
                        concept(102L, "ESP", "D01", CalculationType.DIRECT_AMOUNT),
                        concept(103L, "ESP", "P01", CalculationType.DIRECT_AMOUNT))
                .withOperands(
                        operand("ESP", "101", OperandRole.QUANTITY, "D01"),
                        operand("ESP", "101", OperandRole.RATE, "P01"))
                .withFeeds(
                        constantSourceRelation("ESP", "D01_FIXED_30", "D01", new BigDecimal("30")),
                        tableSourceRelation("ESP", "P01_DAILY_AMOUNT_TABLE", "P01"))
                .build();

        when(bindingLookup.resolveBoundObjectCode("ESP", "AGREEMENT", "99002405011982", "P01_DAILY_AMOUNT_TABLE"))
                .thenReturn(Optional.of("P01_99002405011982"));
        when(tableRowLookup.findApplicableRow("ESP", "P01_99002405011982", "99002405-G2", LocalDate.of(2025, 1, 31)))
                .thenReturn(Optional.of(new PayrollTableRowRead(
                        4242L, "P01_99002405011982", null, new BigDecimal("47.50"))));

        PayrollConceptExecutionResult precioDia =
                service.calculateConceptResult("P01", context, metamodel);

        assertNotNull(precioDia.sourceTableRow(), "el precio por dia se leyo de una fila y tiene que decir de cual");
        assertEquals("P01_99002405011982", precioDia.sourceTableRow().tableCode());
        assertEquals(4242L, precioDia.sourceTableRow().rowId());

        // Y el concepto que lo multiplica no: su importe es un producto, no una lectura.
        PayrollConceptExecutionResult salarioBase =
                service.calculateConceptResult("101", context, metamodel);
        assertNull(salarioBase.sourceTableRow(),
                "un importe calculado a partir de otros no viene de ninguna fila");
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

package com.b4rrhh.payroll_engine.concept;

import com.b4rrhh.payroll_engine.concept.domain.exception.OperandCrossesSegmentToPeriodException;
import com.b4rrhh.payroll_engine.concept.domain.model.OperandScopeInvariant;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptOperand;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptOperandRepository;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptRepository;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ningun operando sembrado cruza de SEGMENT a PERIOD (ADR-058).
 *
 * El invariante se comprueba al guardar el grafo por el API, pero la semilla no
 * entra por el API: entra por las migraciones, y una migracion puede plantar la
 * arista prohibida sin que nadie la vea. Este test recorre todos los conceptos
 * sembrados del rule system ESP y falla si alguno PERIOD lee un operando
 * SEGMENT, nombrando el operando y los dos conceptos.
 *
 * Hoy la semilla lo cumple sobre el papel —35 conceptos en PERIOD y el 101
 * (SALARIO_BASE) en SEGMENT desde la V118, y al 101 nadie lo lee como operando—,
 * asi que el test esta en verde sin merito. Su momento llega con backend#64,
 * cuando los ambitos de los 36 se repasen uno a uno: ahi cada cambio a SEGMENT
 * de un concepto que alguien lee como operando hara saltar esto.
 *
 * Se prueba en rojo plantando una arista prohibida, no solo en verde: un
 * guardian que nunca ha fallado no se sabe si mira.
 */
@TestSobreEsquemaReal
class SeededGraphOperandScopeInvariantTest {

    private static final String RULE_SYSTEM_CODE = "ESP";

    @Autowired
    private PayrollConceptRepository conceptRepository;

    @Autowired
    private PayrollConceptOperandRepository operandRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void noSeededOperandCrossesFromSegmentToPeriod() {
        Scan scan = scan();

        assertThat(scan.conceptsScanned)
                .as("la semilla de ESP tiene conceptos; si no, el guardian no mira nada")
                .isPositive();
        assertThat(scan.operandsScanned)
                .as("la semilla de ESP tiene operandos; si no, el guardian no mira nada")
                .isPositive();
        assertThat(scan.violations)
                .as("operandos sembrados que cruzan de SEGMENT a PERIOD (ADR-058)")
                .isEmpty();
    }

    @Test
    void guardFailsWhenAForbiddenEdgeIsPlanted() {
        // Un concepto SEGMENT nuevo, que nadie siembra, leido como operando por el
        // 800 (IRPF), que es PERIOD en la semilla. El rol LEFT no lo usa el 800,
        // asi que la fila no choca con la unicidad (target, role). La transaccion
        // del test lo deshace todo.
        seedSegmentConcept("T_GUARD_SEGMENT", "T_GUARD_SEGMENT");
        jdbc.update("insert into payroll_engine.payroll_concept_operand "
                        + "(target_object_id, operand_role, source_object_id) "
                        + "select target.id, 'LEFT', source.id "
                        + "from payroll_engine.payroll_object target, payroll_engine.payroll_object source "
                        + "where target.rule_system_code = ? and target.object_type_code = 'CONCEPT' "
                        + "  and target.object_code = '800' "
                        + "  and source.rule_system_code = ? and source.object_type_code = 'CONCEPT' "
                        + "  and source.object_code = 'T_GUARD_SEGMENT'",
                RULE_SYSTEM_CODE, RULE_SYSTEM_CODE);

        Scan scan = scan();

        assertThat(scan.violations).hasSize(1);
        assertThat(scan.violations.get(0))
                .contains("LEFT")
                .contains("ESP/800 (")
                .contains("ESP/T_GUARD_SEGMENT (");
    }

    private Scan scan() {
        List<PayrollConcept> concepts = conceptRepository.findAllByRuleSystemCode(RULE_SYSTEM_CODE);
        Map<String, PayrollConcept> byCode = concepts.stream()
                .collect(Collectors.toMap(PayrollConcept::getConceptCode, Function.identity()));

        Scan scan = new Scan();
        scan.conceptsScanned = concepts.size();
        for (PayrollConcept target : concepts) {
            List<PayrollConceptOperand> operands = operandRepository
                    .findByRuleSystemCodeAndConceptCode(RULE_SYSTEM_CODE, target.getConceptCode());
            for (PayrollConceptOperand operand : operands) {
                scan.operandsScanned++;
                PayrollConcept source = byCode.get(operand.getSourceObject().getObjectCode());
                assertThat(source)
                        .as("el origen %s del operando %s de %s es un concepto sembrado",
                                operand.getSourceObject().getObjectCode(), operand.getOperandRole(),
                                target.getConceptCode())
                        .isNotNull();
                try {
                    OperandScopeInvariant.check(target, operand.getOperandRole(), source);
                } catch (OperandCrossesSegmentToPeriodException violation) {
                    scan.violations.add(violation.getMessage());
                }
            }
        }
        return scan;
    }

    private void seedSegmentConcept(String objectCode, String mnemonic) {
        jdbc.update("insert into payroll_engine.payroll_object "
                        + "(rule_system_code, object_type_code, object_code, created_at, updated_at) "
                        + "values (?, 'CONCEPT', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                RULE_SYSTEM_CODE, objectCode);
        jdbc.update("insert into payroll_engine.payroll_concept "
                        + "(object_id, concept_mnemonic, calculation_type, functional_nature, "
                        + "payslip_order_code, execution_scope, created_at, updated_at) "
                        + "select id, ?, 'DIRECT_AMOUNT', 'TECHNICAL', null, 'SEGMENT', "
                        + "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP "
                        + "from payroll_engine.payroll_object "
                        + "where rule_system_code = ? and object_type_code = 'CONCEPT' and object_code = ?",
                mnemonic, RULE_SYSTEM_CODE, objectCode);
    }

    private static final class Scan {
        int conceptsScanned;
        int operandsScanned;
        final List<String> violations = new ArrayList<>();
    }
}

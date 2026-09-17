package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.payroll.application.port.RuleSystemLastChangeLookupPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * El último cambio conocido en la reglamentación del motor, de una consulta ({@code backend#107}).
 *
 * <p>Siete tablas y un máximo. Van por {@code union all} de siete {@code max(updated_at)} y no por
 * uniones entre ellas: cada término mira su propio índice y devuelve una fila, mientras que cruzar
 * las siete para quedarse con un número sería un plan mucho mayor para la misma respuesta.
 *
 * <p>Los conceptos, sus operandos y sus alimentaciones no llevan {@code rule_system_code}: cuelgan
 * de un objeto, y por eso esos tres términos sí tienen que pasar por {@code payroll_object}.
 */
@Component
public class RuleSystemLastChangeLookupAdapter implements RuleSystemLastChangeLookupPort {

    private static final String LAST_CHANGE_QUERY = """
        select max(cambio) from (
            select max(o.updated_at) as cambio
              from payroll_engine.payroll_object o
             where o.rule_system_code = :ruleSystemCode
            union all
            select max(c.updated_at)
              from payroll_engine.payroll_concept c
              join payroll_engine.payroll_object o on o.id = c.object_id
             where o.rule_system_code = :ruleSystemCode
            union all
            select max(op.updated_at)
              from payroll_engine.payroll_concept_operand op
              join payroll_engine.payroll_object o on o.id = op.target_object_id
             where o.rule_system_code = :ruleSystemCode
            union all
            select max(f.updated_at)
              from payroll_engine.payroll_concept_feed_relation f
              join payroll_engine.payroll_object o on o.id = f.target_object_id
             where o.rule_system_code = :ruleSystemCode
            union all
            select max(a.updated_at)
              from payroll_engine.concept_assignment a
             where a.rule_system_code = :ruleSystemCode
            union all
            select max(b.updated_at)
              from payroll.payroll_object_binding b
             where b.rule_system_code = :ruleSystemCode
            union all
            select max(r.updated_at)
              from payroll.payroll_table_row r
             where r.rule_system_code = :ruleSystemCode
        ) reglamentacion
        """;

    private final EntityManager entityManager;

    public RuleSystemLastChangeLookupAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<LocalDateTime> lastChangedAt(String ruleSystemCode) {
        Object cambio = entityManager.createNativeQuery(LAST_CHANGE_QUERY)
                .setParameter("ruleSystemCode", ruleSystemCode)
                .getSingleResult();

        // Nulo cuando ese sistema de reglas no tiene reglamentacion ninguna: los siete terminos
        // devuelven nulo y el maximo de siete nulos es nulo. No es un error, es «no hay reglas».
        if (cambio instanceof Timestamp timestamp) {
            return Optional.of(timestamp.toLocalDateTime());
        }
        if (cambio instanceof LocalDateTime localDateTime) {
            return Optional.of(localDateTime);
        }
        return Optional.empty();
    }
}

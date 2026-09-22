package com.b4rrhh.payroll_engine.execution.infrastructure.persistence;

import com.b4rrhh.payroll_engine.execution.domain.model.SsTarifaPrimaAt;
import com.b4rrhh.payroll_engine.execution.domain.port.SsTarifaPrimasAtRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class SsTarifaPrimasAtJdbcAdapter implements SsTarifaPrimasAtRepository {

    private final JdbcTemplate jdbc;

    public SsTarifaPrimasAtJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * El {@code like} va al reves de lo habitual: es el CNAE de la empresa el que tiene que
     * empezar por el de la entrada, no al contrario. Y se ordena por longitud descendente para
     * que gane la entrada mas especifica, que es como la tarifa declara sus excepciones.
     */
    @Override
    public Optional<SsTarifaPrimaAt> findForCnae(
            String ruleSystemCode, String cnaeCode, LocalDate referenceDate) {

        if (cnaeCode == null || cnaeCode.isBlank()) {
            return Optional.empty();
        }
        List<SsTarifaPrimaAt> results = jdbc.query(
                """
                SELECT cnae_code, activity_name, tipo_it, tipo_ims
                  FROM payroll_engine.ss_tarifa_primas_at
                 WHERE rule_system_code = ?
                   AND ? LIKE cnae_code || '%'
                   AND valid_from <= ?
                   AND (valid_to IS NULL OR valid_to >= ?)
                 ORDER BY length(cnae_code) DESC, valid_from DESC
                 LIMIT 1
                """,
                (rs, i) -> new SsTarifaPrimaAt(
                        rs.getString("cnae_code"),
                        rs.getString("activity_name"),
                        rs.getBigDecimal("tipo_it"),
                        rs.getBigDecimal("tipo_ims")
                ),
                ruleSystemCode, cnaeCode.trim(), referenceDate, referenceDate
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}

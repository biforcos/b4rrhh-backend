package com.b4rrhh.payroll_engine.execution.infrastructure.persistence;

import com.b4rrhh.payroll_engine.execution.domain.port.SsDesempleoModalidadRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class SsDesempleoModalidadJdbcAdapter implements SsDesempleoModalidadRepository {

    private final JdbcTemplate jdbc;

    public SsDesempleoModalidadJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<String> findModalidad(
            String ruleSystemCode, String contractCode, LocalDate referenceDate) {

        if (contractCode == null || contractCode.isBlank()) {
            return Optional.empty();
        }
        List<String> filas = jdbc.query(
                """
                SELECT modality
                  FROM payroll_engine.ss_desempleo_modalidad_contrato
                 WHERE rule_system_code = ?
                   AND contract_code = ?
                   AND valid_from <= ?
                   AND (valid_to IS NULL OR valid_to >= ?)
                 ORDER BY valid_from DESC
                 LIMIT 1
                """,
                (rs, i) -> rs.getString("modality"),
                ruleSystemCode, contractCode.trim(), referenceDate, referenceDate
        );
        return filas.isEmpty() ? Optional.empty() : Optional.of(filas.get(0));
    }
}

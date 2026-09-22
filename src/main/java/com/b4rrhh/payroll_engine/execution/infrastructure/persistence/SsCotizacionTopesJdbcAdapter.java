package com.b4rrhh.payroll_engine.execution.infrastructure.persistence;

import com.b4rrhh.payroll_engine.execution.domain.model.SsCotizacionTope;
import com.b4rrhh.payroll_engine.execution.domain.port.SsCotizacionTopesRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class SsCotizacionTopesJdbcAdapter implements SsCotizacionTopesRepository {

    private final JdbcTemplate jdbc;

    public SsCotizacionTopesJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<SsCotizacionTope> findActive(
            String ruleSystemCode, String grupoCode, String periodType,
            String contingencyCode, LocalDate referenceDate) {
        List<SsCotizacionTope> results = jdbc.query(
                """
                SELECT base_min, base_max
                  FROM payroll_engine.ss_cotizacion_topes
                 WHERE rule_system_code = ?
                   AND grupo_code       = ?
                   AND period_type      = ?
                   AND contingency_code = ?
                   AND valid_from      <= ?
                   AND (valid_to IS NULL OR valid_to >= ?)
                 ORDER BY valid_from DESC
                 LIMIT 1
                """,
                (rs, i) -> new SsCotizacionTope(
                        rs.getBigDecimal("base_min"),
                        rs.getBigDecimal("base_max")
                ),
                ruleSystemCode, grupoCode, periodType, contingencyCode, referenceDate, referenceDate
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}

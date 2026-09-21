package com.b4rrhh.payroll_engine.execution.infrastructure.persistence;

import com.b4rrhh.payroll_engine.execution.domain.port.SsCotizacionTiposRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Lee {@code payroll_engine.ss_cotizacion_tipos}, misma consulta que la de los topes.
 *
 * <p>El {@code order by valid_from desc limit 1} no es defensivo de mas: con dos vigencias
 * abiertas por error, la que manda es la mas reciente que ya haya empezado, que es la respuesta
 * menos sorprendente de las dos posibles.
 */
@Repository
public class SsCotizacionTiposJdbcAdapter implements SsCotizacionTiposRepository {

    private final JdbcTemplate jdbc;

    public SsCotizacionTiposJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<BigDecimal> findRate(
            String ruleSystemCode, String contingencyCode, LocalDate referenceDate) {
        List<BigDecimal> results = jdbc.query(
                """
                SELECT rate
                  FROM payroll_engine.ss_cotizacion_tipos
                 WHERE rule_system_code = ?
                   AND contingency_code = ?
                   AND valid_from      <= ?
                   AND (valid_to IS NULL OR valid_to >= ?)
                 ORDER BY valid_from DESC
                 LIMIT 1
                """,
                (rs, i) -> rs.getBigDecimal("rate"),
                ruleSystemCode, contingencyCode, referenceDate, referenceDate
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}

package com.b4rrhh.payroll_engine.execution.infrastructure.persistence;

import com.b4rrhh.payroll_engine.execution.domain.model.ItPrestacionTramo;
import com.b4rrhh.payroll_engine.execution.domain.port.ItPrestacionTramoRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class ItPrestacionTramoJdbcAdapter implements ItPrestacionTramoRepository {

    private final JdbcTemplate jdbc;

    public ItPrestacionTramoJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<ItPrestacionTramo> findActive(
            String ruleSystemCode,
            String absenceTypeCode,
            String tramoCode,
            LocalDate referenceDate
    ) {
        List<ItPrestacionTramo> filas = jdbc.query(
                """
                SELECT day_from, day_to, percentage
                  FROM payroll_engine.it_prestacion_tramo
                 WHERE rule_system_code = ?
                   AND absence_type_code = ?
                   AND tramo_code = ?
                   AND effective_from <= ?
                   AND (effective_to IS NULL OR effective_to >= ?)
                 ORDER BY effective_from DESC
                 LIMIT 1
                """,
                (rs, i) -> {
                    // wasNull() habla de la ULTIMA columna leida, asi que se pregunta justo despues
                    // de leer day_to y antes de leer nada mas. Escrito dentro del constructor, Java
                    // evaluaba primero day_from y wasNull() contestaba por el: el tramo sin fin
                    // llegaba con day_to = 0 y no pagaba ni un dia, sin error ninguno.
                    int dayTo = rs.getInt("day_to");
                    Integer hasta = rs.wasNull() ? null : dayTo;
                    return new ItPrestacionTramo(
                            rs.getInt("day_from"),
                            hasta,
                            rs.getBigDecimal("percentage"));
                },
                ruleSystemCode, absenceTypeCode, tramoCode, referenceDate, referenceDate
        );
        return filas.isEmpty() ? Optional.empty() : Optional.of(filas.get(0));
    }
}

package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.payroll.application.port.PayrollBulkStatusTransitionPort;
import com.b4rrhh.payroll.application.usecase.PayrollCalculationUnit;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PayrollBulkStatusTransitionAdapter implements PayrollBulkStatusTransitionPort {

    // Un UPDATE que modifica datos dentro de un WITH se ejecuta aunque la consulta final no lo
    // lea, y todas las partes ven la misma instantanea: el recuento sale del estado de antes.
    // updated_at se pone desde la JVM, como hace el @PreUpdate de PayrollEntity.
    private static final String SQL = """
            with unit (employee_type_code, employee_number, presence_number) as (
                select * from unnest(?::text[], ?::text[], ?::int[])
            ), found as (
                select p.id, p.status
                  from payroll.payroll p
                  join unit u using (employee_type_code, employee_number, presence_number)
                 where p.rule_system_code = ?
                   and p.payroll_period_code = ?
                   and p.payroll_type_code = ?
            ), moved as (
                update payroll.payroll p
                   set status = ?, status_reason_code = ?, updated_at = ?
                  from found f
                 where p.id = f.id
                   and f.status = ?
                returning p.id
            )
            select status, count(*) as total from found group by status
            """;

    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;

    public PayrollBulkStatusTransitionAdapter(JdbcTemplate jdbcTemplate, EntityManager entityManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.entityManager = entityManager;
    }

    @Override
    public Map<PayrollStatus, Integer> moveStatus(
            String ruleSystemCode,
            String payrollPeriodCode,
            String payrollTypeCode,
            List<PayrollCalculationUnit> units,
            PayrollStatus from,
            PayrollStatus to,
            String statusReasonCode
    ) {
        Map<PayrollStatus, Integer> before = new EnumMap<>(PayrollStatus.class);
        if (units.isEmpty()) {
            return before;
        }
        // La sentencia no pasa por la sesion de JPA, como cualquier actualizacion masiva: lo que
        // la sesion tenga pendiente se vuelca antes, para que el UPDATE lo vea, y la sesion se
        // vacia despues, para que nadie en la misma transaccion lea el recibo con el estado de
        // antes desde la cache. Es lo que hace @Modifying(flushAutomatically, clearAutomatically).
        entityManager.flush();
        jdbcTemplate.query(connection -> {
            PreparedStatement statement = connection.prepareStatement(SQL);
            statement.setArray(1, connection.createArrayOf("text",
                    units.stream().map(PayrollCalculationUnit::employeeTypeCode).toArray()));
            statement.setArray(2, connection.createArrayOf("text",
                    units.stream().map(PayrollCalculationUnit::employeeNumber).toArray()));
            statement.setArray(3, connection.createArrayOf("int4",
                    units.stream().map(PayrollCalculationUnit::presenceNumber).toArray()));
            statement.setString(4, ruleSystemCode);
            statement.setString(5, payrollPeriodCode);
            statement.setString(6, payrollTypeCode);
            statement.setString(7, to.name());
            statement.setString(8, statusReasonCode);
            statement.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(10, from.name());
            return statement;
        }, resultSet -> {
            before.put(PayrollStatus.valueOf(resultSet.getString("status")), resultSet.getInt("total"));
        });
        entityManager.clear();
        return before;
    }
}

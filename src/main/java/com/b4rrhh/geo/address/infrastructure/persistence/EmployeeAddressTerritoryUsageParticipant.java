package com.b4rrhh.geo.address.infrastructure.persistence;

import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * El tipo de vía y la provincia de las direcciones de los empleados (backend#168). El país ya lo
 * cuenta {@code AddressRuleEntityUsageParticipant}, que declara la columna; tipo de vía y provincia
 * no se declaran ahí porque la guardia de integridad exigiría que existan, y una dirección sin
 * normalizar puede guardar uno que no está.
 */
@Component
public class EmployeeAddressTerritoryUsageParticipant extends PostalAddressRuleEntityUsageParticipant {

    public EmployeeAddressTerritoryUsageParticipant(JdbcTemplate jdbcTemplate, RuleEntityRepository ruleEntities) {
        super(jdbcTemplate, ruleEntities, "addresses", "employee.address", null);
    }
}

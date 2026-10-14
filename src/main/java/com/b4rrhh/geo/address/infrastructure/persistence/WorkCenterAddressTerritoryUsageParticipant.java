package com.b4rrhh.geo.address.infrastructure.persistence;

import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** El tipo de vía, la provincia y el país de las direcciones de los centros de trabajo (backend#168). */
@Component
public class WorkCenterAddressTerritoryUsageParticipant extends PostalAddressRuleEntityUsageParticipant {

    public WorkCenterAddressTerritoryUsageParticipant(JdbcTemplate jdbcTemplate, RuleEntityRepository ruleEntities) {
        super(jdbcTemplate, ruleEntities, "work-center-addresses", "rulesystem.work_center_profile",
                new Owner("WORK_CENTER", "work_center_rule_entity_id"));
    }
}

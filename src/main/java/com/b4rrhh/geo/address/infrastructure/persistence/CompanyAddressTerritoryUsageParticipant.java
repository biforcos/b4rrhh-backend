package com.b4rrhh.geo.address.infrastructure.persistence;

import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** El tipo de vía, la provincia y el país de las direcciones de las empresas (backend#168). */
@Component
public class CompanyAddressTerritoryUsageParticipant extends PostalAddressRuleEntityUsageParticipant {

    public CompanyAddressTerritoryUsageParticipant(JdbcTemplate jdbcTemplate, RuleEntityRepository ruleEntities) {
        super(jdbcTemplate, ruleEntities, "company-addresses", "rulesystem.company_profile",
                new Owner("COMPANY", "company_rule_entity_id"));
    }
}

package com.b4rrhh.rulesystem.agreementprofile.infrastructure.persistence;

import com.b4rrhh.rulesystem.agreementprofile.application.port.AgreementCatalogLookupPort;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resolves AGREEMENT rule entity IDs by business key, through the one port that resolves rule
 * entities by the level of their type (ADR-077 §4, backend#157).
 */
@Component
public class AgreementCatalogLookupAdapter implements AgreementCatalogLookupPort {

    private static final String AGREEMENT_TYPE_CODE = "AGREEMENT";

    private final RuleEntityRepository ruleEntityRepository;

    public AgreementCatalogLookupAdapter(RuleEntityRepository ruleEntityRepository) {
        this.ruleEntityRepository = ruleEntityRepository;
    }

    @Override
    public Optional<Long> findAgreementRuleEntityId(String ruleSystemCode, String agreementCode) {
        return ruleEntityRepository
                .findByBusinessKey(ruleSystemCode, AGREEMENT_TYPE_CODE, agreementCode)
                .map(RuleEntity::getId);
    }
}

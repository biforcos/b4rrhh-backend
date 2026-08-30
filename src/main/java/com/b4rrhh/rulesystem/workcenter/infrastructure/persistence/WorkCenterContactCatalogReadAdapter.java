package com.b4rrhh.rulesystem.workcenter.infrastructure.persistence;

import com.b4rrhh.rulesystem.workcenter.application.port.WorkCenterContactCatalogReadPort;
import com.b4rrhh.rulesystem.workcenter.application.usecase.WorkCenterRuleEntityTypeCodes;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class WorkCenterContactCatalogReadAdapter implements WorkCenterContactCatalogReadPort {

    private final RuleEntityLabelResolver ruleEntityLabelResolver;

    public WorkCenterContactCatalogReadAdapter(RuleEntityLabelResolver ruleEntityLabelResolver) {
        this.ruleEntityLabelResolver = ruleEntityLabelResolver;
    }

    @Override
    public Optional<String> findContactTypeName(String ruleSystemCode, String contactTypeCode, String languageCode) {
        return ruleEntityLabelResolver.resolveName(
                ruleSystemCode,
                WorkCenterRuleEntityTypeCodes.CONTACT_TYPE,
                contactTypeCode,
                languageCode
        );
    }
}

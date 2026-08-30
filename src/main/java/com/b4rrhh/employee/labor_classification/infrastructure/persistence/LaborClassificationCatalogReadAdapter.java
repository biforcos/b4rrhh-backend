package com.b4rrhh.employee.labor_classification.infrastructure.persistence;

import com.b4rrhh.employee.labor_classification.application.port.LaborClassificationCatalogReadPort;
import com.b4rrhh.employee.labor_classification.application.usecase.LaborClassificationRuleEntityTypeCodes;
import com.b4rrhh.rulesystem.agreementcategoryprofile.domain.port.AgreementCategoryProfileRepository;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class LaborClassificationCatalogReadAdapter implements LaborClassificationCatalogReadPort {

    private final RuleEntityLabelResolver ruleEntityLabelResolver;
    private final AgreementCategoryProfileRepository agreementCategoryProfileRepository;

    public LaborClassificationCatalogReadAdapter(
            RuleEntityLabelResolver ruleEntityLabelResolver,
            AgreementCategoryProfileRepository agreementCategoryProfileRepository
    ) {
        this.ruleEntityLabelResolver = ruleEntityLabelResolver;
        this.agreementCategoryProfileRepository = agreementCategoryProfileRepository;
    }

    @Override
    public Optional<String> findAgreementName(String ruleSystemCode, String agreementCode, String languageCode) {
        return ruleEntityLabelResolver.resolveName(
                ruleSystemCode,
                LaborClassificationRuleEntityTypeCodes.AGREEMENT,
                agreementCode,
                languageCode
        );
    }

    @Override
    public Optional<String> findAgreementCategoryName(String ruleSystemCode, String agreementCategoryCode, String languageCode) {
        return ruleEntityLabelResolver.resolveName(
                ruleSystemCode,
                LaborClassificationRuleEntityTypeCodes.AGREEMENT_CATEGORY,
                agreementCategoryCode,
                languageCode
        );
    }

    @Override
    public Optional<String> findGrupoCotizacionCode(String ruleSystemCode, String agreementCategoryCode) {
        if (agreementCategoryCode == null || agreementCategoryCode.trim().isEmpty()) {
            return Optional.empty();
        }
        return agreementCategoryProfileRepository.findGrupoCotizacionCodeByCategoryCode(
                ruleSystemCode.trim().toUpperCase(),
                agreementCategoryCode.trim().toUpperCase()
        );
    }
}
package com.b4rrhh.employee.workcenter.infrastructure.persistence;

import com.b4rrhh.employee.workcenter.application.port.WorkCenterCatalogReadPort;
import com.b4rrhh.employee.workcenter.application.usecase.WorkCenterRuleEntityTypeCodes;
import com.b4rrhh.employee.workcenter.domain.port.WorkCenterCompanyLookupPort;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
public class WorkCenterCatalogReadAdapter implements WorkCenterCatalogReadPort {

    private final RuleEntityLabelResolver ruleEntityLabelResolver;
    private final WorkCenterCompanyLookupPort workCenterCompanyLookupPort;

    public WorkCenterCatalogReadAdapter(
            RuleEntityLabelResolver ruleEntityLabelResolver,
            WorkCenterCompanyLookupPort workCenterCompanyLookupPort
    ) {
        this.ruleEntityLabelResolver = ruleEntityLabelResolver;
        this.workCenterCompanyLookupPort = workCenterCompanyLookupPort;
    }

    @Override
    public Optional<String> findWorkCenterName(String ruleSystemCode, String workCenterCode, String languageCode) {
        String normalizedRuleSystemCode = normalizeRequiredUppercase("ruleSystemCode", ruleSystemCode);
        String normalizedWorkCenterCode = normalizeRequiredUppercase("workCenterCode", workCenterCode);

        return ruleEntityLabelResolver.resolveName(
                normalizedRuleSystemCode,
                WorkCenterRuleEntityTypeCodes.WORK_CENTER,
                normalizedWorkCenterCode,
                languageCode
        );
    }

    @Override
    public Optional<String> findWorkCenterCompanyCode(String ruleSystemCode, String workCenterCode, LocalDate referenceDate) {
        String normalizedRuleSystemCode = normalizeRequiredUppercase("ruleSystemCode", ruleSystemCode);
        String normalizedWorkCenterCode = normalizeRequiredUppercase("workCenterCode", workCenterCode);

        return workCenterCompanyLookupPort.findCompanyCode(
                normalizedRuleSystemCode,
                normalizedWorkCenterCode,
                referenceDate
        );
    }

    @Override
    public Optional<String> findCompanyName(String ruleSystemCode, String companyCode, String languageCode) {
        String normalizedRuleSystemCode = normalizeRequiredUppercase("ruleSystemCode", ruleSystemCode);
        String normalizedCompanyCode = normalizeRequiredUppercase("companyCode", companyCode);

        return ruleEntityLabelResolver.resolveName(
                normalizedRuleSystemCode,
                WorkCenterRuleEntityTypeCodes.COMPANY,
                normalizedCompanyCode,
                languageCode
        );
    }

    private String normalizeRequiredUppercase(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        return value.trim().toUpperCase();
    }
}

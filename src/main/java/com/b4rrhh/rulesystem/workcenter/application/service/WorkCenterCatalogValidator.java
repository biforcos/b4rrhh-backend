package com.b4rrhh.rulesystem.workcenter.application.service;

import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import com.b4rrhh.rulesystem.workcenter.application.usecase.WorkCenterRuleEntityTypeCodes;
import com.b4rrhh.rulesystem.workcenter.domain.exception.WorkCenterCompanyInvalidException;
import com.b4rrhh.rulesystem.workcenter.domain.exception.WorkCenterContactTypeInvalidException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component("rulesystemWorkCenterCatalogValidator")
public class WorkCenterCatalogValidator {

    private final RuleEntityRepository ruleEntityRepository;

    public WorkCenterCatalogValidator(RuleEntityRepository ruleEntityRepository) {
        this.ruleEntityRepository = ruleEntityRepository;
    }

    public void validateCompanyCode(String ruleSystemCode, String companyCode, LocalDate referenceDate) {
        if (companyCode == null) {
            return;
        }

        validate(ruleSystemCode, WorkCenterRuleEntityTypeCodes.COMPANY, companyCode, referenceDate,
                () -> new WorkCenterCompanyInvalidException(ruleSystemCode, companyCode));
    }

    public void validateContactTypeCode(String ruleSystemCode, String contactTypeCode, LocalDate referenceDate) {
        validate(ruleSystemCode, WorkCenterRuleEntityTypeCodes.CONTACT_TYPE, contactTypeCode, referenceDate,
                () -> new WorkCenterContactTypeInvalidException(ruleSystemCode, contactTypeCode));
    }

    private void validate(
            String ruleSystemCode,
            String ruleEntityTypeCode,
            String code,
            LocalDate referenceDate,
            java.util.function.Supplier<? extends RuntimeException> exceptionSupplier
    ) {
        RuleEntity ruleEntity = ruleEntityRepository
                .findApplicableByBusinessKey(ruleSystemCode, ruleEntityTypeCode, code, referenceDate)
                .orElseThrow(exceptionSupplier);

        if (!ruleEntity.isActive() || !isDateApplicable(ruleEntity, referenceDate)) {
            throw exceptionSupplier.get();
        }
    }

    private boolean isDateApplicable(RuleEntity ruleEntity, LocalDate referenceDate) {
        if (referenceDate == null) {
            return true;
        }

        boolean startsBeforeOrOnDate = !referenceDate.isBefore(ruleEntity.getStartDate());
        boolean endsAfterOrOnDate = ruleEntity.getEndDate() == null || !referenceDate.isAfter(ruleEntity.getEndDate());
        return startsBeforeOrOnDate && endsAfterOrOnDate;
    }
}
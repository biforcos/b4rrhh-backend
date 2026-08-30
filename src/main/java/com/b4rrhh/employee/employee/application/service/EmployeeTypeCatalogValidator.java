package com.b4rrhh.employee.employee.application.service;

import com.b4rrhh.employee.employee.application.usecase.EmployeeRuleEntityTypeCodes;
import com.b4rrhh.employee.employee.domain.exception.EmployeeTypeInvalidException;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class EmployeeTypeCatalogValidator {

    private final RuleEntityRepository ruleEntityRepository;

    public EmployeeTypeCatalogValidator(RuleEntityRepository ruleEntityRepository) {
        this.ruleEntityRepository = ruleEntityRepository;
    }

    public void validateEmployeeTypeCode(
            String ruleSystemCode,
            String employeeTypeCode,
            LocalDate referenceDate
    ) {
        RuleEntity ruleEntity = ruleEntityRepository
                .findByBusinessKey(ruleSystemCode, EmployeeRuleEntityTypeCodes.EMPLOYEE_TYPE, employeeTypeCode)
                .orElseThrow(() -> new EmployeeTypeInvalidException(employeeTypeCode));

        if (!ruleEntity.isActive() || !isDateApplicable(ruleEntity, referenceDate)) {
            throw new EmployeeTypeInvalidException(employeeTypeCode);
        }
    }

    private boolean isDateApplicable(RuleEntity ruleEntity, LocalDate referenceDate) {
        if (referenceDate == null) return true;
        boolean startsBeforeOrOnDate = !referenceDate.isBefore(ruleEntity.getStartDate());
        boolean endsAfterOrOnDate = ruleEntity.getEndDate() == null || !referenceDate.isAfter(ruleEntity.getEndDate());
        return startsBeforeOrOnDate && endsAfterOrOnDate;
    }
}

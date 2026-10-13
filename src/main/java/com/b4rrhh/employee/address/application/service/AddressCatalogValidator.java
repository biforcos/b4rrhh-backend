package com.b4rrhh.employee.address.application.service;

import com.b4rrhh.employee.address.application.usecase.AddressRuleEntityTypeCodes;
import com.b4rrhh.employee.address.domain.exception.AddressCatalogValueInvalidException;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class AddressCatalogValidator {

    private final RuleEntityRepository ruleEntityRepository;

    public AddressCatalogValidator(RuleEntityRepository ruleEntityRepository) {
        this.ruleEntityRepository = ruleEntityRepository;
    }

    public String normalizeRequiredCode(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new AddressCatalogValueInvalidException(fieldName, String.valueOf(value));
        }

        return value.trim().toUpperCase();
    }

    public void validateAddressTypeCode(String ruleSystemCode, String addressTypeCode, LocalDate referenceDate) {
        validateCatalog(
                ruleSystemCode,
                AddressRuleEntityTypeCodes.EMPLOYEE_ADDRESS_TYPE,
                addressTypeCode,
                "addressTypeCode",
                referenceDate
        );
    }

    private void validateCatalog(
            String ruleSystemCode,
            String ruleEntityTypeCode,
            String code,
            String fieldName,
            LocalDate referenceDate
    ) {
        RuleEntity ruleEntity = ruleEntityRepository
                .findByBusinessKey(ruleSystemCode, ruleEntityTypeCode, code)
                .orElseThrow(() -> new AddressCatalogValueInvalidException(fieldName, code));

        if (!ruleEntity.isActive() || !isDateApplicable(ruleEntity, referenceDate)) {
            throw new AddressCatalogValueInvalidException(fieldName, code);
        }
    }

    // Inclusive temporal semantics: startDate <= referenceDate <= endDate (when endDate exists).
    private boolean isDateApplicable(RuleEntity ruleEntity, LocalDate referenceDate) {
        if (referenceDate == null) {
            return true;
        }

        boolean startsBeforeOrOnDate = !referenceDate.isBefore(ruleEntity.getStartDate());
        boolean endsAfterOrOnDate = ruleEntity.getEndDate() == null || !referenceDate.isAfter(ruleEntity.getEndDate());

        return startsBeforeOrOnDate && endsAfterOrOnDate;
    }
}

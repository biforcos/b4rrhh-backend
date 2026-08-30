package com.b4rrhh.employee.address.infrastructure.persistence;

import com.b4rrhh.employee.address.application.port.AddressCatalogReadPort;
import com.b4rrhh.employee.address.application.usecase.AddressRuleEntityTypeCodes;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AddressCatalogReadAdapter implements AddressCatalogReadPort {

    private final RuleEntityLabelResolver ruleEntityLabelResolver;

    public AddressCatalogReadAdapter(RuleEntityLabelResolver ruleEntityLabelResolver) {
        this.ruleEntityLabelResolver = ruleEntityLabelResolver;
    }

    @Override
    public Optional<String> findAddressTypeName(String ruleSystemCode, String addressTypeCode, String languageCode) {
        return ruleEntityLabelResolver.resolveName(
                ruleSystemCode,
                AddressRuleEntityTypeCodes.EMPLOYEE_ADDRESS_TYPE,
                addressTypeCode,
                languageCode
        );
    }
}

package com.b4rrhh.employee.address.infrastructure.web.assembler;

import com.b4rrhh.employee.address.application.usecase.AddressRuleEntityTypeCodes;
import com.b4rrhh.employee.address.domain.model.Address;
import com.b4rrhh.employee.address.infrastructure.web.dto.AddressResponse;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AddressResponseAssembler {

    private final RuleEntityLabelResolver ruleEntityLabelResolver;

    public AddressResponseAssembler(RuleEntityLabelResolver ruleEntityLabelResolver) {
        this.ruleEntityLabelResolver = ruleEntityLabelResolver;
    }

    public AddressResponse toResponse(String ruleSystemCode, Address address, ResponseLanguage language) {
        String addressTypeName = ruleEntityLabelResolver
                .resolveName(ruleSystemCode, AddressRuleEntityTypeCodes.EMPLOYEE_ADDRESS_TYPE,
                        address.getAddressTypeCode(), language.code())
                .orElse(null);

        return new AddressResponse(
                address.getAddressNumber(),
                address.getAddressTypeCode(),
                addressTypeName,
                address.getStreet(),
                address.getCity(),
                address.getCountryCode(),
                address.getPostalCode(),
                address.getRegionCode(),
                address.getStartDate(),
                address.getEndDate()
        );
    }

    public List<AddressResponse> toResponseList(String ruleSystemCode, List<Address> addresss, ResponseLanguage language) {
        return addresss.stream()
                .map(address -> toResponse(ruleSystemCode, address, language))
                .toList();
    }
}

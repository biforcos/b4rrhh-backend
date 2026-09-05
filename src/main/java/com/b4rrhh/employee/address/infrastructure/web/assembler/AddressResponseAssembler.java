package com.b4rrhh.employee.address.infrastructure.web.assembler;

import com.b4rrhh.employee.address.application.model.AddressPlan;
import com.b4rrhh.employee.address.application.model.AddressPlanAdjustment;
import com.b4rrhh.employee.address.application.usecase.AddressRuleEntityTypeCodes;
import com.b4rrhh.employee.address.domain.model.Address;
import com.b4rrhh.employee.address.domain.model.AddressOccurrence;
import com.b4rrhh.employee.address.domain.model.AddressPeriod;
import com.b4rrhh.employee.address.infrastructure.web.dto.AddressOccurrenceResponse;
import com.b4rrhh.employee.address.infrastructure.web.dto.AddressPeriodResponse;
import com.b4rrhh.employee.address.infrastructure.web.dto.AddressPlanAdjustmentResponse;
import com.b4rrhh.employee.address.infrastructure.web.dto.AddressPlanResponse;
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

    public AddressPlanResponse toPlanResponse(AddressPlan plan) {
        return new AddressPlanResponse(
                plan.operation().name(),
                plan.isAccepted(),
                plan.rejection() == null ? null : plan.rejection().name(),
                plan.addressTypeCode(),
                toOccurrence(plan.occurrence()),
                plan.correctedOccurrence() == null ? null : toOccurrence(plan.correctedOccurrence()),
                toAdjustment(plan.adjustedOccurrence()),
                plan.overlaps().stream().map(this::toPeriod).toList(),
                plan.gaps().stream().map(this::toPeriod).toList(),
                plan.stretchCandidates().stream().map(this::toOccurrence).toList(),
                plan.projected().stream().map(this::toOccurrence).toList()
        );
    }

    private AddressPlanAdjustmentResponse toAdjustment(AddressPlanAdjustment adjustment) {
        if (adjustment == null) {
            return null;
        }

        return new AddressPlanAdjustmentResponse(
                adjustment.addressNumber(),
                toPeriod(adjustment.before()),
                toPeriod(adjustment.after())
        );
    }

    private AddressOccurrenceResponse toOccurrence(AddressOccurrence occurrence) {
        return new AddressOccurrenceResponse(occurrence.addressNumber(), occurrence.startDate(), occurrence.endDate());
    }

    private AddressPeriodResponse toPeriod(AddressPeriod period) {
        return new AddressPeriodResponse(period.startDate(), period.endDate());
    }
}

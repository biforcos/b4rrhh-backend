package com.b4rrhh.employee.address.application.usecase;

import com.b4rrhh.employee.address.application.model.AddressPlan;
import com.b4rrhh.employee.address.application.port.EmployeeAddressContext;
import com.b4rrhh.employee.address.application.port.EmployeeAddressLookupPort;
import com.b4rrhh.employee.address.application.service.AddressCatalogValidator;
import com.b4rrhh.employee.address.application.service.AddressTimelineService;
import com.b4rrhh.employee.address.domain.exception.AddressEmployeeNotFoundException;
import com.b4rrhh.employee.address.domain.exception.AddressRuleSystemNotFoundException;
import com.b4rrhh.employee.address.domain.model.Address;
import com.b4rrhh.employee.address.domain.port.AddressRepository;
import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.rulesystem.domain.port.RuleSystemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adds an address to the series of its type (ADR-057, decision 2). The one
 * automatic consequence is closing, the day before, the address of the same
 * type in force on the new start date; anything else the resulting series
 * would break is rejected by the plan.
 */
@Service
public class CreateAddressService implements CreateAddressUseCase {

    private final AddressRepository addressRepository;
    private final EmployeeAddressLookupPort employeeAddressLookupPort;
    private final RuleSystemRepository ruleSystemRepository;
    private final AddressCatalogValidator addressCatalogValidator;
    private final AddressTimelineService addressTimelineService;

    public CreateAddressService(
            AddressRepository addressRepository,
            EmployeeAddressLookupPort employeeAddressLookupPort,
            RuleSystemRepository ruleSystemRepository,
            AddressCatalogValidator addressCatalogValidator,
            AddressTimelineService addressTimelineService
    ) {
        this.addressRepository = addressRepository;
        this.employeeAddressLookupPort = employeeAddressLookupPort;
        this.ruleSystemRepository = ruleSystemRepository;
        this.addressCatalogValidator = addressCatalogValidator;
        this.addressTimelineService = addressTimelineService;
    }

    @Override
    @Transactional
    public Address create(CreateAddressCommand command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String normalizedEmployeeNumber = normalizeEmployeeNumber(command.employeeNumber());

        ruleSystemRepository.findByCode(normalizedRuleSystemCode)
                .orElseThrow(() -> new AddressRuleSystemNotFoundException(normalizedRuleSystemCode));

        EmployeeAddressContext employee = employeeAddressLookupPort
                .findByBusinessKeyForUpdate(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                )
                .orElseThrow(() -> new AddressEmployeeNotFoundException(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                ));

        String addressTypeCode = addressCatalogValidator.normalizeRequiredCode("addressTypeCode", command.addressTypeCode());
        addressCatalogValidator.validateAddressTypeCode(normalizedRuleSystemCode, addressTypeCode, command.startDate());

        String countryCode = addressCatalogValidator.normalizeRequiredCode("countryCode", command.countryCode());
        addressCatalogValidator.validateCountryCode(normalizedRuleSystemCode, countryCode, command.startDate());

        int nextAddressNumber = addressRepository.findMaxAddressNumberByEmployeeId(employee.employeeId())
                .map(value -> value + 1)
                .orElse(1);

        Address newAddress = new Address(
                null,
                employee.employeeId(),
                nextAddressNumber,
                addressTypeCode,
                command.street(),
                command.city(),
                countryCode,
                command.postalCode(),
                command.regionCode(),
                command.startDate(),
                command.endDate(),
                null,
                null
        );

        // Adding is planned against the invariants of the series of this type (ADR-057): no
        // overlap within the type, no gap in the presence when the type is the domicile. The
        // one automatic consequence is closing the address of the type in force on the new
        // start date the day before it.
        AddressPlan plan = addressTimelineService.planAdd(
                employee.employeeId(),
                normalizedRuleSystemCode,
                newAddress.getAddressTypeCode(),
                new DateRange(newAddress.getStartDate(), newAddress.getEndDate())
        );
        addressTimelineService.requireAccepted(
                plan,
                normalizedRuleSystemCode,
                normalizedEmployeeTypeCode,
                normalizedEmployeeNumber
        );

        if (plan.adjustsAnOccurrence()) {
            Address covering = addressRepository
                    .findByEmployeeIdAndAddressNumber(employee.employeeId(), plan.adjustedOccurrence().addressNumber())
                    .orElseThrow(() -> new IllegalStateException(
                            "Planned occurrence vanished: addressNumber=" + plan.adjustedOccurrence().addressNumber()
                    ));
            addressRepository.save(covering.adjustEndDate(plan.adjustedOccurrence().after().endDate()));
        }

        return addressRepository.save(newAddress);
    }

    private String normalizeRuleSystemCode(String ruleSystemCode) {
        if (ruleSystemCode == null || ruleSystemCode.trim().isEmpty()) {
            throw new IllegalArgumentException("ruleSystemCode is required");
        }

        return ruleSystemCode.trim().toUpperCase();
    }

    private String normalizeEmployeeTypeCode(String employeeTypeCode) {
        if (employeeTypeCode == null || employeeTypeCode.trim().isEmpty()) {
            throw new IllegalArgumentException("employeeTypeCode is required");
        }

        return employeeTypeCode.trim().toUpperCase();
    }

    private String normalizeEmployeeNumber(String employeeNumber) {
        if (employeeNumber == null || employeeNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("employeeNumber is required");
        }

        return employeeNumber.trim();
    }
}

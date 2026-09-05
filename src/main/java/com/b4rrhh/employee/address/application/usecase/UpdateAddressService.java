package com.b4rrhh.employee.address.application.usecase;

import com.b4rrhh.employee.address.application.model.AddressPlan;
import com.b4rrhh.employee.address.application.port.EmployeeAddressContext;
import com.b4rrhh.employee.address.application.port.EmployeeAddressLookupPort;
import com.b4rrhh.employee.address.application.service.AddressCatalogValidator;
import com.b4rrhh.employee.address.application.service.AddressTimelineService;
import com.b4rrhh.employee.address.domain.exception.AddressEmployeeNotFoundException;
import com.b4rrhh.employee.address.domain.exception.AddressNotFoundException;
import com.b4rrhh.employee.address.domain.exception.AddressRuleSystemNotFoundException;
import com.b4rrhh.employee.address.domain.model.Address;
import com.b4rrhh.employee.address.domain.port.AddressRepository;
import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.rulesystem.domain.port.RuleSystemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Corrects an address: its fields, its dates, or both. Nothing else moves
 * (ADR-057, decision 3): if the corrected dates leave a gap in the domicile
 * or an overlap within the type, the plan rejects them and names what the
 * user would have to stretch instead. The type is the series and never
 * changes here.
 */
@Service
public class UpdateAddressService implements UpdateAddressUseCase {

    private final AddressRepository addressRepository;
    private final EmployeeAddressLookupPort employeeAddressLookupPort;
    private final RuleSystemRepository ruleSystemRepository;
    private final AddressCatalogValidator addressCatalogValidator;
    private final AddressTimelineService addressTimelineService;

    public UpdateAddressService(
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
    public Address update(UpdateAddressCommand command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String normalizedEmployeeNumber = normalizeEmployeeNumber(command.employeeNumber());
        Integer normalizedAddressNumber = normalizeAddressNumber(command.addressNumber());

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

        Address existing = addressRepository
                .findByEmployeeIdAndAddressNumber(employee.employeeId(), normalizedAddressNumber)
                .orElseThrow(() -> new AddressNotFoundException(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber,
                        normalizedAddressNumber
                ));

        String countryCode = normalizeCountryCodeForCorrection(command.countryCode(), existing.getCountryCode());
        String street = normalizeRequiredTextForCorrection("street", command.street(), existing.getStreet());
        String city = normalizeRequiredTextForCorrection("city", command.city(), existing.getCity());
        String postalCode = normalizeOptionalTextForCorrection(command.postalCode(), existing.getPostalCode());
        String regionCode = normalizeOptionalCodeForCorrection(command.regionCode(), existing.getRegionCode());
        boolean datesCorrected = command.startDate() != null;
        LocalDate startDate = datesCorrected ? command.startDate() : existing.getStartDate();
        LocalDate endDate = datesCorrected ? command.endDate() : existing.getEndDate();
        addressCatalogValidator.validateCountryCode(
                normalizedRuleSystemCode,
                countryCode,
                startDate
        );

        Address corrected = existing.correct(
                street,
                city,
                countryCode,
                postalCode,
                regionCode,
                startDate,
                endDate
        );

        AddressPlan plan = addressTimelineService.planCorrect(
                employee.employeeId(),
                normalizedRuleSystemCode,
                existing,
                new DateRange(corrected.getStartDate(), corrected.getEndDate())
        );
        addressTimelineService.requireAccepted(
                plan,
                normalizedRuleSystemCode,
                normalizedEmployeeTypeCode,
                normalizedEmployeeNumber
        );

        return addressRepository.save(corrected);
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

    private Integer normalizeAddressNumber(Integer addressNumber) {
        if (addressNumber == null || addressNumber <= 0) {
            throw new IllegalArgumentException("addressNumber must be a positive integer");
        }

        return addressNumber;
    }

    private String normalizeCountryCodeForCorrection(String requestedCountryCode, String existingCountryCode) {
        String normalizedRequested = requestedCountryCode == null ? "" : requestedCountryCode.trim();
        if (!normalizedRequested.isEmpty()) {
            return addressCatalogValidator.normalizeRequiredCode("countryCode", normalizedRequested);
        }

        String normalizedExisting = existingCountryCode == null ? "" : existingCountryCode.trim();
        if (!normalizedExisting.isEmpty()) {
            return normalizedExisting.toUpperCase();
        }

        throw new IllegalArgumentException("countryCode is required");
    }

    private String normalizeRequiredTextForCorrection(String fieldName, String requestedValue, String existingValue) {
        String normalizedRequested = requestedValue == null ? "" : requestedValue.trim();
        if (!normalizedRequested.isEmpty()) {
            return normalizedRequested;
        }

        String normalizedExisting = existingValue == null ? "" : existingValue.trim();
        if (!normalizedExisting.isEmpty()) {
            return normalizedExisting;
        }

        throw new IllegalArgumentException(fieldName + " is required");
    }

    private String normalizeOptionalTextForCorrection(String requestedValue, String existingValue) {
        if (requestedValue == null) {
            return existingValue;
        }

        if (requestedValue.trim().isEmpty()) {
            return existingValue;
        }

        return requestedValue.trim();
    }

    private String normalizeOptionalCodeForCorrection(String requestedValue, String existingValue) {
        if (requestedValue == null) {
            return existingValue;
        }

        if (requestedValue.trim().isEmpty()) {
            return existingValue;
        }

        return requestedValue.trim().toUpperCase();
    }
}

package com.b4rrhh.employee.address.application.usecase;

import com.b4rrhh.employee.address.application.model.AddressPlan;
import com.b4rrhh.employee.address.application.port.EmployeeAddressContext;
import com.b4rrhh.employee.address.application.port.EmployeeAddressLookupPort;
import com.b4rrhh.employee.address.application.service.AddressTimelineService;
import com.b4rrhh.employee.address.domain.exception.AddressEmployeeNotFoundException;
import com.b4rrhh.employee.address.domain.exception.AddressNotFoundException;
import com.b4rrhh.employee.address.domain.exception.AddressRuleSystemNotFoundException;
import com.b4rrhh.employee.address.domain.exception.InvalidAddressDateRangeException;
import com.b4rrhh.employee.address.domain.model.Address;
import com.b4rrhh.employee.address.domain.port.AddressRepository;
import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.geo.address.application.service.PostalAddressValidator;
import com.b4rrhh.geo.address.domain.model.PostalAddress;
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
    private final PostalAddressValidator postalAddressValidator;
    private final AddressTimelineService addressTimelineService;

    public UpdateAddressService(
            AddressRepository addressRepository,
            EmployeeAddressLookupPort employeeAddressLookupPort,
            RuleSystemRepository ruleSystemRepository,
            PostalAddressValidator postalAddressValidator,
            AddressTimelineService addressTimelineService
    ) {
        this.addressRepository = addressRepository;
        this.employeeAddressLookupPort = employeeAddressLookupPort;
        this.ruleSystemRepository = ruleSystemRepository;
        this.postalAddressValidator = postalAddressValidator;
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

        LocalDate startDate = requireCorrectedStartDate(command.startDate());
        LocalDate endDate = command.endDate();
        // La dirección corregida entera, validada como en el alta (backend#155).
        PostalAddress address = postalAddressValidator.validate(normalizedRuleSystemCode, command.address(), startDate);

        Address corrected = existing.correct(
                address,
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

    /**
     * The correction says where the address starts, always. Omitting it used
     * to mean "leave both dates as they are", which reads the same on the
     * wire as a client that forgot to send it: the request was legal, the
     * answer was a 200, and the user's edit was gone without a trace. Three
     * screens fell for it before anyone noticed (backend#69). Correcting
     * without moving the start is now said by repeating the date the address
     * already has.
     */
    private LocalDate requireCorrectedStartDate(LocalDate startDate) {
        if (startDate == null) {
            throw new InvalidAddressDateRangeException("startDate is required");
        }

        return startDate;
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
}

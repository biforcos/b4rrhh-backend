package com.b4rrhh.employee.address.application.usecase;

import com.b4rrhh.employee.address.application.model.AddressPlan;
import com.b4rrhh.employee.address.application.port.EmployeeAddressContext;
import com.b4rrhh.employee.address.application.port.EmployeeAddressLookupPort;
import com.b4rrhh.employee.address.application.service.AddressTimelineService;
import com.b4rrhh.employee.address.domain.exception.AddressEmployeeNotFoundException;
import com.b4rrhh.employee.address.domain.exception.AddressNotFoundException;
import com.b4rrhh.employee.address.domain.model.Address;
import com.b4rrhh.employee.address.domain.port.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Removes an address from the series of its type (ADR-057, decision 3).
 * Removing the last one reopens the previous one of the same type: it is the
 * "oops" and it is safe. Removing one in the middle of the domicile would
 * leave a gap, and the invariant rejects it naming the neighbours the user
 * would have to stretch first; in an optional type the gap is legal.
 */
@Service
public class DeleteAddressService implements DeleteAddressUseCase {

    private final AddressRepository addressRepository;
    private final EmployeeAddressLookupPort employeeAddressLookupPort;
    private final AddressTimelineService addressTimelineService;

    public DeleteAddressService(
            AddressRepository addressRepository,
            EmployeeAddressLookupPort employeeAddressLookupPort,
            AddressTimelineService addressTimelineService
    ) {
        this.addressRepository = addressRepository;
        this.employeeAddressLookupPort = employeeAddressLookupPort;
        this.addressTimelineService = addressTimelineService;
    }

    @Override
    @Transactional
    public void delete(DeleteAddressCommand command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String normalizedEmployeeNumber = normalizeEmployeeNumber(command.employeeNumber());
        Integer normalizedAddressNumber = normalizeAddressNumber(command.addressNumber());

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

        AddressPlan plan = addressTimelineService.planRemove(employee.employeeId(), normalizedRuleSystemCode, existing);
        addressTimelineService.requireAccepted(
                plan,
                normalizedRuleSystemCode,
                normalizedEmployeeTypeCode,
                normalizedEmployeeNumber
        );

        if (plan.adjustsAnOccurrence()) {
            Address previous = addressRepository
                    .findByEmployeeIdAndAddressNumber(employee.employeeId(), plan.adjustedOccurrence().addressNumber())
                    .orElseThrow(() -> new IllegalStateException(
                            "Planned occurrence vanished: addressNumber=" + plan.adjustedOccurrence().addressNumber()
                    ));
            addressRepository.save(previous.adjustEndDate(plan.adjustedOccurrence().after().endDate()));
        }

        addressRepository.delete(existing);
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

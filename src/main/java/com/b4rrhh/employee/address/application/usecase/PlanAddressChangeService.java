package com.b4rrhh.employee.address.application.usecase;

import com.b4rrhh.employee.address.application.model.AddressPlan;
import com.b4rrhh.employee.address.application.port.EmployeeAddressContext;
import com.b4rrhh.employee.address.application.port.EmployeeAddressLookupPort;
import com.b4rrhh.employee.address.application.service.AddressTimelineService;
import com.b4rrhh.employee.address.domain.exception.AddressEmployeeNotFoundException;
import com.b4rrhh.employee.address.domain.exception.AddressNotFoundException;
import com.b4rrhh.employee.address.domain.model.Address;
import com.b4rrhh.employee.address.domain.port.AddressRepository;
import com.b4rrhh.employee.temporal.support.DateRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Answers what an add, a removal or a correction would do to an address
 * series without applying it (ADR-057, decision 6). Rejected plans come back
 * as plans, not as errors: the screen shows the gap or the overlap and the
 * user decides. The series is the one of the type: an ADD names it, a REMOVE
 * or a CORRECT takes it from the address they are about.
 */
@Service
public class PlanAddressChangeService implements PlanAddressChangeUseCase {

    private final AddressRepository addressRepository;
    private final EmployeeAddressLookupPort employeeAddressLookupPort;
    private final AddressTimelineService addressTimelineService;

    public PlanAddressChangeService(
            AddressRepository addressRepository,
            EmployeeAddressLookupPort employeeAddressLookupPort,
            AddressTimelineService addressTimelineService
    ) {
        this.addressRepository = addressRepository;
        this.employeeAddressLookupPort = employeeAddressLookupPort;
        this.addressTimelineService = addressTimelineService;
    }

    @Override
    @Transactional(readOnly = true)
    public AddressPlan plan(PlanAddressChangeCommand command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String normalizedEmployeeNumber = normalizeEmployeeNumber(command.employeeNumber());
        if (command.operation() == null) {
            throw new IllegalArgumentException("operation is required");
        }

        EmployeeAddressContext employee = employeeAddressLookupPort
                .findByBusinessKey(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                )
                .orElseThrow(() -> new AddressEmployeeNotFoundException(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                ));

        return switch (command.operation()) {
            case ADD -> addressTimelineService.planAdd(
                    employee.employeeId(),
                    normalizedRuleSystemCode,
                    requireAddressTypeCode(command),
                    requireDates(command)
            );
            case REMOVE -> addressTimelineService.planRemove(
                    employee.employeeId(),
                    normalizedRuleSystemCode,
                    requireOccurrence(command, employee)
            );
            case CORRECT -> addressTimelineService.planCorrect(
                    employee.employeeId(),
                    normalizedRuleSystemCode,
                    requireOccurrence(command, employee),
                    requireDates(command)
            );
        };
    }

    private static String requireAddressTypeCode(PlanAddressChangeCommand command) {
        if (command.addressTypeCode() == null || command.addressTypeCode().trim().isEmpty()) {
            throw new IllegalArgumentException("addressTypeCode is required to plan an add");
        }

        return command.addressTypeCode().trim().toUpperCase();
    }

    /**
     * The address a REMOVE or a CORRECT is about. Its type is the series; a
     * type given alongside that does not match it is a contradiction, not a
     * request to move the address to another series.
     */
    private Address requireOccurrence(PlanAddressChangeCommand command, EmployeeAddressContext employee) {
        if (command.addressNumber() == null || command.addressNumber() <= 0) {
            throw new IllegalArgumentException("addressNumber must be a positive integer");
        }

        Address existing = addressRepository
                .findByEmployeeIdAndAddressNumber(employee.employeeId(), command.addressNumber())
                .orElseThrow(() -> new AddressNotFoundException(
                        employee.ruleSystemCode(),
                        employee.employeeTypeCode(),
                        employee.employeeNumber(),
                        command.addressNumber()
                ));

        if (command.addressTypeCode() != null
                && !command.addressTypeCode().trim().isEmpty()
                && !existing.getAddressTypeCode().equals(command.addressTypeCode().trim().toUpperCase())) {
            throw new IllegalArgumentException("addressTypeCode does not match address #"
                    + existing.getAddressNumber() + ", which is " + existing.getAddressTypeCode());
        }

        return existing;
    }

    private static DateRange requireDates(PlanAddressChangeCommand command) {
        if (command.startDate() == null) {
            throw new IllegalArgumentException("startDate is required");
        }

        return new DateRange(command.startDate(), command.endDate());
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

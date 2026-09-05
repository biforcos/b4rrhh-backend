package com.b4rrhh.employee.address.infrastructure.persistence;

import com.b4rrhh.employee.address.application.port.AddressTypeCoverageLookupPort;
import com.b4rrhh.employee.temporal.support.TimelineCoverage;
import com.b4rrhh.rulesystem.employeeaddresstypeprofile.domain.model.EmployeeAddressTypeCoverage;
import com.b4rrhh.rulesystem.employeeaddresstypeprofile.domain.port.EmployeeAddressTypeProfileRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Reads the coverage of an address type from its catalog profile (backend#53)
 * and says it in the words of the temporal component. The switch is
 * exhaustive on purpose: a coverage the catalog learns and this vertical does
 * not translate stops compiling.
 */
@Component
public class AddressTypeCoverageLookupAdapter implements AddressTypeCoverageLookupPort {

    private final EmployeeAddressTypeProfileRepository employeeAddressTypeProfileRepository;

    public AddressTypeCoverageLookupAdapter(EmployeeAddressTypeProfileRepository employeeAddressTypeProfileRepository) {
        this.employeeAddressTypeProfileRepository = employeeAddressTypeProfileRepository;
    }

    @Override
    public Optional<TimelineCoverage> findCoverage(String ruleSystemCode, String addressTypeCode) {
        return employeeAddressTypeProfileRepository
                .findCoverageByAddressType(ruleSystemCode, addressTypeCode)
                .map(AddressTypeCoverageLookupAdapter::toTimelineCoverage);
    }

    private static TimelineCoverage toTimelineCoverage(EmployeeAddressTypeCoverage coverage) {
        return switch (coverage) {
            case MANDATORY -> TimelineCoverage.MANDATORY;
            case OPTIONAL -> TimelineCoverage.OPTIONAL;
        };
    }
}

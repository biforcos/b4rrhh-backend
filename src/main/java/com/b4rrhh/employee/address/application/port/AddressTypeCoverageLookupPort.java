package com.b4rrhh.employee.address.application.port;

import com.b4rrhh.employee.temporal.support.TimelineCoverage;

import java.util.Optional;

/**
 * What the catalog says about the coverage of an address type in a rule
 * system (backend#53): mandatory for the domicile, optional for the rest.
 * Empty when the type has no declared coverage, which the caller must not
 * turn into a default.
 */
public interface AddressTypeCoverageLookupPort {

    Optional<TimelineCoverage> findCoverage(String ruleSystemCode, String addressTypeCode);
}

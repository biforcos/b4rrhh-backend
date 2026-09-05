package com.b4rrhh.rulesystem.employeeaddresstypeprofile.domain.port;

import com.b4rrhh.rulesystem.employeeaddresstypeprofile.domain.model.EmployeeAddressTypeCoverage;

import java.util.Optional;

/**
 * The PROFILE extension of {@code EMPLOYEE_ADDRESS_TYPE} (ADR-053, backend#53).
 * It is declared as required, so every address type of every rule system has
 * a coverage; an empty answer means the type does not exist or its profile
 * was never seeded, and neither is something the caller should guess about.
 */
public interface EmployeeAddressTypeProfileRepository {

    Optional<EmployeeAddressTypeCoverage> findCoverageByAddressType(String ruleSystemCode, String addressTypeCode);
}

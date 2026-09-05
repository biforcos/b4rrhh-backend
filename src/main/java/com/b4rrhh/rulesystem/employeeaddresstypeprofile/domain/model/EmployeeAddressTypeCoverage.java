package com.b4rrhh.rulesystem.employeeaddresstypeprofile.domain.model;

/**
 * What the catalog says about a type of employee address (backend#53): whether
 * an employee has to have one of this type at every date they are present.
 *
 * <p>Exactly one type per rule system is {@link #MANDATORY}: the domicile the
 * employee is legally bound to declare. The rest are things an employee may
 * simply not have. Which type is the domicile is decided here, in the rule
 * system's catalog, and never as a constant in code: another rule system may
 * mark a different one.
 */
public enum EmployeeAddressTypeCoverage {

    /** While the employee is present there has to be an address of this type in force. */
    MANDATORY,

    /** An employee may have none, and a stretch of presence without one is legal. */
    OPTIONAL
}

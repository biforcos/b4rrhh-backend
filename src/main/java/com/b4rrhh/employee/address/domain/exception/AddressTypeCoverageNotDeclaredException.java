package com.b4rrhh.employee.address.domain.exception;

/**
 * The catalog does not say whether this type of address is mandatory or
 * optional (backend#53). Every {@code EMPLOYEE_ADDRESS_TYPE} carries that
 * decision as its PROFILE extension; a type without it is not optional by
 * default, it is a question nobody answered, and no address of that type
 * can be written until someone does.
 */
public class AddressTypeCoverageNotDeclaredException extends RuntimeException {

    private final String addressTypeCode;

    public AddressTypeCoverageNotDeclaredException(String ruleSystemCode, String addressTypeCode) {
        super("The catalog does not declare the coverage of address type "
                + addressTypeCode
                + " in ruleSystemCode="
                + ruleSystemCode
                + ": its EMPLOYEE_ADDRESS_TYPE profile is missing.");
        this.addressTypeCode = addressTypeCode;
    }

    public String addressTypeCode() {
        return addressTypeCode;
    }
}

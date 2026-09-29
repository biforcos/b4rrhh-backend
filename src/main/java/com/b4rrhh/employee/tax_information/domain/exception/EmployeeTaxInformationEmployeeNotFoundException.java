package com.b4rrhh.employee.tax_information.domain.exception;

public class EmployeeTaxInformationEmployeeNotFoundException extends RuntimeException {
    public EmployeeTaxInformationEmployeeNotFoundException(String ruleSystemCode, String employeeTypeCode, String employeeNumber) {
        super("No existe el empleado " + ruleSystemCode + "/" + employeeTypeCode + "/" + employeeNumber + ".");
    }
}

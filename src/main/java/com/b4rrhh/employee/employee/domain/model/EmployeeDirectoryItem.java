package com.b4rrhh.employee.employee.domain.model;

public class EmployeeDirectoryItem {

    private final String ruleSystemCode;
    private final String employeeTypeCode;
    private final String employeeNumber;
    private final String displayName;
    // El nombre y el primer apellido, aparte del displayName: las iniciales del avatar son
    // nombre + primer apellido, y de «Maria del Carmen Gutierrez Ramos» no se deduce dónde acaba el
    // nombre (frontend#144).
    private final String firstName;
    private final String lastName1;
    private final String status;
    private final String workCenterCode;
    private final String photoKey;

    public EmployeeDirectoryItem(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String displayName,
            String firstName,
            String lastName1,
            String status,
            String workCenterCode,
            String photoKey
    ) {
        this.ruleSystemCode = ruleSystemCode;
        this.employeeTypeCode = employeeTypeCode;
        this.employeeNumber = employeeNumber;
        this.displayName = displayName;
        this.firstName = firstName;
        this.lastName1 = lastName1;
        this.status = status;
        this.workCenterCode = workCenterCode;
        this.photoKey = photoKey;
    }

    public String getRuleSystemCode() {
        return ruleSystemCode;
    }

    public String getEmployeeTypeCode() {
        return employeeTypeCode;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName1() {
        return lastName1;
    }

    public String getStatus() {
        return status;
    }

    public String getWorkCenterCode() {
        return workCenterCode;
    }

    public String getPhotoKey() {
        return photoKey;
    }
}

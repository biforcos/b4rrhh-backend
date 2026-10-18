package com.b4rrhh.employee.photo.application.usecase;

public record UploadEmployeePhotoCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        byte[] jpeg
) {
}

package com.b4rrhh.employee.photo.application.port;

/** El empleado y la clave de su foto en el almacén, o {@code null} si no tiene (backend#177). */
public record EmployeePhotoContext(Long employeeId, String photoKey) {
}

package com.b4rrhh.employee.employee.domain.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class EmployeePhotoTest {

    private static final String CLAVE = "photos/ESP/EMP/00001/0b6d3c1e-6f0a-4c43-9d6a-2b1a3e5f7c90.jpg";

    private Employee employeeWithNoPhoto() {
        return new Employee(1L, "ESP", "EMP", "00001",
                "Juan", "García", null, null,
                null, LocalDateTime.now(), null);
    }

    @Test
    void withPhotoKey_returns_new_instance_with_the_key() {
        Employee employee = employeeWithNoPhoto().withPhotoKey(CLAVE);

        assertThat(employee.getPhotoKey()).isEqualTo(CLAVE);
        assertThat(employee.getFirstName()).isEqualTo("Juan");
    }

    @Test
    void withoutPhoto_clears_existing_photo() {
        Employee employee = employeeWithNoPhoto()
                .withPhotoKey(CLAVE)
                .withoutPhoto();

        assertThat(employee.getPhotoKey()).isNull();
    }

    @Test
    void new_employee_has_no_photo() {
        assertThat(employeeWithNoPhoto().getPhotoKey()).isNull();
    }
}

package com.b4rrhh.employee.employee.domain.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class EmployeePhotoTest {

    private Employee employeeWithNoPhoto() {
        return new Employee(1L, "ESP", "EMP", "00001",
                "Juan", "García", null, null,
                null, LocalDateTime.now(), null);
    }

    @Test
    void withPhotoUrl_returns_new_instance_with_photo_url() {
        Employee employee = employeeWithNoPhoto().withPhotoUrl("http://minio:9000/bucket/photos/key.jpg");

        assertThat(employee.getPhotoUrl()).isEqualTo("http://minio:9000/bucket/photos/key.jpg");
        assertThat(employee.getFirstName()).isEqualTo("Juan");
    }

    @Test
    void withoutPhotoUrl_clears_existing_photo() {
        Employee employee = employeeWithNoPhoto()
                .withPhotoUrl("http://minio:9000/bucket/photos/key.jpg")
                .withoutPhotoUrl();

        assertThat(employee.getPhotoUrl()).isNull();
    }

    @Test
    void new_employee_has_null_photo_url() {
        assertThat(employeeWithNoPhoto().getPhotoUrl()).isNull();
    }
}

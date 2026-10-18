package com.b4rrhh.employee.photo.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmployeePhotoKeyTest {

    @Test
    void aNewKeyNamesTheEmployeeAndCarriesAFreshVersion() {
        EmployeePhotoKey una = EmployeePhotoKey.newFor("ESP", "INTERNAL", "EMP000025");
        EmployeePhotoKey otra = EmployeePhotoKey.newFor("ESP", "INTERNAL", "EMP000025");

        assertThat(una.value()).startsWith("photos/ESP/INTERNAL/EMP000025/").endsWith(".jpg");
        assertThat(una.value()).endsWith(una.version() + ".jpg");
        assertThat(una.version()).isNotEqualTo(otra.version());
    }

    @Test
    void anUrlIsNotAKey() {
        // Lo que guardaba photo_url hasta backend#177: la V182 lo convierte, y si algo se escapara
        // no se lee como clave.
        assertThatThrownBy(() -> new EmployeePhotoKey(
                "http://localhost:9000/b4rrhh-employee-photos/photos/ESP/INTERNAL/EMP000025/"
                        + "0b6d3c1e-6f0a-4c43-9d6a-2b1a3e5f7c90.jpg"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

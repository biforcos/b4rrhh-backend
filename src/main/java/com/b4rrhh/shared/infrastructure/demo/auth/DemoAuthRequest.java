package com.b4rrhh.shared.infrastructure.demo.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DemoAuthRequest(
        @NotBlank @Size(max = 60) String subject,
        @NotBlank @Size(max = 200) String password
) {
}

package com.b4rrhh.shared.infrastructure.demo.auth;

import java.time.Instant;
import java.util.List;

public record DemoAuthResponse(
        String tokenType,
        String token,
        String subject,
        List<String> roles,
        Instant expiresAt
) {
}

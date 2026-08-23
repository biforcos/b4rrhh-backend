package com.b4rrhh.shared.infrastructure.demo.auth;

import com.b4rrhh.shared.infrastructure.config.JwtProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * Emite tokens para la demo publica.
 *
 * Es el hermano mayor de DevAuthTokenService: mismo mecanismo de firma, pero
 * exige contrasena y solo admite una lista cerrada de sujetos, cada uno con
 * sus propios roles. El de desarrollo firma siempre ADMIN; este no.
 */
@Service
@Profile("demo")
@ConditionalOnProperty(prefix = "app.demo-auth", name = "enabled", havingValue = "true")
public class DemoAuthTokenService {

    private final JwtProperties jwtProperties;
    private final DemoAuthProperties demoAuthProperties;

    public DemoAuthTokenService(JwtProperties jwtProperties, DemoAuthProperties demoAuthProperties) {
        this.jwtProperties = jwtProperties;
        this.demoAuthProperties = demoAuthProperties;
    }

    /**
     * @return el token, o null si el sujeto no esta permitido o la contrasena
     *         no coincide. Un solo valor para los dos casos a proposito: si el
     *         error distinguiera "sujeto desconocido" de "contrasena mala",
     *         estarias contando quien existe.
     */
    public DemoAuthResponse issueToken(DemoAuthRequest request) {
        if (!passwordMatches(request.password())) {
            return null;
        }

        String subject = request.subject().trim();
        List<String> roles = demoAuthProperties.getSubjects().get(subject);
        if (roles == null || roles.isEmpty()) {
            return null;
        }

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(demoAuthProperties.getExpiresInMinutes(), ChronoUnit.MINUTES);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .claim("roles", roles)
                .build();

        return new DemoAuthResponse("Bearer", sign(claims), subject, roles, expiresAt);
    }

    /**
     * Comparacion en tiempo constante. La contrasena de la demo es publica, asi
     * que aqui no protege ningun secreto; se hace igual porque es la forma
     * correcta de comparar credenciales y no quiero dejar el mal ejemplo
     * escrito en un repositorio que ensenas.
     */
    private boolean passwordMatches(String candidate) {
        String expected = demoAuthProperties.getPassword();
        if (expected == null || expected.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                candidate.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(JWTClaimsSet claims) {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            jwt.sign(new MACSigner(signingKeyBytes()));
        } catch (JOSEException e) {
            throw new IllegalStateException("No se pudo firmar el token de demo", e);
        }
        return jwt.serialize();
    }

    private byte[] signingKeyBytes() {
        byte[] keyBytes = jwtProperties.secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least 32 bytes (256 bits) for HMAC-SHA256. "
                            + "Current length: " + keyBytes.length + " bytes.");
        }
        return keyBytes;
    }
}

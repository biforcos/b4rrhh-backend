package com.b4rrhh.shared.infrastructure.demo.counts;

import com.b4rrhh.shared.infrastructure.config.SecurityConfig;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sin el perfil 'demo' el endpoint no existe, aunque app.demo-auth.enabled
 * este a true: es la misma doble llave que /demo/auth (backend#45).
 */
@WebMvcTest(controllers = DemoCountsController.class)
@Import({DemoCountsController.class, SecurityConfig.class})
@TestPropertySource(properties = {
        "app.jwt.secret=b4rrhh-dev-secret-key-minimum-256-bits-for-hmac-sha256-algorithm",
        "app.demo-auth.enabled=true",
        "app.demo-auth.password=demo"
})
class DemoCountsControllerDisabledHttpTest {

    private static final String SECRET = "b4rrhh-dev-secret-key-minimum-256-bits-for-hmac-sha256-algorithm";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DemoCountsQuery query;

    @Test
    void endpointDoesNotExistOutsideTheDemoProfile() throws Exception {
        // Con token valido, para que lo que se vea sea la ausencia de ruta y no el muro.
        mockMvc.perform(get("/demo/counts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void endpointIsNotPublicOutsideTheDemoProfile() throws Exception {
        mockMvc.perform(get("/demo/counts"))
                .andExpect(status().isUnauthorized());
    }

    private String jwtToken() throws JOSEException {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("bifor")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(3600)))
                .build();

        SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
        return signedJwt.serialize();
    }
}

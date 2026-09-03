package com.b4rrhh.shared.infrastructure.config;

import com.b4rrhh.shared.infrastructure.demo.auth.DemoAuthProperties;
import com.b4rrhh.shared.infrastructure.dev.auth.DevAuthProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, DevAuthProperties.class, DemoAuthProperties.class})
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Prefijo que marca un secreto de desarrollo. Los valores de desarrollo
     * viven en el repositorio, y el repositorio es público: quien lo lea puede
     * firmarse un token con el rol que quiera. Por eso cualquier secreto con
     * este prefijo se rechaza fuera de local y test.
     */
    private static final String DEV_SECRET_PREFIX = "dev-only-";

    private final JwtProperties jwtProperties;
    private final DevAuthProperties devAuthProperties;
    private final DemoAuthProperties demoAuthProperties;
    private final Environment environment;

    public SecurityConfig(JwtProperties jwtProperties,
                          DevAuthProperties devAuthProperties,
                          DemoAuthProperties demoAuthProperties,
                          Environment environment) {
        this.jwtProperties = jwtProperties;
        this.devAuthProperties = devAuthProperties;
        this.demoAuthProperties = demoAuthProperties;
        this.environment = environment;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
                    if (isDevAuthEnabled()) {
                        auth.requestMatchers("/dev/auth/token").permitAll();
                    }
                    if (isDemoAuthEnabled()) {
                        // La puerta de la demo tiene que ser accesible sin token:
                        // es donde se consigue el token. Y las cifras de la
                        // portada tambien: se pintan antes de entrar (backend#45).
                        auth.requestMatchers("/demo/auth/login", "/demo/auth/info", "/demo/counts").permitAll();
                    }
                    auth.anyRequest().authenticated();
                }
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
    }

    private boolean isDevAuthEnabled() {
        return devAuthProperties.isEnabled() && environment.acceptsProfiles(Profiles.of("local"));
    }

    private boolean isDemoAuthEnabled() {
        return demoAuthProperties.isEnabled() && environment.acceptsProfiles(Profiles.of("demo"));
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return converter;
    }

    @Bean
    JwtDecoder jwtDecoder() {
        String secret = jwtProperties.secret();
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least 32 bytes (256 bits) for HMAC-SHA256. "
                    + "Current length: " + keyBytes.length + " bytes."
            );
        }
        // Sin perfil activo estamos en desarrollo o en tests: ahi el secreto de
        // desarrollo vale. En cuanto hay un perfil explicito y no es de desarrollo,
        // estamos en un despliegue de verdad y el secreto publico no se admite.
        boolean deployed = environment.getActiveProfiles().length > 0
                && !environment.acceptsProfiles(Profiles.of("local", "test"));
        if (secret.startsWith(DEV_SECRET_PREFIX) && deployed) {
            throw new IllegalStateException(
                    "app.jwt.secret sigue siendo el valor de desarrollo, que es publico. "
                    + "Define la variable de entorno JWT_SECRET con un secreto generado, "
                    + "por ejemplo: openssl rand -base64 48"
            );
        }
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }
}

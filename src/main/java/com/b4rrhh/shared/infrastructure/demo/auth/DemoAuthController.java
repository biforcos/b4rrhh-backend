package com.b4rrhh.shared.infrastructure.demo.auth;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Puerta de entrada de la demo publica.
 *
 * Existe solo con el perfil 'demo' y con app.demo-auth.enabled=true. En
 * desarrollo y en cualquier otro despliegue, estas rutas no existen.
 */
@RestController
@Profile("demo")
@ConditionalOnProperty(prefix = "app.demo-auth", name = "enabled", havingValue = "true")
@RequestMapping("/demo/auth")
public class DemoAuthController {

    private static final Logger log = LoggerFactory.getLogger(DemoAuthController.class);

    private final DemoAuthTokenService service;
    private final DemoAuthProperties properties;

    public DemoAuthController(DemoAuthTokenService service, DemoAuthProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    /**
     * Que perfiles ofrece la demo. La pantalla de acceso lo pinta sola, asi que
     * anadir un perfil es tocar configuracion, no codigo.
     *
     * Publicar la lista no revela nada: son sujetos sinteticos de una base de
     * datos sintetica, y sin la contrasena no sirven de nada.
     */
    @GetMapping("/subjects")
    public Map<String, List<String>> availableSubjects() {
        return properties.getSubjects();
    }

    @PostMapping("/login")
    public ResponseEntity<DemoAuthResponse> login(@Valid @RequestBody DemoAuthRequest request) {
        DemoAuthResponse response = service.issueToken(request);
        if (response == null) {
            log.info("Intento de acceso rechazado a la demo para el sujeto '{}'", request.subject());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("Acceso a la demo concedido a '{}' con roles {}", response.subject(), response.roles());
        return ResponseEntity.ok(response);
    }
}

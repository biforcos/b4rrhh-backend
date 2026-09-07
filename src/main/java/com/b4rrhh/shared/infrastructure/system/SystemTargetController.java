package com.b4rrhh.shared.infrastructure.system;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Con quien esta hablando quien pregunta (workforce-loader#8).
 *
 * Existe para que un cliente que va a escribir mil filas pueda comprobar antes
 * que la base del otro lado es la que su corrida espera. La pregunta que ya se
 * sabia contestar —"estas vivo", "vale mi token"— la contesta igual de bien el
 * backend equivocado.
 *
 * Fuera del contrato OpenAPI, como /dev/auth/token, /demo/counts y /actuator:
 * no es API de negocio y el frontend no genera cliente para esto.
 *
 * Solo ADMIN. Dice donde vive la base, y eso no es algo que tenga que saber
 * cualquiera que se autentique en la demo.
 */
@RestController
@RequestMapping("/system/target")
public class SystemTargetController {

    private final SystemTargetQuery query;

    public SystemTargetController(SystemTargetQuery query) {
        this.query = query;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public SystemTarget target() {
        return query.target();
    }
}

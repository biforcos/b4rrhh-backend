package com.b4rrhh.shared.infrastructure.demo.counts;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Las cifras de la demo para la portada (backend#45).
 *
 * Mismas dos llaves que la puerta de la demo: perfil 'demo' y
 * app.demo-auth.enabled=true. Fuera de ahi, la ruta no existe (404), no
 * responde vacia. Y sin autenticar, porque la portada se pinta antes de que
 * exista token: SecurityConfig la deja pasar solo cuando esta este controlador.
 */
@RestController
@Profile("demo")
@ConditionalOnProperty(prefix = "app.demo-auth", name = "enabled", havingValue = "true")
@RequestMapping("/demo/counts")
public class DemoCountsController {

    /**
     * Lo pide cada visita y cambia una vez al dia, con el reseteo nocturno.
     * Una hora de cache en el navegador (y en cualquier proxy por medio)
     * basta: como mucho, la portada va una hora por detras del reseteo.
     */
    static final CacheControl CACHE = CacheControl.maxAge(Duration.ofHours(1)).cachePublic();

    private final DemoCountsQuery query;

    public DemoCountsController(DemoCountsQuery query) {
        this.query = query;
    }

    @GetMapping
    public ResponseEntity<DemoCounts> counts() {
        return ResponseEntity.ok().cacheControl(CACHE).body(query.count());
    }
}

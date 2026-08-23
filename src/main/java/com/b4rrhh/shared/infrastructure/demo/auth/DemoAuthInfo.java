package com.b4rrhh.shared.infrastructure.demo.auth;

import java.util.List;
import java.util.Map;

/**
 * Lo que la pantalla de acceso necesita saber para pintarse sola.
 *
 * Si, incluye la contrasena. Es deliberado: en una demo abierta la contrasena
 * no es un secreto, es una instruccion. Que la sirva el backend evita que
 * frontend y backend se desincronicen el dia que se cambie.
 */
public record DemoAuthInfo(
        String password,
        Map<String, List<String>> subjects
) {
}

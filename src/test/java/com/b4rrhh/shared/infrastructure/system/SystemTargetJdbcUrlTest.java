package com.b4rrhh.shared.infrastructure.system;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * El recorte de la URL JDBC, que es donde se puede colar una contrasena.
 *
 * No necesita base, y por eso no vive en el test de integracion: es la parte
 * del endpoint que hay que poder ejecutar sin Docker delante.
 */
class SystemTargetJdbcUrlTest {

    @Test
    void takesHostAndPort() {
        assertEquals("localhost:5432", SystemTargetQuery.hostAndPortFrom("jdbc:postgresql://localhost:5432/b4rrhh"));
        assertEquals("postgres:5432", SystemTargetQuery.hostAndPortFrom("jdbc:postgresql://postgres:5432/b4rrhh"));
    }

    @Test
    void acceptsAUrlWithoutAnExplicitPort() {
        assertEquals("localhost", SystemTargetQuery.hostAndPortFrom("jdbc:postgresql://localhost/b4rrhh"));
    }

    @Test
    void dropsEverythingAfterTheQuestionMark() {
        assertEquals(
                "localhost:5432",
                SystemTargetQuery.hostAndPortFrom("jdbc:postgresql://localhost:5432/b4rrhh?user=b4rrhh&password=b4rrhh"));
    }

    @Test
    void saysItDoesNotKnowInsteadOfGuessing() {
        assertEquals("?", SystemTargetQuery.hostAndPortFrom(null));
        assertEquals("?", SystemTargetQuery.hostAndPortFrom("jdbc:postgresql:b4rrhh"));
        assertEquals("?", SystemTargetQuery.hostAndPortFrom("jdbc:postgresql:///b4rrhh"));
    }
}

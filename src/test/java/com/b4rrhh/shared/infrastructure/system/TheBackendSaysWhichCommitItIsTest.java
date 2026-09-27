package com.b4rrhh.shared.infrastructure.system;

import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El backend dice de qué commit es (`b4rrhh/deploy`, la procedencia de la semilla).
 *
 * <p>La semilla anota en {@code deploy.semilla} qué backend produjo sus datos, y hasta aquí ese commit se
 * escribía a mano en la línea de órdenes de {@code crear-semilla.sh}. La semilla del {@code deploy#22}
 * salió anotada con el commit del <b>loader</b>, que es el que se tenía delante: una procedencia escrita a
 * mano dice lo que alguien creía, no lo que sirvió. Ahora se le pregunta al backend que atendió al loader,
 * que es el único que lo sabe.
 *
 * <p>Sin autenticarse, como {@code health}: lo pregunta un script, y un commit no es un secreto. Sólo el
 * modo {@code simple} —rama, commit y hora—: el completo publicaría el correo de quien construyó.
 */
@TestWebSobreEsquemaReal
class TheBackendSaysWhichCommitItIsTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void elInfoDelActuatorLlevaElCommitYNoPideToken() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.git.commit.id").isNotEmpty())
                .andExpect(jsonPath("$.git.build").doesNotExist());
    }
}

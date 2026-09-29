package com.b4rrhh.employee;

import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La ficha dice igual, en todas sus colecciones, que un empleado no existe (b4rrhh/backend#144).
 *
 * <p>Desde el b4rrhh/frontend#92 la pantalla enseña lo que dice el servidor cuando algo falla. Eso
 * sólo sirve si el servidor lo dice, y lo dice igual para lo mismo. Medido el 28/09 con
 * {@code ESP/INTERNAL/NOEXISTE}: dos colecciones contestaban 200 vacío —la pantalla decía «no tiene
 * correcciones» de un empleado que no existe—, las ausencias daban 422 donde todas las demás daban
 * 404, y la mitad de los mensajes estaban en inglés y se leían tal cual en pantalla.
 *
 * <p>Ahora las quince: 404, y en castellano.
 */
@TestWebSobreEsquemaReal
class EveryCollectionOfTheFileSaysTheEmployeeDoesNotExistTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "contracts",
            "working-times",
            "labor-classifications",
            "work-centers",
            "cost-centers",
            "extra-payment-regimes",
            "absences",
            "retro-marks",
            "payroll-inputs?period=202609",
            "presences",
            "addresses",
            "contacts",
            "identifiers",
            "tax-information",
            "journey-v2"
    })
    @WithMockUser(roles = "ADMIN")
    void answers404InSpanish(String collection) throws Exception {
        mockMvc.perform(get("/employees/ESP/INTERNAL/NOEXISTE/" + collection))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("NOEXISTE")))
                .andExpect(jsonPath("$.message").value(not(containsString("not found"))))
                .andExpect(jsonPath("$.message").value(not(containsString("Employee"))));
    }
}

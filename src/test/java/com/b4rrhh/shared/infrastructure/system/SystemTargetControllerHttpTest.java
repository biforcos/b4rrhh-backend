package com.b4rrhh.shared.infrastructure.system;

import com.b4rrhh.shared.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Quien puede preguntar a que base escribe el backend, y que contesta (workforce-loader#8). */
@WebMvcTest(controllers = SystemTargetController.class)
@Import({SystemTargetController.class, SecurityConfig.class})
@TestPropertySource(properties = {
        "app.jwt.secret=b4rrhh-dev-secret-key-minimum-256-bits-for-hmac-sha256-algorithm"
})
class SystemTargetControllerHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SystemTargetQuery query;

    @Test
    @WithMockUser(roles = "ADMIN")
    void tellsTheDatabaseTheSchemaVersionAndHowManyEmployeesAreThere() throws Exception {
        when(query.target()).thenReturn(new SystemTarget("localhost:5432/b4rrhh_wl7", "120", 0));

        mockMvc.perform(get("/system/target"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.database").value("localhost:5432/b4rrhh_wl7"))
                .andExpect(jsonPath("$.schemaVersion").value("120"))
                .andExpect(jsonPath("$.employees").value(0))
                // Tres campos y no mas: lo lee un cliente que compara, no una pantalla.
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @WithMockUser(roles = "HR_VIEWER")
    void doesNotTellItToEveryoneWhoIsAuthenticated() throws Exception {
        mockMvc.perform(get("/system/target"))
                .andExpect(status().isForbidden());
    }

    @Test
    void doesNotTellItWithoutAToken() throws Exception {
        mockMvc.perform(get("/system/target"))
                .andExpect(status().isUnauthorized());
    }
}

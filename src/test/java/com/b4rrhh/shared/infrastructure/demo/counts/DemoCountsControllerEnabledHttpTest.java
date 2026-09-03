package com.b4rrhh.shared.infrastructure.demo.counts;

import com.b4rrhh.shared.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Con la demo encendida, las cifras se sirven sin token y con cache (backend#45). */
@WebMvcTest(controllers = DemoCountsController.class)
@ActiveProfiles("demo")
@Import({DemoCountsController.class, SecurityConfig.class})
@TestPropertySource(properties = {
        "app.jwt.secret=b4rrhh-dev-secret-key-minimum-256-bits-for-hmac-sha256-algorithm",
        "app.demo-auth.enabled=true",
        "app.demo-auth.password=demo"
})
class DemoCountsControllerEnabledHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DemoCountsQuery query;

    @Test
    void servesTheThreeCountsWithoutAuthentication() throws Exception {
        when(query.count()).thenReturn(new DemoCounts(120, 840, 37));

        mockMvc.perform(get("/demo/counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employees").value(120))
                .andExpect(jsonPath("$.calculatedPayrolls").value(840))
                .andExpect(jsonPath("$.payrollConcepts").value(37))
                // El contrato dice tres numeros: que no crezca sin que se note.
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void asksTheBrowserToCacheTheAnswer() throws Exception {
        when(query.count()).thenReturn(new DemoCounts(1, 1, 1));

        // Spring Security pone no-store a todo; el controlador tiene que ganar.
        mockMvc.perform(get("/demo/counts"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("max-age=3600")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("public")));
    }
}

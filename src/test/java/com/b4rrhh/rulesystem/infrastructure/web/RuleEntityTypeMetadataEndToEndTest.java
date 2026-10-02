package com.b4rrhh.rulesystem.infrastructure.web;

import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Extremo a extremo (backend#37): el menú de maestros lee de aquí su pertenencia, grupo,
 * orden y modo (ADR-054 §8, b4rrhh/frontend#33). Contra el esquema real: lo que se afirma
 * es la clasificación que sembró la V111, no un mock que repite lo que le digan.
 */
@TestWebSobreEsquemaReal
class RuleEntityTypeMetadataEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void theMenuMetadataComesFromTheModel() throws Exception {
        mockMvc.perform(get("/rule-entity-types"))
                .andExpect(status().isOk())
                // la lista sale en el orden del menú: primero Organización (1)
                .andExpect(jsonPath("$[0].group.displayOrder").value(1))
                // el tipo cerrado, con su clase y su grupo
                .andExpect(jsonPath("$[?(@.code == 'EMPLOYEE_TYPE')].maintenanceMode").value("CLOSED"))
                .andExpect(jsonPath("$[?(@.code == 'EMPLOYEE_TYPE')].literalClass").value("DOMAIN_VOCABULARY"))
                .andExpect(jsonPath("$[?(@.code == 'EMPLOYEE_TYPE')].group.name").value("Organización"))
                // una cita reglamentaria de referencia, en Sociedad y con su orden
                .andExpect(jsonPath("$[?(@.code == 'CONTRACT')].maintenanceMode").value("REFERENCE"))
                .andExpect(jsonPath("$[?(@.code == 'CONTRACT')].group.code").value("SOCIETY"))
                .andExpect(jsonPath("$[?(@.code == 'CONTRACT')].group.displayOrder").value(2))
                // y un nombre propio que se mantiene
                .andExpect(jsonPath("$[?(@.code == 'COMPANY')].literalClass").value("PROPER_NOUN"))
                .andExpect(jsonPath("$[?(@.code == 'COMPANY')].maintenanceMode").value("MAINTAINED"));
    }

    /**
     * ADR-053 §7 (frontend#33): las extensiones declaradas viajan con cada tipo, que es de
     * donde el menú deriva quién tiene pantalla propia. Lo afirmado es el seed de la V106.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void theDeclaredExtensionsTravelWithEachType() throws Exception {
        mockMvc.perform(get("/rule-entity-types"))
                .andExpect(status().isOk())
                // COMPANY declara su perfil obligatorio 1:1
                .andExpect(jsonPath("$[?(@.code == 'COMPANY')].extensions[*].extensionCode").value("PROFILE"))
                .andExpect(jsonPath("$[?(@.code == 'COMPANY')].extensions[?(@.extensionCode == 'PROFILE')].required").value(true))
                .andExpect(jsonPath("$[?(@.code == 'COMPANY')].extensions[?(@.extensionCode == 'PROFILE')].cardinality").value("1:1"))
                // WORK_CENTER tiene perfil y contactos
                .andExpect(jsonPath("$[?(@.code == 'WORK_CENTER')].extensions[*].extensionCode")
                        .value(containsInAnyOrder("CONTACTS", "PROFILE")))
                // un tipo que es sólo código y literal viaja con la lista vacía: «solo raíz»
                .andExpect(jsonPath("$[?(@.code == 'CONTACT_TYPE')].extensions[*]").isEmpty())
                // y COST_CENTER, la pregunta que abrió frontend#33: el modelo no le declara nada
                .andExpect(jsonPath("$[?(@.code == 'COST_CENTER')].extensions[*]").isEmpty());
    }

    /**
     * backend#164: cada tipo dice su nivel, que es de donde Catálogos agrupa por nivel antes de
     * pedir una sola entidad (b4rrhh/frontend#127). Lo afirmado es la reclasificación del
     * backend#158: uno de cada nivel que tiene tipos.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void eachTypeSaysItsLevel() throws Exception {
        mockMvc.perform(get("/rule-entity-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'CONTACT_TYPE')].level").value(1))
                .andExpect(jsonPath("$[?(@.code == 'COUNTRY')].level").value(2))
                .andExpect(jsonPath("$[?(@.code == 'COMPANY')].level").value(3));

        mockMvc.perform(get("/rule-entity-types/COUNTRY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value(2));
    }

    /**
     * backend#152: el nombre de un tipo viaja dos veces. {@code name} es el almacenado, el que
     * se edita, y no cambia con el idioma; {@code label} es el de {@code Accept-Language}, y
     * sin idioma es el almacenado. Lo afirmado es la semilla de la V164.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void theTypeNameTravelsStoredAndInTheRequestedLanguage() throws Exception {
        mockMvc.perform(get("/rule-entity-types").header("Accept-Language", "es-ES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'CONTACT_TYPE')].name").value("Contact Type"))
                .andExpect(jsonPath("$[?(@.code == 'CONTACT_TYPE')].label").value("Tipo de contacto"))
                .andExpect(jsonPath("$[?(@.code == 'EMPLOYEE_PRESENCE_EXIT_REASON')].label").value("Motivo de baja"))
                .andExpect(jsonPath("$[?(@.code == 'PAYROLL_RUN_MESSAGE')].label").value("Mensaje del cálculo de nómina"));

        mockMvc.perform(get("/rule-entity-types/CONTACT_TYPE").header("Accept-Language", "es-ES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Contact Type"))
                .andExpect(jsonPath("$.label").value("Tipo de contacto"));

        mockMvc.perform(get("/rule-entity-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'CONTACT_TYPE')].label").value("Contact Type"));
    }
}

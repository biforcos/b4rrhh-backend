package com.b4rrhh.rulesystem.employeeaddresstypeprofile;

import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El catálogo de tipos de dirección dice cuál es obligatoria (b4rrhh/backend#145).
 *
 * <p>Salió del b4rrhh/frontend#91: direcciones era la única sección que no podía ofrecer «Cerrar»,
 * porque la pantalla no sabía qué tipos son opcionales. El catálogo lo sabía desde la V117
 * ({@code employee_address_type_profile}) y la cronología de direcciones lo aplicaba, pero la API no
 * lo publicaba. Ahora lo publica, y el cierre sigue la misma regla.
 */
@TestWebSobreEsquemaReal
class TheAddressTypesSayWhichIsMandatoryEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    private String employeeNumber;

    @BeforeEach
    void hire() throws Exception {
        employeeNumber = JsonPath.read(mockMvc.perform(post("/employees/hire")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DatosDePrueba.conDni("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "employeeTypeCode": "INTERNAL",
                                  "firstName": "Ana",
                                  "lastName1": "Lopez",
                                  "hireDate": "2026-04-01",
                                  "companyCode": "ES01",
                                  "workCenterCode": "MAIN_OFFICE",
                                  "costCenterDistribution": { "items": [ { "costCenterCode": "CC_ADMIN", "allocationPercentage": 100.0 } ] },
                                  "contract": { "contractTypeCode": "100", "contractSubtypeCode": "01" },
                                  "laborClassification": { "agreementCode": "99002405011982", "agreementCategoryCode": "99002405-G3" },
                                  "workingTime": { "workingTimePercentage": 100 }
                                }
                                """)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.employeeNumber");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void theCatalogPublishesTheCoverageOfEveryType() throws Exception {
        mockMvc.perform(get("/address-types/ESP/profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleSystemCode").value("ESP"))
                .andExpect(jsonPath("$.items[?(@.addressTypeCode == 'HOME')].coverage").value(hasItem("MANDATORY")))
                .andExpect(jsonPath("$.items[?(@.addressTypeCode == 'MAILING')].coverage").value(hasItem("OPTIONAL")))
                .andExpect(jsonPath("$.items[?(@.addressTypeCode == 'FISCAL')].coverage").value(hasItem("OPTIONAL")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void closingTheMandatoryOneIsRefusedWithItsReason() throws Exception {
        int number = create("HOME");

        mockMvc.perform(put("/employees/ESP/INTERNAL/{n}/addresses/{a}", employeeNumber, number)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(close()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ADDRESS_COVERAGE_GAP"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void closingAnOptionalOneIsAccepted() throws Exception {
        create("HOME");
        int mailing = create("MAILING");

        mockMvc.perform(put("/employees/ESP/INTERNAL/{n}/addresses/{a}", employeeNumber, mailing)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(close()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endDate").value("2026-06-30"));
    }

    private int create(String type) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/employees/ESP/INTERNAL/{n}/addresses", employeeNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "addressTypeCode": "%s", "street": "Calle Mayor 1", "city": "Madrid",
                                  "countryCode": "ESP", "postalCode": "28001", "startDate": "2026-04-01" }
                                """.formatted(type)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.addressNumber");
    }

    private static String close() {
        return """
                { "street": "Calle Mayor 1", "city": "Madrid", "countryCode": "ESP", "postalCode": "28001",
                  "startDate": "2026-04-01", "endDate": "2026-06-30" }
                """;
    }
}

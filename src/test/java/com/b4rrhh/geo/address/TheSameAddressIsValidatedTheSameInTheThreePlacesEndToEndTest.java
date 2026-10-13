package com.b4rrhh.geo.address;

import jakarta.persistence.EntityManager;
import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * backend#155 (ADR-078): la dirección del empleado, la de la empresa y la del centro son la misma
 * estructura y pasan por la misma validación. Lo que se comprueba es el «Cómo se demuestra» del
 * issue, en los tres sitios y por su API:
 *
 * <ul>
 *   <li>la misma dirección válida entra en los tres, normalizada, con la provincia deducida del
 *       municipio y el nombre del municipio leído del maestro;</li>
 *   <li>un CP de Madrid con un municipio de Valencia se rechaza en los tres con el mismo mensaje;</li>
 *   <li>un municipio que no existe entra en los tres, con la marca «sin normalizar».</li>
 * </ul>
 */
@TestWebSobreEsquemaReal
class TheSameAddressIsValidatedTheSameInTheThreePlacesEndToEndTest {

    private static final String VALID = """
            {"streetTypeCode": "CL", "streetName": "Mayor", "streetNumber": "1", "floor": "3", "door": "B",
             "municipalityCode": "28079", "postalCode": "28013", "countryCode": "ESP"}
            """;

    private static final String MADRID_POSTAL_CODE_WITH_A_VALENCIA_MUNICIPALITY = """
            {"streetTypeCode": "CL", "streetName": "Mayor", "streetNumber": "1",
             "municipalityCode": "46250", "postalCode": "28013", "countryCode": "ESP"}
            """;

    private static final String A_MUNICIPALITY_THAT_DOES_NOT_EXIST = """
            {"streetTypeCode": "CL", "streetName": "Mayor", "streetNumber": "1", "locality": "Villarriba",
             "municipalityCode": "28999", "postalCode": "28013", "countryCode": "ESP"}
            """;

    private static final String OUTSIDE_SPAIN = """
            {"streetName": "Rue de la Gare", "streetNumber": "4", "locality": "Toulouse",
             "postalCode": "31000", "countryCode": "FRA"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    @WithMockUser
    void theSameValidAddressEntersNormalizedInTheThreePlaces() throws Exception {
        for (ResultActions created : inTheThreePlaces(VALID, "OK")) {
            created.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.address.normalized").value(true))
                    .andExpect(jsonPath("$.address.subdivisionCode").value("28"))
                    .andExpect(jsonPath("$.address.municipalityName").value("Madrid"))
                    .andExpect(jsonPath("$.address.streetTypeCode").value("CL"));
        }

        // Leída de la base, no de lo que se escribió: el nombre lo pone la fórmula del maestro geo.
        entityManager.flush();
        entityManager.clear();
        mockMvc.perform(get("/employees/ESP/INTERNAL/T155OK/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].address.municipalityName").value("Madrid"))
                .andExpect(jsonPath("$[0].address.normalized").value(true));
        mockMvc.perform(get("/companies/ESP/ZOK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address.municipalityName").value("Madrid"));
    }

    @Test
    @WithMockUser
    void aMadridPostalCodeWithAValenciaMunicipalityIsRejectedInTheThreeWithTheSameMessage() throws Exception {
        String expected = "postalCode 28013 is of province 28, not of province 46: the first two digits of a"
                + " Spanish postal code are its province";
        for (ResultActions rejected : inTheThreePlaces(MADRID_POSTAL_CODE_WITH_A_VALENCIA_MUNICIPALITY, "KO")) {
            rejected.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(expected));
        }
    }

    @Test
    @WithMockUser
    void aMunicipalityThatDoesNotExistEntersInTheThreeWithTheMark() throws Exception {
        for (ResultActions created : inTheThreePlaces(A_MUNICIPALITY_THAT_DOES_NOT_EXIST, "NX")) {
            created.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.address.normalized").value(false))
                    .andExpect(jsonPath("$.address.municipalityCode").value("28999"))
                    .andExpect(jsonPath("$.address.locality").value("Villarriba"))
                    // La provincia sale del CP cuando el municipio no está.
                    .andExpect(jsonPath("$.address.subdivisionCode").value("28"));
        }

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from employee.address where municipality_code = '28999' and not normalized",
                Integer.class)).isEqualTo(1);
    }

    @Test
    @WithMockUser
    void outsideSpainTheCountryIsEnough() throws Exception {
        for (ResultActions created : inTheThreePlaces(OUTSIDE_SPAIN, "FR")) {
            created.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.address.normalized").value(true))
                    .andExpect(jsonPath("$.address.subdivisionCode").doesNotExist());
        }
    }

    @Test
    @WithMockUser
    void anAddressWithoutCountryIsNotStored() throws Exception {
        for (ResultActions rejected : inTheThreePlaces("{\"streetName\": \"Mayor\"}", "NC")) {
            rejected.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("countryCode is required: an address without a country is not stored"));
        }
    }

    /** La misma dirección como domicilio de un empleado, de una empresa y de un centro. */
    private ResultActions[] inTheThreePlaces(String address, String suffix) throws Exception {
        String employeeNumber = "T155" + suffix;
        Long employeeId = DatosDePrueba.empleado(jdbcTemplate, employeeNumber);
        DatosDePrueba.presencia(jdbcTemplate, employeeId, 1, LocalDate.of(2026, 1, 1), null);

        ResultActions employee = mockMvc.perform(post("/employees/ESP/INTERNAL/{n}/addresses", employeeNumber)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"addressTypeCode": "HOME", "startDate": "2026-01-01", "address": %s}
                        """.formatted(address)));
        ResultActions company = mockMvc.perform(post("/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"ruleSystemCode": "ESP", "companyCode": "Z%s", "name": "Probe", "startDate": "2026-01-01",
                         "legalName": "Probe SA", "address": %s}
                        """.formatted(suffix, address)));
        ResultActions workCenter = mockMvc.perform(post("/work-centers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"ruleSystemCode": "ESP", "workCenterCode": "Z155%s", "name": "Probe", "startDate": "2026-01-01", "companyCode": "ES01",
                         "address": %s}
                        """.formatted(suffix, address)));
        return new ResultActions[] {employee, company, workCenter};
    }
}

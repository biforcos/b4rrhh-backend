package com.b4rrhh.geo.address.infrastructure.persistence;

import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Borrar territorio que las direcciones usan (backend#168). Las direcciones guardan tipo de vía,
 * provincia y país por código y sin clave ajena, a propósito: la marca «sin normalizar» del
 * backend#155 tiene que poder guardar uno que no está. Así que es el recuento de referencias del
 * borrado el que tiene que saberlo, como ya lo sabe de las demás referencias de catálogo.
 *
 * <p>Tipo de vía y provincia se cuentan por el país de la dirección, que es por donde se validan;
 * el país, por la reglamentación de quien tiene la dirección; la comunidad, por las provincias que
 * cuelgan de ella. Todo dentro de la transacción del test, que se deshace.</p>
 */
@TestWebSobreEsquemaReal
@WithMockUser(roles = "ADMIN")
class DeletingTerritoryThatAddressesUseIsRefusedEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void aStreetTypeNoAddressUsesIsDeleted() throws Exception {
        mockMvc.perform(delete("/layers/ESP/rule-entities/STREET_TYPE/PZ/1900-01-01"))
                .andExpect(status().isNoContent());
    }

    @Test
    void aStreetTypeAnEmployeeAddressUsesIsRefusedCountingIt() throws Exception {
        anEmployeeLivingIn("PZ", "46");

        mockMvc.perform(delete("/layers/ESP/rule-entities/STREET_TYPE/PZ/1900-01-01"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("1 addresses")));
    }

    @Test
    void aProvinceAWorkCenterAddressUsesIsRefusedCountingIt() throws Exception {
        // Branch South, en València: la V51 y la V177 la dejaron en la 46.
        mockMvc.perform(delete("/layers/ESP/rule-entities/PROVINCE/46/1900-01-01"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("1 work-center-addresses")));
    }

    @Test
    void aProvinceAnEmployeeAddressUsesIsCountedWithTheRest() throws Exception {
        anEmployeeLivingIn("CL", "46");

        mockMvc.perform(delete("/layers/ESP/rule-entities/PROVINCE/46/1900-01-01"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("1 addresses")))
                .andExpect(jsonPath("$.message", containsString("1 work-center-addresses")));
    }

    @Test
    void aRegionItsProvincesHangFromIsRefusedCountingThem() throws Exception {
        // La Comunitat Valenciana: Alicante, Castellón y Valencia.
        mockMvc.perform(delete("/layers/ESP/rule-entities/REGION/10/1900-01-01"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("3 provinces")));
    }

    @Test
    void aCountryACompanyAddressUsesIsRefusedCountingIt() throws Exception {
        jdbcTemplate.update("""
                update rulesystem.company_profile p set country_code = 'ITA'
                  from rulesystem.rule_entity e
                 where e.id = p.company_rule_entity_id and e.layer_code = 'FRA' and e.code = 'FR01'
                """);

        mockMvc.perform(delete("/layers/INT/rule-entities/COUNTRY/ITA/1900-01-01"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("1 company-addresses")));
    }

    private void anEmployeeLivingIn(String streetTypeCode, String provinceCode) {
        Long employeeId = jdbcTemplate.queryForObject("""
                insert into employee.employee (rule_system_code, employee_type_code, employee_number,
                                               first_name, last_name_1)
                values ('ESP', 'INTERNAL', 'ZZ168', 'Prueba', 'Territorio')
                returning id
                """, Long.class);
        jdbcTemplate.update("""
                insert into employee.address (employee_id, address_number, address_type_code, country_code,
                                              street_type_code, subdivision_code, start_date)
                values (?, 1, 'HOME', 'ESP', ?, ?, date '2026-01-01')
                """, employeeId, streetTypeCode, provinceCode);
    }
}

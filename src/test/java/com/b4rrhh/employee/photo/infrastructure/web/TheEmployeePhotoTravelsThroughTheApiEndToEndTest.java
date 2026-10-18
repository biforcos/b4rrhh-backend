package com.b4rrhh.employee.photo.infrastructure.web;

import com.b4rrhh.support.AlmacenDeFotosEnMemoria.FotosEnMemoria;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La foto viaja siempre por la API (backend#177): se sube con un {@code PUT}, se lee con un {@code GET}
 * que no pide token pero sí la versión vigente, y en la base queda sólo la clave.
 *
 * <p>Con la aplicación entera, la seguridad de verdad y el esquema real; sólo el almacén va en memoria
 * (ver {@code AlmacenDeFotosEnMemoria}).
 */
@TestWebSobreEsquemaReal
class TheEmployeePhotoTravelsThroughTheApiEndToEndTest {

    private static final String RUTA = "/employees/ESP/INTERNAL/FOTO0001/photo";
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3, 4};
    private static final byte[] OTRO_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE1, 9, 9};

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private FotosEnMemoria almacen;

    @BeforeEach
    void anEmployeeWithoutPhoto() {
        jdbcTemplate.update("""
                insert into employee.employee (rule_system_code, employee_type_code, employee_number,
                                               first_name, last_name_1)
                values ('ESP', 'INTERNAL', 'FOTO0001', 'Ana', 'Lopez')
                """);
    }

    private String subir(byte[] jpeg) throws Exception {
        String cuerpo = mockMvc.perform(put(RUTA).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.IMAGE_JPEG).content(jpeg))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.photoUrl");
    }

    private String clave() {
        return jdbcTemplate.queryForObject(
                "select photo_url from employee.employee where employee_number = 'FOTO0001'", String.class);
    }

    private static String version(String enlace) {
        return enlace.substring(enlace.indexOf("?v=") + 3);
    }

    @Test
    void theRowKeepsTheKeyAndTheEmployeeGetsARelativeLinkOfTheApi() throws Exception {
        String enlace = subir(JPEG);

        assertThat(clave()).matches("photos/ESP/INTERNAL/FOTO0001/[0-9a-f-]{36}\\.jpg");
        assertThat(enlace).isEqualTo("/api/employees/ESP/INTERNAL/FOTO0001/photo?v=" + version(enlace));
        assertThat(clave()).endsWith(version(enlace) + ".jpg");
        assertThat(almacen.claves()).contains(clave());
    }

    @Test
    void anImgWithoutTokenGetsThePhotoWithTheCurrentVersionCachedForAYear() throws Exception {
        String enlace = subir(JPEG);

        mockMvc.perform(get(RUTA).param("v", version(enlace)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(JPEG))
                .andExpect(header().string("Cache-Control", "max-age=31536000, public, immutable"))
                .andExpect(header().string("ETag", "\"" + clave() + "\""));
    }

    @Test
    void withoutTheVersionOrWithAnotherOneThereIsNoPhoto() throws Exception {
        subir(JPEG);

        mockMvc.perform(get(RUTA)).andExpect(status().isNotFound());
        mockMvc.perform(get(RUTA).param("v", "11111111-2222-3333-4444-555555555555"))
                .andExpect(status().isNotFound());
    }

    @Test
    void aNewPhotoRetiresTheOldLinkAndTheOldObject() throws Exception {
        String vieja = subir(JPEG);
        String claveVieja = clave();

        String nueva = subir(OTRO_JPEG);

        assertThat(version(nueva)).isNotEqualTo(version(vieja));
        assertThat(almacen.claves()).doesNotContain(claveVieja).contains(clave());
        mockMvc.perform(get(RUTA).param("v", version(vieja))).andExpect(status().isNotFound());
        mockMvc.perform(get(RUTA).param("v", version(nueva))).andExpect(content().bytes(OTRO_JPEG));
    }

    @Test
    void deletingClearsTheRowTheObjectAndTheLink() throws Exception {
        String enlace = subir(JPEG);
        String claveBorrada = clave();

        mockMvc.perform(delete(RUTA).with(user("admin").roles("ADMIN"))).andExpect(status().isNoContent());

        assertThat(clave()).isNull();
        assertThat(almacen.claves()).doesNotContain(claveBorrada);
        mockMvc.perform(get(RUTA).param("v", version(enlace))).andExpect(status().isNotFound());
    }

    @Test
    void uploadingStillNeedsAnAdmin() throws Exception {
        mockMvc.perform(put(RUTA).contentType(MediaType.IMAGE_JPEG).content(JPEG))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(RUTA).with(user("lector").roles("READONLY"))
                        .contentType(MediaType.IMAGE_JPEG).content(JPEG))
                .andExpect(status().isForbidden());
        assertThat(clave()).isNull();
    }

    @Test
    void whatIsNotAJpegIsRefusedWith415() throws Exception {
        mockMvc.perform(put(RUTA).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.IMAGE_JPEG).content(new byte[] {(byte) 0x89, 'P', 'N', 'G', 1}))
                .andExpect(status().isUnsupportedMediaType());
        assertThat(clave()).isNull();
    }
}

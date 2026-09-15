package com.b4rrhh.payroll_engine.infrastructure.web;

import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * La sonda del {@code backend#78}: qué cuerpo sirve de verdad {@code /payroll-engine} cuando algo
 * falla.
 *
 * <p>Sonda y no lectura del código, porque el issue lo pide así y con razón: el
 * {@code backend#66} encontró el problema mirando respuestas reales, y una forma deducida de un
 * {@code @ExceptionHandler} no cuenta los dos sitios donde la forma la pone otra cosa —un
 * {@code Map.of} sin DTO, o Spring cuando no hay manejador ninguno—.
 *
 * <p>Lo que se cita aquí es el <b>cuerpo literal</b>, no un {@code jsonPath}: un {@code jsonPath}
 * sobre {@code $.message} pasa igual aunque al lado viajen otros tres campos, y lo que este issue
 * discute es precisamente cuántos campos hay y cómo se llaman.
 *
 * <p><b>Este test no aprueba la situación: la fija.</b> Mientras las tres formas sigan ahí, aquí
 * están escritas y nadie puede decir que el contrato de errores de {@code /payroll-engine} es uno.
 * Cuando el {@code backend#78} se ejecute entero, este test cambia con él y es el sitio donde se
 * verá que la unificación pasó de verdad.
 */
@TestWebSobreEsquemaReal
class PayrollEngineErrorShapeProbeTest {

    private static final String RULE_SYSTEM = "ESP";
    private static final String TABLA = "SB_99002405011982";

    @Autowired
    private MockMvc mockMvc;

    /** Primera forma: {@code {"error": …}}, la de las tablas. Sale de un {@code Map.of}, sin DTO. */
    @Test
    @WithMockUser(roles = "ADMIN")
    void lasTablasContestanConError() throws Exception {
        MvcResult resultado = mockMvc.perform(
                        delete("/payroll-engine/{rs}/tables/{t}/rows/{id}", RULE_SYSTEM, TABLA, 999_999_999L))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        assertThat(cuerpo(resultado))
                .startsWith("{\"error\":")
                .doesNotContain("\"message\"");
        System.out.println("[backend#78] tablas 404 -> " + cuerpo(resultado));
    }

    /**
     * Segunda forma: {@code {"message": …}}, la de los conceptos y casi todo lo demás del contexto.
     *
     * <p>Las dos salen del <b>mismo</b> {@code /payroll-engine} y en la misma petición podrían
     * alternarse: quien reciba un error de aquí no puede saber qué campo leer sin haber probado
     * antes el endpoint concreto. Eso es el issue entero.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void losConceptosContestanConMessage() throws Exception {
        MvcResult resultado = mockMvc.perform(
                        delete("/payroll-engine/{rs}/concepts/{c}", RULE_SYSTEM, "NO_EXISTE_ZZ"))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        assertThat(cuerpo(resultado))
                .startsWith("{\"message\":")
                .doesNotContain("\"error\"");
        System.out.println("[backend#78] conceptos 404 -> " + cuerpo(resultado));
    }

    /**
     * Tercera forma, y es la que no se puede documentar: <b>Bean Validation contesta 400 sin
     * cuerpo</b>.
     *
     * <p>No hay esquema que escribir para «no hay cuerpo», y escribirlo sería bendecir el fallo: el
     * cliente sabe que algo iba mal y no puede decirle al usuario qué. El {@code Content-Type} se
     * afirma vacío también, porque es la otra mitad del defecto.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void beanValidationContesta400SinCuerpoYSinTipo() throws Exception {
        MvcResult resultado = mockMvc.perform(
                        post("/payroll-engine/{rs}/tables/{t}/rows", RULE_SYSTEM, TABLA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(cuerpo(resultado))
                .as("un 400 sin cuerpo no se puede documentar, y por eso el backend#78 lo llama"
                        + " defecto y no hueco")
                .isEmpty();
        assertThat(resultado.getResponse().getContentType()).isNull();
        System.out.println("[backend#78] bean validation 400 -> cuerpo vacio, sin Content-Type");
    }

    /**
     * Y el precedente, que está al lado y no en {@code /payroll-engine}: {@code PayrollErrorResponse}
     * declara {@code (code, message, details)} con {@code NON_NULL} desde el {@code #100}, y por eso
     * un 404 de {@code /payrolls} sale <b>exactamente</b> como salía antes.
     *
     * <p>Esto es el dato que decide el {@code backend#78}, y por eso se comprueba aquí y no se
     * razona: {@code {code, message, details}} <b>no es una segunda forma frente a</b>
     * {@code {message}} — es la misma forma con sitio para más. Elegir la rica no obliga a nadie a
     * cambiar lo que ya lee {@code message}.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void laFormaRicaDePayrollsSaleComoLaPobreCuandoNoHayCodigo() throws Exception {
        MvcResult resultado = mockMvc.perform(
                        post("/payrolls/{rs}/{et}/{en}/{p}/{pt}/{pn}/recalculate",
                                RULE_SYSTEM, "INTERNAL", "NO_EXISTE", "202501", "NORMAL", 1))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        assertThat(cuerpo(resultado))
                .as("con code y details nulos, NON_NULL deja el cuerpo en {\"message\": ...}")
                .startsWith("{\"message\":")
                .doesNotContain("\"code\"")
                .doesNotContain("\"details\"");
        System.out.println("[backend#78] payrolls 404 (forma rica, sin codigo) -> " + cuerpo(resultado));
    }

    private String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString();
    }
}

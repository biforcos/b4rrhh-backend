package com.b4rrhh.payroll_engine.concept.infrastructure.web;

import com.b4rrhh.support.TestWebSobreEsquemaReal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El grafo en una llamada dice lo mismo que las {@code 1 + 2N} ({@code designer#15}).
 *
 * <h2>Que defiende</h2>
 *
 * <p>Dibujar el grafo costaba una peticion para la lista de conceptos y dos por concepto: 78
 * llamadas con los 38 de {@code ESP}, 200 con un catalogo de 99, y 198 de esas 200 devolviendo
 * entre cero y dos elementos. El extremo nuevo lo hace en una.
 *
 * <p>Lo que hay que probar no es que conteste: es que conteste <b>lo mismo</b>. Un extremo
 * agregado que se deje una arista es peor que las 200 peticiones, porque el grafo sale dibujado
 * —solo que incompleto— y nadie lo nota. Por eso el «esperado» de este test no es una lista
 * escrita a mano, que seria verde el dia que se escribe y dejaria de significar nada al
 * siguiente: es el resultado de recorrer el camino viejo, endpoint por endpoint, contra el mismo
 * catalogo. Es el patron del {@code ThePdfSaysTheSameAsTheFolioTest}, entre dos formas de
 * preguntar lo mismo.
 *
 * <p>Se prueba contra {@code ESP}, que es el catalogo real que traen las migraciones, y no contra
 * uno montado aqui: lo que interesa es que las dos formas coincidan sobre el grafo que existe,
 * con sus agregados, sus tablas como fuente y sus alimentaciones con fecha.
 */
@TestWebSobreEsquemaReal
class TheGraphInOneCallSaysTheSameAsTheNPlusOneTest {

    private static final String RULE_SYSTEM = "ESP";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void theOneCallAndTheNPlusOneReturnTheSameConceptsAndTheSameEdges() throws Exception {
        JsonNode grafo = pedir("/payroll-engine/" + RULE_SYSTEM + "/graph");

        assertEquals(RULE_SYSTEM, grafo.get("ruleSystemCode").asText());

        // --- los conceptos, tal cual los sirve el extremo de siempre --------------------
        JsonNode conceptos = pedir("/payroll-engine/" + RULE_SYSTEM + "/concepts");
        assertEquals(conceptos, grafo.get("concepts"),
                "el grafo tiene que traer exactamente los conceptos que sirve la lista, campo a "
                        + "campo: si aqui empiezan a ser dos cosas parecidas, el dibujo y el panel "
                        + "de detalle dejan de hablar del mismo concepto");

        int n = conceptos.size();
        assertTrue(n > 5, "el catalogo de ESP tiene que traer conceptos, o este test no mira nada: " + n);

        // --- las aristas, recorriendo el camino viejo ----------------------------------
        List<String> operandosEsperados = new ArrayList<>();
        List<String> alimentacionesEsperadas = new ArrayList<>();
        int llamadasDelCaminoViejo = 1;

        for (JsonNode concepto : conceptos) {
            String code = concepto.get("conceptCode").asText();

            for (JsonNode operando : pedir("/payroll-engine/" + RULE_SYSTEM + "/concepts/" + code + "/operands")) {
                operandosEsperados.add(String.join("|", code,
                        operando.get("operandRole").asText(),
                        operando.get("sourceObjectCode").asText()));
            }
            for (JsonNode feed : pedir("/payroll-engine/" + RULE_SYSTEM + "/concepts/" + code + "/feeds")) {
                alimentacionesEsperadas.add(String.join("|", code,
                        feed.get("sourceObjectCode").asText(),
                        feed.get("invertSign").asText(),
                        feed.get("effectiveFrom").asText(),
                        feed.get("effectiveTo").isNull() ? "-" : feed.get("effectiveTo").asText()));
            }
            llamadasDelCaminoViejo += 2;
        }

        assertEquals(1 + 2 * n, llamadasDelCaminoViejo,
                "el camino viejo cuesta 1 + 2N y el nuevo una. Con " + n + " conceptos son "
                        + llamadasDelCaminoViejo + " peticiones contra 1, y por eso existe el extremo");

        assertEquals(ordenado(operandosEsperados), ordenado(aristasDe(grafo, "operands",
                        "conceptCode", "operandRole", "sourceObjectCode")),
                "los operandos del grafo tienen que ser los de los N extremos, ni uno mas ni uno "
                        + "menos. Una arista que falte deja el dibujo incompleto sin que falle nada");

        assertEquals(ordenado(alimentacionesEsperadas), ordenado(aristasDe(grafo, "feeds",
                        "conceptCode", "sourceObjectCode", "invertSign", "effectiveFrom", "effectiveTo")),
                "y las alimentaciones igual, con sus fechas: el disenador dibuja lo declarado, no "
                        + "solo lo vigente, asi que filtrar por hoy aqui haria desaparecer aristas");

        // Y que la comparacion no sea dos listas vacias comparadas entre si.
        assertTrue(operandosEsperados.size() > 5 && !alimentacionesEsperadas.isEmpty(),
                "ESP tiene operandos y alimentaciones; si estas listas salen vacias, este test esta"
                        + " comparando nada con nada. Operandos: " + operandosEsperados.size()
                        + ", alimentaciones: " + alimentacionesEsperadas.size());
    }

    /**
     * Un sistema de reglas sin nada contesta un grafo vacio y no un 404.
     *
     * <p>Es lo que ve el disenador la primera vez que alguien abre una reglamentacion nueva, y
     * «no hay nada dibujado» es una respuesta. {@code PRT} esta sembrado y vacio a proposito.
     */
    @Test
    @WithMockUser
    void anEmptyRuleSystemAnswersAnEmptyGraph() throws Exception {
        JsonNode grafo = pedir("/payroll-engine/PRT/graph");

        assertEquals("PRT", grafo.get("ruleSystemCode").asText());
        assertEquals(0, grafo.get("concepts").size());
        assertEquals(0, grafo.get("operands").size());
        assertEquals(0, grafo.get("feeds").size());
    }

    // ---------------------------------------------------------------------

    private JsonNode pedir(String ruta) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get(ruta))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    /** Las aristas de una lista del grafo, aplanadas a texto para poder compararlas. */
    private static List<String> aristasDe(JsonNode grafo, String lista, String... campos) {
        List<String> aristas = new ArrayList<>();
        for (JsonNode arista : grafo.get(lista)) {
            List<String> partes = new ArrayList<>(campos.length);
            for (String campo : campos) {
                JsonNode valor = arista.get(campo);
                partes.add(valor == null || valor.isNull() ? "-" : valor.asText());
            }
            aristas.add(String.join("|", partes));
        }
        return aristas;
    }

    /** Ordenadas, porque lo que se compara es el conjunto de aristas y no en que orden vienen. */
    private static List<String> ordenado(List<String> aristas) {
        return List.copyOf(new TreeSet<>(aristas));
    }
}

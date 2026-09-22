package com.b4rrhh.payroll_engine.concept;

import com.b4rrhh.payroll_engine.concept.domain.model.CalculationType;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodel;
import com.b4rrhh.payroll_engine.metamodel.domain.port.RuleSystemMetamodelRepository;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ningun total suma el mismo concepto dos veces ({@code backend#120}).
 *
 * <h2>Lo que protege</h2>
 *
 * <p>Un total es la suma de lo que le alimenta. Si un concepto le llega <b>por dos caminos</b>
 * —directamente y ademas por dentro de otro alimentador— el total lo cuenta dos veces, y el
 * importe sale mal sin que nada falle: no hay excepcion, no hay ciclo, no hay concepto sin
 * calcular. Sale un numero, y es el numero equivocado.
 *
 * <p>Es lo que le pasaba al liquido. La {@code V133} declaro {@code 102 → 970} y
 * {@code 102 → 990}, y el {@code 970} ya iba al {@code 990}:
 *
 * <pre>
 *   990 = 101 + 2 x 102 - 980
 * </pre>
 *
 * <p>245 de los 863 recibos de la demo pagaban las horas extra dos veces en el liquido. La
 * {@code V77} habia retirado a proposito el {@code 101 → 990} al meter el {@code 970 → 990}
 * —«REMOVE: 101 → 990, replaced by 970 → 990»— y el {@code 102} entro despues repitiendo el
 * patron que aquella migracion acababa de quitar. <b>Nada lo dijo</b>: ni la corrida, ni el
 * folio, ni los tests de la cadena de horas extra, que comprobaban que el {@code 102} se
 * calcula y se imprime pero no a cuantos sitios va.
 *
 * <h2>Por que este test y no solo uno de importes</h2>
 *
 * <p>Un test de importes prueba un recibo; este prueba el catalogo.
 * {@code TheNetPayIsWhatIsEarnedMinusWhatIsDeductedTest} afirma la identidad sobre un recibo con
 * horas extra, que es donde el defecto se veia, pero solo del {@code 990} y solo de ese recibo.
 * Este dice lo mismo de <b>todos</b> los agregados del catalogo real, y es el que habria cantado
 * el dia que se escribio la {@code V133}.
 *
 * <h2>El borde: alimentaciones, no operandos</h2>
 *
 * <p>Solo se sigue la arista de alimentacion, que es la que suma. Un {@code PERCENTAGE} que lee
 * {@code B01} como operando <b>no</b> es un camino de doble conteo: el {@code 700} es un
 * porcentaje de la base, no una copia de ella, y que los dos acaben influyendo en el liquido es
 * la nomina y no un defecto. Lo que este test persigue es una cantidad que se suma dos veces en
 * el mismo sitio.
 */
@TestWebSobreEsquemaReal
class NoTotalAddsTheSameConceptTwiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 9, 30);

    @Autowired
    private RuleSystemMetamodelRepository reglamentaciones;

    /**
     * El catalogo real de {@code ESP}, que es el que calcula la demo.
     *
     * <p>Si esta lista deja de estar vacia no es que el test este mal: es que hay un total que
     * suma algo dos veces y hay recibos con un importe equivocado.
     */
    @Test
    void noAggregateInTheRealEspCatalogAddsTheSameConceptTwice() {
        RuleSystemMetamodel esp = reglamentaciones.load("ESP", HOY);

        assertEquals(List.of(), dobleContabilidadEn(esp),
                "ningun total del catalogo ESP puede sumar el mismo concepto por dos caminos");
    }

    /**
     * Y muerde: el grafo del liquido tal y como lo dejo la {@code V133} sale nombrado, con los
     * dos caminos escritos.
     *
     * <p>Sin esto, el criterio de arriba se pondria verde el dia que alguien rompiera el
     * recorrido del grafo y nadie se enteraria. Un guardian que no se ha visto fallar no guarda
     * nada.
     */
    @Test
    void aTotalThatAddsAConceptTwiceIsNamedWithBothPaths() {
        List<String> hallazgos =
                dobleContabilidad(elLiquidoComoLoDejoLaV133(), Set.of("970", "980", "990"));

        assertEquals(1, hallazgos.size(), "un hallazgo y uno solo: " + hallazgos);
        String hallazgo = hallazgos.get(0);
        assertTrue(hallazgo.contains("102 → 990"),
                "tiene que nombrar el camino directo: " + hallazgo);
        assertTrue(hallazgo.contains("102 → 970 → 990"),
                "y el camino por dentro del otro alimentador, o no se sabe que quitar: " + hallazgo);
    }

    /**
     * El mismo grafo sin la alimentacion que sobra no dice nada. Es la mitad que hace que el
     * criterio valga algo: un guardian que se queja siempre no se distingue de uno roto.
     */
    @Test
    void theSameGraphWithoutTheStrayFeedSaysNothing() {
        Map<String, List<String>> sano = elLiquidoComoLoDejoLaV133();
        sano.put("990", List.of("970", "980"));

        assertEquals(List.of(), dobleContabilidad(sano, Set.of("970", "980", "990")));
    }

    // ── el grafo del liquido, tal y como estaba ───────────────────────────────

    private static Map<String, List<String>> elLiquidoComoLoDejoLaV133() {
        Map<String, List<String>> fuentes = new LinkedHashMap<>();
        fuentes.put("970", List.of("101", "102"));
        fuentes.put("980", List.of("700", "800"));
        fuentes.put("990", List.of("102", "970", "980"));
        return fuentes;
    }

    // ── el criterio ───────────────────────────────────────────────────────────

    private static List<String> dobleContabilidadEn(RuleSystemMetamodel reglamentacion) {
        Map<String, List<String>> fuentes = new LinkedHashMap<>();
        Set<String> agregados = new LinkedHashSet<>();
        for (PayrollConcept concepto : reglamentacion.concepts()) {
            fuentes.put(concepto.getConceptCode(),
                    reglamentacion.activeFeedsOf(concepto.getObject().getId()).stream()
                            .map(feed -> feed.getSourceObject().getObjectCode())
                            .toList());
            if (concepto.getCalculationType() == CalculationType.AGGREGATE) {
                agregados.add(concepto.getConceptCode());
            }
        }
        return dobleContabilidad(fuentes, agregados);
    }

    /**
     * Para cada agregado, los alimentadores directos que ademas le llegan por dentro de otro
     * alimentador directo. Devuelve los dos caminos escritos, que es lo que hace falta para
     * saber cual de los dos sobra.
     */
    private static List<String> dobleContabilidad(
            Map<String, List<String>> fuentesPorDestino, Set<String> agregados) {

        List<String> hallazgos = new ArrayList<>();
        for (String agregado : agregados) {
            List<String> directas = fuentesPorDestino.getOrDefault(agregado, List.of());
            for (String fuente : new LinkedHashSet<>(directas)) {
                if (directas.indexOf(fuente) != directas.lastIndexOf(fuente)) {
                    hallazgos.add(agregado + " suma " + fuente + " dos veces: la alimentacion "
                            + fuente + " → " + agregado + " esta declarada dos veces");
                }
                for (String otra : directas) {
                    if (otra.equals(fuente)) {
                        continue;
                    }
                    List<String> camino = caminoHasta(fuente, otra, fuentesPorDestino);
                    if (camino != null) {
                        hallazgos.add(agregado + " suma " + fuente + " dos veces: "
                                + fuente + " → " + agregado + " (directo) y ademas "
                                + String.join(" → ", camino) + " → " + agregado);
                    }
                }
            }
        }
        return hallazgos;
    }

    /**
     * El camino de alimentaciones por el que {@code buscado} llega hasta {@code destino}, o
     * {@code null} si no llega. En anchura, para dar el mas corto: el mensaje se lee mejor y el
     * corte que hay que hacer es el mismo.
     */
    private static List<String> caminoHasta(
            String buscado, String destino, Map<String, List<String>> fuentesPorDestino) {

        Map<String, String> vengoDe = new LinkedHashMap<>();
        Deque<String> porVisitar = new ArrayDeque<>(List.of(destino));
        Set<String> vistos = new LinkedHashSet<>(List.of(destino));
        while (!porVisitar.isEmpty()) {
            String actual = porVisitar.removeFirst();
            for (String fuente : fuentesPorDestino.getOrDefault(actual, List.of())) {
                if (!vistos.add(fuente)) {
                    continue;
                }
                vengoDe.put(fuente, actual);
                if (fuente.equals(buscado)) {
                    List<String> camino = new ArrayList<>();
                    for (String paso = fuente; paso != null; paso = vengoDe.get(paso)) {
                        camino.add(paso);
                    }
                    return camino;
                }
                porVisitar.addLast(fuente);
            }
        }
        return null;
    }
}

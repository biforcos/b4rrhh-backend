package com.b4rrhh.payroll_engine.layer;

import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodel;
import com.b4rrhh.payroll_engine.metamodel.domain.port.RuleSystemMetamodelRepository;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Qué nodo del grafo es ley y qué nodo es empresa, y qué dependencias por nombre cruzan la línea
 * ({@code backend#160}, ADR-077 §7).
 *
 * <h2>Es un informe, no un candado</h2>
 *
 * <p>ADR-077 dice que la capa 4 es cerrada: un nodo de ley no nombra un nodo de empresa, agrega
 * por atributo. Hoy no es verdad —el motor sólo sabe alimentar por nombre ({@code FEED_BY_SOURCE}
 * es el único {@code FeedMode})— y no se arregla hasta que se cruce la puerta de partir el grafo
 * ({@code CAMINOS.md}). Lo que hace este test hasta entonces es <b>decir dónde no es verdad</b>:
 * imprime cada operando y cada alimentación que va de un nodo de ley a uno de empresa, y pasa.
 *
 * <p>El día que se parta el grafo, el candado es una línea: {@link #ES_CANDADO} a {@code true}.
 *
 * <h2>Lo que sí falla</h2>
 *
 * <p>Que un nodo del catálogo no tenga capa. Un informe que se salta los nodos que no conoce
 * presenta como «sin cruces» lo que no ha mirado, y eso es justo un fallo que parece un hecho. El
 * concepto nuevo se clasifica aquí en el mismo commit que lo crea.
 *
 * <h2>La capa es provisional</h2>
 *
 * <p>Es la del primer comentario del issue, nodo a nodo, y se puede discutir. No está en la base
 * porque todavía no decide nada: el grafo entero cuelga de la reglamentación.
 */
@TestWebSobreEsquemaReal
class WhereTheLawNamesACompanyNodeTest {

    /** La línea que convierte el informe en candado el día que se parta el grafo. */
    private static final boolean ES_CANDADO = false;

    private static final LocalDate HOY = LocalDate.of(2026, 9, 30);

    /** Capa 4: lo que es igual para cualquier empresa que cotice y retenga en España. */
    static final Set<String> LEY = Set.of(
            // cotización del trabajador y de la empresa, y su total
            "700", "701", "702", "703", "704",
            "720", "721", "722", "723", "724", "725", "726", "727",
            // IRPF
            "800",
            // prestación de incapacidad temporal
            "110", "111", "111_D60", "111_D75",
            "T_IT_E60", "T_IT_D60", "T_IT_D75",
            "BR_CC", "BR_ACT", "BR_ANT", "BR_TEO",
            "D_IT_0", "D_IT_C", "D_IT_E60", "D_IT_D60", "D_IT_D75",
            // bases de cotización y de retención
            "B01", "B02", "B03", "B04", "B05", "B06", "B07", "B08", "B09", "B10",
            "B_CC", "B_CC_MAX", "B_CP", "B_CP_MAX",
            // totales del recibo
            "970", "980", "990",
            // atrasos: los provee el motor, igual para cualquier esquema
            "A_DEV", "A_DED", "A_EMP",
            // tipos y topes: los leen las tablas de ley de la capa 4 (backend#159)
            "P_SS_CC", "P_SS_CC_EMP", "P_FP_TRAB", "P_SS_FP_EMP", "P_MEI_TRAB", "P_SS_MEI_EMP",
            "P_SS_DESEMPLEO", "P_SS_DESEMPLEO_EMP", "P_SS_FOGASA_EMP", "P_HE_TRAB", "P_HE_EMP",
            "P_AT_EP", "P_TOPE_MAX", "P_TOPE_MIN", "P_TOPE_MAX_CP", "P_TOPE_MIN_CP",
            "P_IRPF", "P_IT_E60", "P_IT_D60", "P_IT_D75",
            // calendario, jornada y régimen de pagas: hechos que el motor provee a cualquier esquema
            "D01", "D02", "D03", "J01", "J_PRORRATEADAS", "J_NO_PRORRATEADAS", "J_SIN_ANT",
            "P_MESES_ANO", "P_CERO");

    /** Capa 5: lo que decide un convenio o una empresa. */
    static final Set<String> EMPRESA = Set.of(
            // devengos salariales y lo que les da precio
            "101", "102", "103", "H01",
            "P01", "P02", "P02_DAILY_AMOUNT_TABLE", "P03", "P03_HOURLY_OVERTIME_TABLE",
            // pagas extraordinarias y su prorrata (backend#117: cuántas y cuánto es del convenio)
            "PE_1", "PE_2", "PE_3", "PE_4", "PE_TOTAL",
            "PE_1_DIA", "PE_2_DIA", "PE_3_DIA", "PE_4_DIA", "PE_TOTAL_DIA",
            "P_PRORRATA", "P_PRORRATA_DIA",
            // complemento de la baja hasta el cien por cien: mejora del convenio, no prestación
            "112", "IT_DIF", "IT_100");

    @Autowired
    private RuleSystemMetamodelRepository reglamentaciones;

    @Test
    void everyNodeOfTheEspGraphHasAProvisionalLayerAndOnlyOne() {
        RuleSystemMetamodel esp = reglamentaciones.load("ESP", HOY);

        Set<String> enLasDos = new TreeSet<>(LEY);
        enLasDos.retainAll(EMPRESA);
        assertEquals(Set.of(), enLasDos, "un nodo es de ley o de empresa, no de las dos");

        Set<String> sinCapa = new TreeSet<>(nodosDe(esp));
        sinCapa.removeAll(LEY);
        sinCapa.removeAll(EMPRESA);
        assertEquals(Set.of(), sinCapa,
                "un nodo sin capa es un nodo que el informe no mira: clasifícalo aquí");
    }

    @Test
    void reportsEveryDependencyByNameFromALawNodeToACompanyNode() {
        List<String> cruces = crucesEn(reglamentaciones.load("ESP", HOY));

        System.out.println("backend#160: dependencias por nombre de la ley (4) a la empresa (5): "
                + cruces.size());
        cruces.forEach(cruce -> System.out.println("  " + cruce));

        if (ES_CANDADO) {
            assertEquals(List.of(), cruces, "la capa 4 es cerrada: un nodo de ley no nombra uno de empresa");
        }
    }

    /**
     * Y el detector muerde: un total de ley que suma un devengo de empresa sale nombrado, por
     * alimentación, y una base de ley que lee un precio de empresa, por operando. Lo que va de la
     * empresa a la ley no es un cruce: la empresa puede leer la ley.
     */
    @Test
    void aLawNodeThatNamesACompanyNodeIsReportedAndTheOtherWayRoundIsNot() {
        Map<String, List<String>> operandos = Map.of(
                "B02", List.of("RATE=P_PRORRATA"),
                "101", List.of("QUANTITY=D01"));
        Map<String, List<String>> alimentaciones = Map.of(
                "970", List.of("101", "110"),
                "IT_DIF", List.of("110"));

        List<String> cruces = cruces(operandos, alimentaciones);

        assertEquals(2, cruces.size(), "dos cruces y sólo dos: " + cruces);
        assertTrue(cruces.contains("970 <- 101 (alimentacion)"), cruces.toString());
        assertTrue(cruces.contains("B02 <- P_PRORRATA (operando RATE)"), cruces.toString());
    }

    // ── el criterio ───────────────────────────────────────────────────────────

    private static Set<String> nodosDe(RuleSystemMetamodel reglamentacion) {
        Set<String> nodos = new TreeSet<>();
        for (PayrollConcept concepto : reglamentacion.concepts()) {
            nodos.add(concepto.getConceptCode());
            reglamentacion.operandsOf(concepto.getConceptCode())
                    .forEach(operando -> nodos.add(operando.getSourceObject().getObjectCode()));
            reglamentacion.activeFeedsOf(concepto.getObject().getId())
                    .forEach(feed -> nodos.add(feed.getSourceObject().getObjectCode()));
        }
        return nodos;
    }

    private static List<String> crucesEn(RuleSystemMetamodel reglamentacion) {
        Map<String, List<String>> operandos = new LinkedHashMap<>();
        Map<String, List<String>> alimentaciones = new LinkedHashMap<>();
        for (PayrollConcept concepto : reglamentacion.concepts()) {
            String codigo = concepto.getConceptCode();
            operandos.put(codigo, reglamentacion.operandsOf(codigo).stream()
                    .map(o -> o.getOperandRole() + "=" + o.getSourceObject().getObjectCode())
                    .toList());
            alimentaciones.put(codigo, reglamentacion.activeFeedsOf(concepto.getObject().getId()).stream()
                    .map(f -> f.getSourceObject().getObjectCode())
                    .toList());
        }
        return cruces(operandos, alimentaciones);
    }

    /**
     * Cada dependencia de un nodo de ley a uno de empresa, ordenada para que el informe se pueda
     * comparar entre corridas. Los operandos llegan como {@code ROL=FUENTE}.
     */
    private static List<String> cruces(
            Map<String, List<String>> operandosPorDestino,
            Map<String, List<String>> alimentacionesPorDestino) {

        Set<String> cruces = new TreeSet<>();
        operandosPorDestino.forEach((destino, operandos) -> {
            if (!LEY.contains(destino)) {
                return;
            }
            for (String operando : operandos) {
                String rol = operando.substring(0, operando.indexOf('='));
                String fuente = operando.substring(operando.indexOf('=') + 1);
                if (EMPRESA.contains(fuente)) {
                    cruces.add(destino + " <- " + fuente + " (operando " + rol + ")");
                }
            }
        });
        alimentacionesPorDestino.forEach((destino, fuentes) -> {
            if (!LEY.contains(destino)) {
                return;
            }
            for (String fuente : fuentes) {
                if (EMPRESA.contains(fuente)) {
                    cruces.add(destino + " <- " + fuente + " (alimentacion)");
                }
            }
        });
        return new ArrayList<>(cruces);
    }
}

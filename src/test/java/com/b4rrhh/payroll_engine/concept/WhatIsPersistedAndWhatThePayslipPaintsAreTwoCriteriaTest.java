package com.b4rrhh.payroll_engine.concept;

import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Lo que se persiste en el recibo y lo que el folio pinta son dos criterios distintos, y hasta
 * ahora no lo decía nada ({@code backend#94}).
 *
 * <h3>Qué separa a los dos</h3>
 *
 * <p>El reparto —qué concepto tiene sitio en el recibo— se hace por {@code payslip_order_code}:
 * lo tiene y es una línea, no lo tiene y es un paso de cálculo que se guarda pero no se imprime.
 * El folio, en cambio, <b>filtra por naturaleza</b>: {@code recibos-folio.component.ts} pinta
 * {@code EARNING} y {@code DEDUCTION} en el cuerpo y busca los tres totales uno a uno. Son dos
 * preguntas distintas con dos respuestas distintas, y hoy <b>no coinciden</b>: se persisten 15 y
 * se pintan 10.
 *
 * <p>Y desde el {@code backend#104} hay una tercera pregunta que no es ninguna de estas dos:
 * <b>cuantas lineas salen impresas en un recibo concreto</b>. Este censo es del catalogo —quien
 * PUEDE ser linea— y la regla del cero decide, recibo a recibo, quien lo es. El {@code 102}
 * (horas extra) cuenta aqui y no sale impreso en quien no tenga horas.
 *
 * <h3>Por qué esto es un test y no un {@code check} de esquema</h3>
 *
 * <p>La salida obvia era un bicondicional en {@code payroll_engine.payroll_concept}: orden de
 * recibo si y sólo si la naturaleza es de las que el folio pinta. Se descartó, y el motivo es el
 * mismo que hace falta leer antes de tocar este test.
 *
 * <p><b>Un {@code check} convierte un estado en una ley.</b> Prohibiría para siempre que un
 * concepto {@code BASE} tenga sitio en el recibo, y una nómina española de verdad lleva abajo el
 * <b>bloque de determinación de las bases de cotización</b> —base de contingencias comunes, base
 * de AT y EP, base sujeta a IRPF—. El día que el folio lo tenga, habría que tirar la restricción
 * con una migración y con los datos ya moldeados alrededor de ella. Es barato de poner y caro de
 * quitar, y no se pone un candado sobre una puerta por la que sabemos que vamos a querer pasar.
 *
 * <p>Y el peligro que lo justificaba está sobrevalorado: si mañana un {@code TECHNICAL} recibe
 * orden de recibo, se persiste y el folio sigue sin pintarlo. No sale una línea de más.
 *
 * <h3>Entonces, ¿cuándo es legítimo romper este test?</h3>
 *
 * <p><b>Cuando el folio empiece a pintar el bloque de bases de cotización</b>, o la aportación
 * empresarial a la Seguridad Social, que es el otro recuadro que a una nómina de verdad le falta.
 * Ese día un {@code BASE} llevará orden de recibo, o los cinco {@code INFORMATIONAL} del 720 al
 * 724 dejarán de ser la divergencia conocida, y este test se pondrá rojo con razón. Lo que hay
 * que hacer entonces es <b>actualizar el censo</b>, no borrarlo: quien lo rompa tiene que
 * encontrarse aquí con el motivo por el que puede tener derecho a romperlo.
 *
 * <h3>La otra mitad vive en el frontend</h3>
 *
 * <p>Este test lleva el censo: qué naturalezas llevan orden de recibo y cuántos conceptos de cada
 * una. Lo que el folio pinta lo fija su propio test, en {@code b4rrhh/frontend}
 * ({@code recibos-folio-censo.component.spec.ts}), porque ese hecho vive allí y un test de aquí
 * no puede verlo. {@link #PAINTED_BY_THE_PAYSLIP} es una copia de esa lista, y el test de allí es
 * lo que la mantiene cierta.
 */
@TestSobreEsquemaReal
class WhatIsPersistedAndWhatThePayslipPaintsAreTwoCriteriaTest {

    private static final String RULE_SYSTEM_CODE = "ESP";

    /**
     * El censo de hoy: naturaleza de los conceptos que llevan {@code payslip_order_code}, y
     * cuántos hay de cada una. Con números y no con adjetivos, que es lo que hace que un cambio
     * se vea.
     */
    private static final Map<String, Integer> PERSISTED_WITH_A_PAYSLIP_ORDER = new LinkedHashMap<>();

    static {
        PERSISTED_WITH_A_PAYSLIP_ORDER.put("DEDUCTION", 5);        // 700, 701, 702, 703, 800
        PERSISTED_WITH_A_PAYSLIP_ORDER.put("EARNING", 2);          // 101, 102
        PERSISTED_WITH_A_PAYSLIP_ORDER.put("INFORMATIONAL", 5);    // 720 a 724
        PERSISTED_WITH_A_PAYSLIP_ORDER.put("NET_PAY", 1);          // 990
        PERSISTED_WITH_A_PAYSLIP_ORDER.put("TOTAL_DEDUCTION", 1);  // 980
        PERSISTED_WITH_A_PAYSLIP_ORDER.put("TOTAL_EARNING", 1);    // 970
    }

    /**
     * Las cinco naturalezas que el folio pinta, de las ocho que hay.
     *
     * <p>Copia de un hecho que vive en {@code b4rrhh/frontend}, en
     * {@code recibos-folio.component.ts}: {@code bodyConcepts} filtra por {@code EARNING} y
     * {@code DEDUCTION}, y {@code netPayConcept}, {@code totalEarningConcept} y
     * {@code totalDeductionConcept} buscan uno de cada. Si esta copia se queda atrás, quien lo
     * dirá es el test de allí; aquí sirve para poder nombrar la divergencia.
     */
    private static final List<String> PAINTED_BY_THE_PAYSLIP =
            List.of("EARNING", "DEDUCTION", "TOTAL_EARNING", "TOTAL_DEDUCTION", "NET_PAY");

    /** Las que se persisten en el recibo sin que el folio las pinte, y por qué se acepta. */
    private static final List<String> PERSISTED_BUT_NOT_PAINTED = List.of("INFORMATIONAL");

    /** Los cinco de la aportación empresarial, que son la divergencia entera. */
    private static final List<String> THE_KNOWN_DIVERGENCE =
            List.of("720", "721", "722", "723", "724");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void theCensusOfWhatCarriesAPayslipOrderIsTheOneWrittenHere() {
        assertEquals(PERSISTED_WITH_A_PAYSLIP_ORDER, censusByNature(),
                """
                El reparto de conceptos con orden de recibo ha cambiado.

                Este censo dice QUE SE PERSISTE en el recibo, que no es lo mismo que QUE PINTA \
                EL FOLIO: el reparto va por payslip_order_code y el folio filtra por naturaleza. \
                Que este numero se mueva no es un fallo: es que alguien ha cambiado uno de los \
                dos criterios, y lo que hace falta es que se sepa cual.

                Si has dado orden de recibo a un concepto nuevo, actualiza el censo de arriba y \
                mira si el folio lo va a pintar: si su naturaleza no esta en PAINTED_BY_THE_PAYSLIP, \
                se persistira y no saldra impreso, y eso puede estar bien (es lo que pasa con los \
                cinco de la aportacion empresarial) o puede ser justo lo que se te ha olvidado.""");
    }

    /**
     * Ni {@code BASE} ni {@code TECHNICAL} llevan orden de recibo, y el día que uno lo lleve
     * será, casi seguro, el bloque de determinación de las bases de cotización.
     *
     * <p>Es el hecho del que depende el reparto entero del {@code backend#93}: los 21 pasos que
     * el motor calcula y no imprime son exactamente estas dos naturalezas. Si una de ellas
     * empieza a llevar orden de recibo, el reparto sigue funcionando —se persiste y el folio no
     * lo pinta— pero deja de ser verdad que «lo que se calcula y no se imprime son los BASE y los
     * TECHNICAL», y eso hay que saberlo.
     *
     * <p>Y hay una razón buena para que ese día llegue: una nómina española lleva al pie la base
     * de contingencias comunes, la de AT y EP y la sujeta a IRPF. Son conceptos {@code BASE} con
     * sitio en el folio. Si estás aquí porque has puesto eso, este test te está diciendo que has
     * cruzado la frontera a propósito — actualiza el censo y sigue.
     */
    @Test
    void neitherBaseNorTechnicalCarriesAPayslipOrderYet_andTheDayOneDoesItIsTheContributionBasesBlock() {
        assertEquals(Map.of(), censusByNature("BASE", "TECHNICAL"),
                """
                Un concepto BASE o TECHNICAL ha recibido orden de recibo.

                No esta prohibido y no sale ninguna linea de mas: el folio filtra por naturaleza \
                y seguira sin pintarlo. Pero es el hecho del que depende el reparto del \
                backend#93 —lo que el motor calcula y no imprime son los BASE y los TECHNICAL— y \
                deja de ser verdad en cuanto esto pase.

                El caso que hace esto LEGITIMO es el bloque de determinacion de las bases de \
                cotizacion: base de contingencias comunes, base de AT y EP, base sujeta a IRPF. \
                Una nomina de verdad lo lleva al pie. Si es eso lo que estas poniendo, actualiza \
                el censo de este test y el folio en b4rrhh/frontend. Si no lo es, lo mas probable \
                es que le hayas dado orden de recibo a un concepto tecnico sin querer.""");
    }

    /**
     * La divergencia conocida —los cinco {@code INFORMATIONAL} del 720 al 724— es la única.
     *
     * <p>Son la aportación empresarial a la Seguridad Social: se persisten bien y el folio hace
     * bien en no pintarlos, porque en una nómina de verdad van en su propio recuadro al pie y no
     * entre las líneas. No están mal y no hay que «arreglarlos».
     *
     * <p>Lo que este test impide es que aparezca <b>una sexta naturaleza</b> persistida y no
     * pintada sin que nadie se entere. Ése es el único caso en el que la diferencia entre los dos
     * criterios pasa de ser una decisión de maquetación a ser un descuido.
     */
    @Test
    void theOnlyNaturePersistedAndNotPaintedIsTheEmployerContribution_andItIsTheseFiveConcepts() {
        List<String> persistedButNotPainted = censusByNature().keySet().stream()
                .filter(nature -> !PAINTED_BY_THE_PAYSLIP.contains(nature))
                .toList();

        assertEquals(PERSISTED_BUT_NOT_PAINTED, persistedButNotPainted,
                """
                Ha aparecido una naturaleza que se persiste en el recibo y el folio no pinta, \
                ademas de la que ya se conocia.

                La conocida es INFORMATIONAL: los cinco de la aportacion empresarial a la \
                Seguridad Social, que van en su propio recuadro al pie y no entre las lineas. Esa \
                esta bien y no hay que tocarla.

                Una sexta es otra cosa. O el folio tiene que aprender a pintarla (y eso es un \
                issue de b4rrhh/frontend y una decision de maquetacion) o le has dado orden de \
                recibo a algo que no va al recibo. Lo que no vale es dejarla sin decidir.""");

        assertEquals(THE_KNOWN_DIVERGENCE, conceptsWithPayslipOrderOfNature("INFORMATIONAL"),
                """
                Los conceptos INFORMATIONAL con orden de recibo ya no son los cinco de la \
                aportacion empresarial (720 a 724).

                Si has anadido uno, comprueba que de verdad es aportacion de empresa y que el \
                folio no tiene que pintarlo. Si has quitado uno, el recuadro del pie que algun \
                dia los ensene tendra un hueco.""");
    }

    /**
     * Y las dos particiones, puestas una al lado de la otra: se persisten 15 y se pintan 10.
     *
     * <p>No es una comprobación distinta de las de arriba —sale de sumarlas— pero es el número
     * que resume de qué va este issue, y el que hay que poder decir en voz alta: cinco
     * naturalezas de ocho se pintan, y cinco conceptos de los quince persistidos no llegan al
     * papel.
     *
     * <p>Eran catorce y nueve hasta el {@code backend#104}, que declaró las horas extra: el
     * {@code 102} es {@code EARNING} y lleva orden de recibo, así que entra en los dos lados.
     * <b>Pintable no es impreso</b>: en un recibo sin horas vale cero y la regla del cero no lo
     * imprime. Este censo no lo sabe ni tiene por qué saberlo — habla del catálogo.
     */
    @Test
    void fifteenAreKeptAndTenArePainted() {
        int persisted = censusByNature().values().stream().mapToInt(Integer::intValue).sum();
        int painted = censusByNature().entrySet().stream()
                .filter(entry -> PAINTED_BY_THE_PAYSLIP.contains(entry.getKey()))
                .mapToInt(Map.Entry::getValue)
                .sum();

        assertEquals(15, persisted, "conceptos con sitio en el recibo");
        assertEquals(10, painted,
                """
                Los conceptos que el folio pinta han dejado de ser diez.

                Persistidos y pintados son dos criterios distintos y por eso este numero no tiene \
                por que ser el de arriba. Si ha subido, el folio pinta mas cosas y probablemente \
                sea el bloque de bases o la aportacion empresarial: bienvenido, actualiza el \
                censo. Si ha bajado, algo que se imprimia ha dejado de imprimirse.""");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Cuántos conceptos con orden de recibo hay de cada naturaleza, ordenado por naturaleza. */
    private Map<String, Integer> censusByNature(String... onlyTheseNatures) {
        Map<String, Integer> census = new TreeMap<>();
        String filter = onlyTheseNatures.length == 0
                ? ""
                : " and c.functional_nature in ('" + String.join("','", onlyTheseNatures) + "')";
        jdbc.queryForList(
                        "select c.functional_nature, count(*) as n"
                                + " from payroll_engine.payroll_concept c"
                                + " join payroll_engine.payroll_object o on o.id = c.object_id"
                                + " where o.rule_system_code = ? and c.payslip_order_code is not null"
                                + filter
                                + " group by c.functional_nature",
                        RULE_SYSTEM_CODE)
                .forEach(row -> census.put(
                        (String) row.get("functional_nature"),
                        ((Number) row.get("n")).intValue()));
        return census;
    }

    private List<String> conceptsWithPayslipOrderOfNature(String nature) {
        return jdbc.queryForList(
                "select c.payslip_order_code from payroll_engine.payroll_concept c"
                        + " join payroll_engine.payroll_object o on o.id = c.object_id"
                        + " where o.rule_system_code = ? and c.payslip_order_code is not null"
                        + "   and c.functional_nature = ?"
                        + " order by c.payslip_order_code",
                String.class, RULE_SYSTEM_CODE, nature);
    }
}

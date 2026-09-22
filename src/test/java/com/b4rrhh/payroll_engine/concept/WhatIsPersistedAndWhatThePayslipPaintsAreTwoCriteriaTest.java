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
 * El folio, en cambio, coloca cada línea en el bloque que declara su sección.
 *
 * <p><b>Hasta el {@code b4rrhh/frontend#76} el folio filtraba por naturaleza</b>, y los dos
 * criterios no coincidían: se persistían 15 y se pintaban 10. Los cinco que faltaban eran la
 * aportación empresarial, y esto lo llamaba «la divergencia conocida» dándola por buena. No lo
 * era: el recibo omitía un bloque del modelo oficial y parecía completo. Hoy los dos números son
 * 15, pero siguen siendo dos criterios y por eso este censo sigue haciendo falta.
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
 * <p>La mitad de esta respuesta ya se gastó: la aportación empresarial dejó de ser la divergencia
 * conocida en el {@code b4rrhh/frontend#76}, y el censo se actualizó en vez de borrarse, que es
 * lo que esta sección mandaba hacer.
 *
 * <p>Queda la otra: <b>cuando el folio empiece a pintar el bloque de bases de cotización</b> —base
 * de contingencias comunes, de AT y EP, y sujeta a IRPF—. Ese día un {@code BASE} llevará orden
 * de recibo y este test se pondrá rojo con razón. La sección {@code BASES} ya está declarada
 * desde la {@code V138} y el folio ya sabría colocarla: lo que falta es que esos conceptos
 * tengan sitio en el recibo. Lo que hay que hacer entonces es <b>actualizar el censo</b>, no
 * borrarlo: quien lo rompa tiene que encontrarse aquí con el motivo por el que puede tener
 * derecho a romperlo.
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
        // Los diez del recuadro, en los cuatro bloques del modelo oficial (V148):
        //   1. comunes        B03, B04, B01, B_CC
        //   2. profesionales  B05, B06, B07, B_CP
        //   3. horas extra    B08
        //   4. base del IRPF  B09
        PERSISTED_WITH_A_PAYSLIP_ORDER.put("BASE", 10);
        PERSISTED_WITH_A_PAYSLIP_ORDER.put("DEDUCTION", 6);        // 700, 701, 702, 703, 704, 800
        PERSISTED_WITH_A_PAYSLIP_ORDER.put("EARNING", 3);          // 101, 102, 103
        PERSISTED_WITH_A_PAYSLIP_ORDER.put("INFORMATIONAL", 7);    // 720 a 724, 726 y 727
        PERSISTED_WITH_A_PAYSLIP_ORDER.put("NET_PAY", 1);          // 990
        PERSISTED_WITH_A_PAYSLIP_ORDER.put("TOTAL_DEDUCTION", 1);  // 980
        PERSISTED_WITH_A_PAYSLIP_ORDER.put("TOTAL_EARNING", 1);    // 970
        PERSISTED_WITH_A_PAYSLIP_ORDER.put("TOTAL_EMPLOYER_CONTRIBUTION", 1);  // 725 (V141)
    }

    /**
     * Las naturalezas que el folio pinta: <b>todas las que tienen bloque declarado</b>
     * ({@code b4rrhh/frontend#76}).
     *
     * <p>Copia de un hecho que vive en {@code b4rrhh/frontend}, en
     * {@code recibos-folio.component.ts}. <b>Era una lista de cinco</b> —{@code bodyConcepts}
     * filtraba por {@code EARNING} y {@code DEDUCTION} y tres getters buscaban un total de cada
     * clase— y esa lista era el defecto: dejaba fuera {@code INFORMATIONAL}, o sea las cinco
     * líneas de aportación empresarial, que se calculaban y no se pintaban.
     *
     * <p>Ahora el folio no filtra por naturaleza: coloca cada línea por su
     * {@code payslip_section_code}, que declara la {@code V138}. Así que esta copia ya no es una
     * lista de naturalezas admitidas, es la de las que tienen sección — y coincide exactamente
     * con las que llegan al recibo. Si esta copia se queda atrás, quien lo dirá es el test de
     * allí.
     */
    private static final List<String> PAINTED_BY_THE_PAYSLIP = List.of(
            "EARNING", "DEDUCTION", "INFORMATIONAL", "BASE",
            "TOTAL_EARNING", "TOTAL_DEDUCTION", "TOTAL_EMPLOYER_CONTRIBUTION", "NET_PAY");

    /**
     * Las que se persisten en el recibo sin que el folio las pinte: <b>ninguna</b>.
     *
     * <p>Hasta el {@code b4rrhh/frontend#76} había una, {@code INFORMATIONAL}, y estaba escrita
     * aquí como «la divergencia conocida» y aceptada como una decisión de maquetación pendiente.
     * No lo era: era que el recibo omitía un bloque del modelo oficial.
     *
     * <p>Que esta lista esté vacía es lo que hay que defender. Una naturaleza que vuelva a
     * aparecer aquí es una que llega al recibo y no se ve, que es exactamente como se perdieron
     * las cinco anteriores.
     */
    private static final List<String> PERSISTED_BUT_NOT_PAINTED = List.of();

    /** Los siete de la aportación empresarial, que son la divergencia entera. */
    private static final List<String> THE_KNOWN_DIVERGENCE =
            List.of("720", "721", "722", "723", "724", "726", "727");

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
     * Ese día llegó: el bloque de bases de cotización se imprime desde la {@code V139}
     * ({@code backend#111}).
     *
     * <p>Este test decía que ni {@code BASE} ni {@code TECHNICAL} llevaban orden de recibo, y que
     * el día que uno lo llevara sería casi seguro el bloque de bases. Lo fue. Lo que vigila ahora
     * es lo que aquella frase escondía: <b>cuáles de los {@code BASE} se imprimen</b>, porque no
     * son todos y la diferencia no se ve mirando la naturaleza.
     *
     * <p>Desde el {@code backend#121} se imprimen <b>diez</b>, los del recuadro del modelo
     * oficial, y están elegidos uno a uno: cuatro del bloque de contingencias comunes
     * ({@code B03}, {@code B04}, {@code B01}, {@code B_CC}), cuatro del de profesionales
     * ({@code B05}, {@code B06}, {@code B07}, {@code B_CP}), la base de horas extraordinarias
     * ({@code B08}) y la sujeta a retención del IRPF ({@code B09}).
     *
     * <p>No se imprimen los dos pasos intermedios de los topes —{@code B_CC_MAX} y
     * {@code B_CP_MAX}—, porque entre una base y su base topada no hay nada que un técnico tenga
     * que ver; ni {@code B02}, que desde la {@code V148} sale sumada con el {@code 103} dentro
     * del {@code B04}; ni {@code P01}, {@code P02} y {@code P03}, que son <b>precios</b> y llevan
     * naturaleza {@code BASE} sólo porque el motor los usa como operando {@code BASE}. Un
     * undécimo concepto en esta lista es, casi seguro, uno de esos precios colado en el recuadro.
     *
     * <p>{@code TECHNICAL} sigue sin llevar ninguno, y eso no ha cambiado: un concepto técnico no
     * va al papel.
     */
    @Test
    void onlyTheBasesOfTheOfficialBlockArePrinted_andNoTechnicalConceptIs() {
        assertEquals(Map.of("BASE", 10), censusByNature("BASE", "TECHNICAL"),
                """
                Ha cambiado que conceptos BASE o TECHNICAL llevan orden de recibo.

                Del recuadro de bases se imprimen DIEZ y estan elegidos uno a uno: los cuatro de \
                contingencias comunes (B03, B04, B01, B_CC), los cuatro de profesionales (B05, \
                B06, B07, B_CP), la base de horas extraordinarias (B08) y la sujeta a retencion \
                del IRPF (B09).

                Si han subido a once o mas, mira si lo que has anadido es una base de verdad o \
                uno de los precios: P01, P02 y P03 llevan naturaleza BASE porque el motor los usa \
                como operando BASE, y no son bases de cotizacion. B_CC_MAX y B_CP_MAX tampoco: \
                son los pasos intermedios de los topes y en el papel no hay nada entre una base y \
                su base topada.

                Si aparece un TECHNICAL, lo mas probable es que le hayas dado orden de recibo a \
                un concepto tecnico sin querer.""");

        // La decena 4xx es la del recuadro de bases; la unidad dice en que bloque va la linea y
        // en que sitio del bloque. Sale del payslip_order_code y no del codigo del concepto, que
        // aqui empieza por letra y no ordena.
        assertEquals(
                List.of("401", "402", "403", "404", "411", "412", "413", "414", "421", "431"),
                conceptsWithPayslipOrderOfNature("BASE"),
                """
                Los ordenes de recibo de los BASE ya no son los diez del modelo oficial (V148):

                  401 B03  Remuneracion mensual          411 B05  Base de contingencias comunes
                  402 B04  Prorrata de pagas extras      412 B06  Horas extraordinarias
                  403 B01  Base de cotizacion            413 B07  Base de cotizacion
                  404 B_CC Base tras topes               414 B_CP Base tras topes
                  421 B08  Base                          431 B09  Base

                Cada bloque se lee de arriba abajo y cierra con la base sobre la que se cotiza de \
                verdad. Si estos numeros se mueven, el recuadro deja de leerse como una suma.""");
    }

    /**
     * Ya no hay divergencia: <b>todo lo que llega al recibo se pinta</b>
     * ({@code b4rrhh/frontend#76}).
     *
     * <p>La había, y eran los cinco {@code INFORMATIONAL} del 720 al 724 —la aportación
     * empresarial—, aceptados aquí como una decisión de maquetación pendiente. Lo que escondían
     * era peor: se calculaban bien, viajaban en la respuesta y un {@code if} del cliente los
     * tiraba, así que el recibo omitía un bloque entero del modelo oficial pareciendo completo.
     *
     * <p>Lo que este test impide ahora es que vuelva a aparecer <b>cualquier</b> naturaleza
     * persistida y no pintada. No hay ninguna que esté bien: el folio coloca por sección
     * declarada, así que una naturaleza sin pintar es una a la que se le olvidó declararle
     * bloque en la {@code V138}.
     */
    @Test
    void everyNatureThatReachesThePayslipIsPainted_thereIsNoDivergenceLeft() {
        List<String> persistedButNotPainted = censusByNature().keySet().stream()
                .filter(nature -> !PAINTED_BY_THE_PAYSLIP.contains(nature))
                .toList();

        assertEquals(PERSISTED_BUT_NOT_PAINTED, persistedButNotPainted,
                """
                Hay una naturaleza que se persiste en el recibo y el folio no pinta.

                Hasta el b4rrhh/frontend#76 habia una aceptada —INFORMATIONAL, la aportacion \
                empresarial— y resulto ser un bloque del modelo oficial que el recibo omitia. Ya \
                no hay ninguna que este bien.

                El folio coloca cada linea por su payslip_section_code. Si una naturaleza no se \
                pinta es que no tiene seccion declarada: decláresela en una migracion nueva, o \
                quitale el orden de recibo si de verdad no va al recibo.""");

        assertEquals(THE_KNOWN_DIVERGENCE, conceptsWithPayslipOrderOfNature("INFORMATIONAL"),
                """
                Los conceptos INFORMATIONAL con orden de recibo ya no son los siete de la \
                aportacion empresarial: 720 a 724, el 726 (cotizacion adicional por horas \
                extraordinarias) y el 727 (accidentes de trabajo y enfermedades profesionales).

                Si has anadido uno, comprueba que de verdad es aportacion de empresa y que el \
                folio no tiene que pintarlo. Si has quitado uno, el recuadro del pie que algun \
                dia los ensene tendra un hueco.""");
    }

    /**
     * Y las dos particiones, puestas una al lado de la otra: se persisten 30 y se pintan
     * <b>30</b>.
     *
     * <p>Eran 15 y 10 hasta el {@code b4rrhh/frontend#76}: los cinco que faltaban eran la
     * aportación empresarial, y que los dos números coincidan es el resultado de aquel issue —lo
     * que tiene sitio en el recibo sale impreso—. Y 15 y 15 hasta el {@code backend#111}, que
     * imprimió el recuadro de bases y añadió {@code B_CC} y {@code B01} a los dos lados a la vez.
     *
     * <p><b>Que coincidan no hace el test redundante.</b> Siguen siendo dos criterios distintos
     * —el sitio lo da {@code payslip_order_code} y el bloque lo da la sección declarada— y pueden
     * volver a separarse en cuanto alguien dé orden de recibo a una naturaleza sin sección. El
     * día que estos dos números dejen de ser el mismo, hay algo que se imprime menos.
     *
     * <p>Eran catorce y nueve hasta el {@code backend#104}, que declaró las horas extra: el
     * {@code 102} es {@code EARNING} y lleva orden de recibo, así que entra en los dos lados. Y
     * eran diecisiete hasta el {@code backend#114}, que le dio total propio al recuadro de
     * aportación empresarial: el {@code 725}, con naturaleza nueva y en el bloque que cierra.
     * Y eran dieciocho hasta el {@code backend#119}, que añadió las <b>dos puertas</b> de la
     * prorrata de pagas extras: el {@code 103} entre los devengos y el {@code B02} en el
     * recuadro de bases.
     *
     * <p>Y eran veinte hasta el {@code backend#121}, que puso las <b>tres bases</b> del modelo
     * oficial: siete líneas nuevas en el recuadro —y el {@code B02} sale de él, porque entra
     * sumado con el {@code 103} dentro del {@code B04}— más las dos cuotas de la cotización
     * adicional por horas extraordinarias, el {@code 704} del trabajador y el {@code 726} de la
     * empresa.
     *
     * <b>Pintable no es impreso</b>: en un recibo sin horas el {@code 102} vale cero y la regla
     * del cero no lo imprime, y de las dos puertas de la prorrata siempre hay una que vale cero
     * y tampoco sale. Este censo no lo sabe ni tiene por qué saberlo — habla del catálogo.
     */
    @Test
    void thirtyAreKeptAndThirtyArePainted() {
        int persisted = censusByNature().values().stream().mapToInt(Integer::intValue).sum();
        int painted = censusByNature().entrySet().stream()
                .filter(entry -> PAINTED_BY_THE_PAYSLIP.contains(entry.getKey()))
                .mapToInt(Map.Entry::getValue)
                .sum();

        assertEquals(30, persisted, "conceptos con sitio en el recibo");
        assertEquals(30, painted,
                """
                Los conceptos que el folio pinta han dejado de ser treinta.

                Persistidos y pintados siguen siendo dos criterios distintos —el sitio lo da \
                payslip_order_code y el bloque lo da la seccion declarada en la V138— y desde el \
                b4rrhh/frontend#76 dan el mismo numero porque todo lo que tiene sitio tiene \
                bloque.

                Si ha bajado, hay algo con orden de recibo cuya naturaleza no tiene seccion \
                declarada: se guarda y no se imprime, que es como se perdieron los cinco de la \
                aportacion empresarial durante meses.""");
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

package com.b4rrhh.payroll_engine.concept;

import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lo que el convenio sabe de las pagas extras, puesto en el sistema de reglas ({@code backend#117},
 * primer tercio del paso 4 de {@code b4rrhh/workspace#9}).
 *
 * <h2>Las dos cosas que no estaban en ningun sitio</h2>
 *
 * <p><b>Cuantas pagas extras tiene el convenio y de que se componen.</b> El de la demo —grandes
 * almacenes, BOE-A-2023-13740, art. 23— distribuye las retribuciones en dieciseis pagas, cuatro de
 * ellas extraordinarias, compuestas de salario base de grupo y complementos. Con lo que el modelo
 * tiene hoy, cada una es un {@code SALARIO_BASE} mensual, asi que son cuatro agregados del motor
 * alimentados por el {@code 101}.
 *
 * <p><b>Si por defecto se prorratean.</b> Un testigo en {@code rulesystem.agreement_profile}. El
 * convenio permite el prorrateo <i>por acuerdo</i>, o sea que su valor por omision es que NO se
 * prorratean: las extras se pagan en su mes. Ese testigo no decide ningun recibo — lo que decide
 * el recibo es la vertical del empleado ({@code backend#118}), y este testigo es lo que la
 * contratacion copia cuando nadie dice otra cosa.
 *
 * <h2>Por que agregados del motor y no filas de una tabla nueva</h2>
 *
 * <p>Porque el designer ya sabe ensenarlos y editarlos (ADR-067): un agregado con sus
 * alimentadores es exactamente lo que el grafo pinta. Una «paga de beneficios II» es un agregado
 * mas, no una columna mas.
 *
 * <h2>Lo que este issue NO hace, y este test lo sujeta</h2>
 *
 * <p>Las cuatro pagas <b>no alimentan a nadie</b> y <b>no se imprimen</b>. Son bases intermedias,
 * como {@code P01}. Quien las consume es el {@code backend#119}, y hasta entonces ningun recibo
 * puede moverse: un concepto que no alimenta a nadie no puede cambiar un importe. Eso es lo que
 * convierte «cero recibos se mueven» en una afirmacion comprobable y no en una esperanza.
 */
@TestSobreEsquemaReal
class TheAgreementSaysItsExtraPaymentsAndWhetherTheyAreProratedTest {

    /** El convenio de la demo. */
    private static final String CONVENIO = "99002405011982";

    /** Las cuatro del articulo 23. */
    private static final List<String> LAS_CUATRO_PAGAS = List.of("PE_1", "PE_2", "PE_3", "PE_4");

    @Autowired
    private JdbcTemplate jdbc;

    // ─────────────────────────────────────────────────────────────────────────
    // 1. El testigo del convenio
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * El convenio de grandes almacenes permite el prorrateo por acuerdo, asi que por omision no
     * prorratea. Es un valor concreto y no un {@code null}: un testigo que pueda faltar obligaria a
     * la contratacion a decidir que hacer cuando falta, y eso es una segunda regla escondida.
     */
    @Test
    void theAgreementCarriesItsDefaultProrationFlagAndItIsNotNullable() {
        Boolean prorratea = jdbc.queryForObject("""
                select p.extra_payments_prorated
                  from rulesystem.agreement_profile p
                  join rulesystem.rule_entity e on e.id = p.agreement_rule_entity_id
                 where e.rule_system_code = 'ESP'
                   and e.rule_entity_type_code = 'AGREEMENT'
                   and e.code = ?
                """, Boolean.class, CONVENIO);

        assertEquals(Boolean.FALSE, prorratea,
                "El convenio de grandes almacenes permite el prorrateo por acuerdo: su valor por "
                        + "omision es que las extras se pagan en su mes.");

        String nullable = jdbc.queryForObject("""
                select is_nullable
                  from information_schema.columns
                 where table_schema = 'rulesystem'
                   and table_name = 'agreement_profile'
                   and column_name = 'extra_payments_prorated'
                """, String.class);

        assertEquals("NO", nullable,
                "El testigo no admite nulos: un convenio sin testigo obligaria a la contratacion a "
                        + "inventarse que hacer cuando falta.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Las cuatro pagas como conceptos del motor
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void theFourExtraPaymentsOfTheAgreementAreDeclaredAsEngineAggregates() {
        List<Map<String, Object>> pagas = jdbc.queryForList("""
                select o.object_code, c.concept_mnemonic, c.calculation_type, c.functional_nature,
                       c.execution_scope, c.payslip_order_code
                  from payroll_engine.payroll_object o
                  join payroll_engine.payroll_concept c on c.object_id = o.id
                 where o.rule_system_code = 'ESP'
                   and o.object_type_code = 'CONCEPT'
                   and o.object_code like 'PE\\_%'
                 order by o.object_code
                """);

        assertEquals(LAS_CUATRO_PAGAS, pagas.stream().map(f -> (String) f.get("object_code")).toList(),
                "El convenio de la demo tiene cuatro pagas extraordinarias (art. 23). Ni tres ni "
                        + "cinco: el numero no es un parametro del motor, es cuantas define el convenio.");

        for (Map<String, Object> paga : pagas) {
            String codigo = (String) paga.get("object_code");
            assertEquals("AGGREGATE", paga.get("calculation_type"),
                    codigo + " es la suma de los conceptos que la componen segun el convenio");
            assertEquals("BASE", paga.get("functional_nature"),
                    codigo + " es una base intermedia, como P01: no es un devengo ni una deduccion");
            assertEquals("SEGMENT", paga.get("execution_scope"),
                    codigo + " se compone de lineas que ya vienen prorrateadas por dias, asi que "
                            + "hereda tramos, presencia y ausencias sin disenar nada");
            assertTrue(paga.get("payslip_order_code") == null,
                    codigo + " no se imprime: la prorrata que se imprime es la del backend#119");
        }
    }

    /**
     * De que se compone cada paga: hoy, un {@code SALARIO_BASE} mensual, que es lo unico que el
     * modelo tiene. El dia que haya complemento de puesto se le anade otra alimentacion y no se
     * toca ni una linea de Java.
     */
    @Test
    void eachExtraPaymentIsFedByTheConceptsTheAgreementSaysItIsMadeOf() {
        for (String paga : LAS_CUATRO_PAGAS) {
            List<String> alimentadores = jdbc.queryForList("""
                    select origen.object_code
                      from payroll_engine.payroll_concept_feed_relation f
                      join payroll_engine.payroll_object origen  on origen.id  = f.source_object_id
                      join payroll_engine.payroll_object destino on destino.id = f.target_object_id
                     where destino.rule_system_code = 'ESP'
                       and destino.object_type_code = 'CONCEPT'
                       and destino.object_code = ?
                     order by origen.object_code
                    """, String.class, paga);

            assertEquals(List.of("101"), alimentadores,
                    paga + " se compone hoy de un salario base mensual, que es de lo unico que el "
                            + "modelo sabe componerla. Lo que cambie aqui es el convenio, no el motor.");
        }
    }

    /**
     * La afirmacion que hace comprobable el «cero recibos se mueven» de este issue: las cuatro
     * pagas no alimentan a nadie.
     *
     * <p>Un concepto que no alimenta a ningun otro no puede mover un importe de un recibo, y por
     * eso declararlas es seguro aunque la semilla no se recalcule. Quien rompa este test esta
     * enchufando la prorrata, y eso es el {@code backend#119} — que mueve los 863 recibos a
     * proposito.
     */
    @Test
    void noExtraPaymentFeedsAnythingYet_whichIsWhyNoPayslipCanMove() {
        List<String> consumidores = jdbc.queryForList("""
                select origen.object_code || ' -> ' || destino.object_code
                  from payroll_engine.payroll_concept_feed_relation f
                  join payroll_engine.payroll_object origen  on origen.id  = f.source_object_id
                  join payroll_engine.payroll_object destino on destino.id = f.target_object_id
                 where origen.rule_system_code = 'ESP'
                   and origen.object_type_code = 'CONCEPT'
                   and origen.object_code like 'PE\\_%'
                 order by 1
                """, String.class);

        assertEquals(List.of(), consumidores,
                "Las pagas del convenio todavia no alimentan nada. En cuanto alimenten algo, los "
                        + "recibos se mueven — y eso es el backend#119, no este issue.");

        List<String> operandos = jdbc.queryForList("""
                select destino.object_code
                  from payroll_engine.payroll_concept_operand op
                  join payroll_engine.payroll_object origen  on origen.id  = op.source_object_id
                  join payroll_engine.payroll_object destino on destino.id = op.target_object_id
                 where origen.rule_system_code = 'ESP'
                   and origen.object_code like 'PE\\_%'
                """, String.class);

        assertEquals(List.of(), operandos,
                "Ni como operando de nadie: eso tambien las meteria en el plan de ejecucion.");
    }

    /**
     * Y aun asi estan asignadas, que es lo que las separa de un concepto olvidado.
     *
     * <p>Las fuentes de un agregado no se expanden ({@code backend#110}): un concepto declarado y
     * sin asignacion propia no se ejecuta nunca, y el aviso {@code UNREACHABLE_CONCEPTS} saldria
     * en todas las corridas de la demo hasta el {@code backend#119}. Un aviso que sale siempre no
     * avisa de nada.
     *
     * <p>Que se ejecuten no mueve un recibo: sin orden de folio no son linea, y sin alimentar a
     * nadie no cambian un importe. Lo unico que crece es el rastro de calculo.
     */
    @Test
    void theFourExtraPaymentsAreAssignedToTheAgreement_soNoRunReportsThemAsUnreachable() {
        List<String> asignadas = jdbc.queryForList("""
                select concept_code
                  from payroll_engine.concept_assignment
                 where rule_system_code = 'ESP'
                   and agreement_code = ?
                   and concept_code like 'PE\\_%'
                 order by concept_code
                """, String.class, CONVENIO);

        assertEquals(LAS_CUATRO_PAGAS, asignadas,
                "Las pagas del convenio se asignan al convenio, que es la dimension por la que "
                        + "existen. Sin asignacion no se ejecutarian y cada corrida lo diria.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Los literales, y quien NO recibe nada
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void eachExtraPaymentIsNamedInSpanishAndNotAfterItsOwnMnemonic() {
        List<Map<String, Object>> nombres = jdbc.queryForList("""
                select o.object_code, c.concept_mnemonic, l.label
                  from payroll_engine.payroll_object o
                  join payroll_engine.payroll_concept c on c.object_id = o.id
                  left join payroll_engine.payroll_concept_label l
                         on l.object_id = o.id and l.language_code = 'es'
                 where o.rule_system_code = 'ESP'
                   and o.object_type_code = 'CONCEPT'
                   and o.object_code like 'PE\\_%'
                 order by o.object_code
                """);

        assertEquals(4, nombres.size());
        for (Map<String, Object> fila : nombres) {
            String codigo = (String) fila.get("object_code");
            String label = (String) fila.get("label");
            assertTrue(label != null && !label.isBlank(),
                    codigo + " se quedaria ensenando su mnemonico, que es el defecto del backend#109");
            assertFalse(label.equalsIgnoreCase((String) fila.get("concept_mnemonic")),
                    codigo + " tiene por nombre su propia clave: " + label);
        }
    }

    /**
     * {@code FRA} y {@code PRT} no reciben nada, y no es un olvido: no tienen convenio. Un concepto
     * de pagas extras en un sistema de reglas sin convenio seria una paga que nadie define.
     */
    @Test
    void theRuleSystemsWithoutAnAgreementGetNothing() {
        List<String> intrusos = jdbc.queryForList("""
                select o.rule_system_code || '/' || o.object_code
                  from payroll_engine.payroll_object o
                 where o.object_type_code = 'CONCEPT'
                   and o.object_code like 'PE\\_%'
                   and o.rule_system_code <> 'ESP'
                 order by 1
                """, String.class);

        assertEquals(List.of(), intrusos,
                "FRA y PRT no tienen convenio, asi que no tienen pagas extras que declarar.");
    }
}

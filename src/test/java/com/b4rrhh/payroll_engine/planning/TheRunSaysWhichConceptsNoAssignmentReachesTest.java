package com.b4rrhh.payroll_engine.planning;

import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationCommand;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationUseCase;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchEmployeeTarget;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelection;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelectionType;
import com.b4rrhh.payroll.domain.model.CalculationRun;
import com.b4rrhh.payroll.scenario.PayrollScenarioFixtures;
import com.b4rrhh.payroll_engine.metamodel.domain.port.RuleSystemMetamodelRepository;
import com.b4rrhh.payroll_engine.planning.application.service.UnreachableConceptFinder;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La ejecucion dice a que conceptos no llega ninguna asignacion ({@code backend#110}).
 *
 * <h2>El caso, tal y como salio</h2>
 *
 * <p>Midiendo el {@code b4rrhh/workspace#10} se sembro un catalogo hondo encadenado con
 * {@code AGGREGATE}, se lanzo el calculo y la corrida salio bien. Corrieron solo los conceptos
 * asignados y <b>el recibo salio identico al de antes: sin error, sin aviso, sin una linea de
 * diferencia</b>. Las fuentes de un agregado no se expanden —a proposito—, asi que un concepto
 * que solo existe como fuente de uno, y que no esta asignado por su cuenta, no se ejecuta nunca.
 *
 * <p>Lo que este test defiende no es que se ejecute: es que <b>se sepa</b>. Un recibo identico no
 * se distingue de «este cambio no tenia que mover nada», que es un caso legitimo y frecuente, y
 * costo medio dia a quien lo encontro sabiendo lo que buscaba.
 *
 * <h2>Los dos lados</h2>
 *
 * <p>Provocado con el catalogo que ya existe —el mismo grafo de los escenarios, mas un concepto
 * que solo alimenta al 970— y comprobado tambien al reves: el catalogo sano no dice nada, y el
 * catalogo real de {@code ESP} tampoco. Un aviso que sale siempre no avisa de nada.
 *
 * <p>Fuera de transaccion ({@code NOT_SUPPORTED}) por lo mismo que los demas escenarios de
 * lanzamiento: lo que se afirma es lo que quedo en la base, no lo que la sesion tenia encolado.
 * Cada test usa su propia reglamentacion, que se queda en el clon.
 */
@TestWebSobreEsquemaReal
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TheRunSaysWhichConceptsNoAssignmentReachesTest {

    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD = "202501";
    private static final String PAYROLL_TYPE = "NORMAL";
    private static final LocalDate PERIOD_START = LocalDate.of(2025, 1, 1);

    private static final String AVISO = "UNREACHABLE_CONCEPTS";

    @Autowired
    private LaunchPayrollCalculationUseCase launchPayrollCalculationUseCase;

    @Autowired
    private RuleSystemMetamodelRepository ruleSystemMetamodelRepository;

    @Autowired
    private UnreachableConceptFinder unreachableConceptFinder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PayrollScenarioFixtures fixtures;
    private String ruleSystem;

    @BeforeEach
    void setUpData() {
        fixtures = new PayrollScenarioFixtures(jdbcTemplate);
    }

    /**
     * Criterio 1 y 2: un concepto que solo alimenta a un agregado, sin asignacion propia, lo dice.
     *
     * <p>Y lo dice <b>sin tumbar nada</b>, que es la otra mitad: la corrida termina
     * {@code COMPLETED} y el recibo sale. Un concepto inalcanzable puede ser transitorio —se
     * declara hoy y se asigna manana— y parar la nomina de 873 personas por eso seria cambiar un
     * silencio por un portazo.
     */
    @Test
    void aConceptThatOnlyFeedsAnAggregateAndIsNotAssignedIsReported() {
        usar("URA");
        conceptoQueSoloAlimentaAlTotal("X01");

        String empleado = altaYCalculo();
        CalculationRun run = lanzar(empleado);

        assertEquals("COMPLETED", run.status(),
                "la corrida sigue: esto avisa, no tumba");
        assertEquals(1, run.totalCalculated(),
                "y el recibo sale, que es justo lo que hacia el caso indistinguible de uno sano");

        List<Map<String, Object>> avisos = avisosDe(run);
        assertEquals(1, avisos.size(),
                "la ejecucion tiene que traer un aviso y uno solo. Lo que trae: " + mensajesDe(run));
        assertEquals("WARNING", avisos.get(0).get("severity_code"));

        String texto = (String) avisos.get(0).get("message");
        assertTrue(texto.contains("X01"),
                "el aviso dice de que concepto habla, o no sirve de nada: " + texto);

        String detalle = String.valueOf(avisos.get(0).get("details_json"));
        assertTrue(detalle.contains("X01") && detalle.contains("URA"),
                "y el detalle lleva la lista entera y el sistema de reglas: " + detalle);
    }

    /**
     * Criterio 3, la mitad que hace que esto valga algo: el mismo catalogo sin el concepto suelto
     * <b>no dice nada</b>.
     *
     * <p>Es el mismo grafo, el mismo empleado y el mismo lanzamiento que el test de arriba: lo
     * unico que cambia es el concepto que no alcanza nadie. Sin esta comprobacion, un aviso que
     * saliera en todas las corridas pasaria el criterio 1 sin haber mirado nada.
     */
    @Test
    void aHealthyCatalogSaysNothing() {
        usar("URB");

        String empleado = altaYCalculo();
        CalculationRun run = lanzar(empleado);

        assertEquals("COMPLETED", run.status());
        assertEquals(List.of(), avisosDe(run),
                "un catalogo en el que toda asignacion alcanza todo lo declarado no tiene nada que"
                        + " decir. Lo que dijo: " + mensajesDe(run));
    }

    /**
     * Y el catalogo de verdad tampoco. Es el mismo criterio 3 sobre el unico catalogo real que
     * hay, el que trae Flyway, y el que iba a salir en cada corrida de la demo si esto se hubiera
     * escrito de más.
     *
     * <p>Se pregunta al buscador directamente y no lanzando: aqui no hace falta una nomina, hace
     * falta la reglamentacion de {@code ESP} entera.
     */
    @Test
    void theRealEspCatalogHasNoUnreachableConcept() {
        assertEquals(List.of(), unreachableConceptFinder.unreachableConceptsIn(
                        ruleSystemMetamodelRepository.load("ESP", LocalDate.of(2026, 9, 30))),
                "el catalogo ESP no tiene conceptos a los que no llegue ninguna asignacion. Si esta"
                        + " lista deja de estar vacia, no es que el test este mal: es que hay"
                        + " conceptos declarados que no se ejecutan");
    }

    // ---------------------------------------------------------------------

    private void usar(String ruleSystemCode) {
        ruleSystem = ruleSystemCode;
        Integer yaSembrado = jdbcTemplate.queryForObject(
                "select count(*) from rulesystem.rule_system where code = ?", Integer.class, ruleSystem);
        if (yaSembrado == 0) {
            fixtures.seedConceptGraph(ruleSystem);
        }
    }

    /**
     * Un concepto declarado, con su operando, que <b>solo</b> alimenta al total de devengos y al
     * que no apunta ninguna asignacion. Es la forma exacta del catalogo del {@code workspace#10}.
     *
     * <p>Alimenta al 970, que es {@code AGGREGATE} y si esta asignado. La expansion llega al 970 y
     * se para ahi, asi que a este no llega nadie.
     */
    private void conceptoQueSoloAlimentaAlTotal(String conceptCode) {
        jdbcTemplate.update(
                "insert into payroll_engine.payroll_object"
                        + " (rule_system_code, object_type_code, object_code, created_at, updated_at)"
                        + " values (?, 'CONCEPT', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                ruleSystem, conceptCode);
        Long id = objectId(conceptCode);

        jdbcTemplate.update(
                "insert into payroll_engine.payroll_concept"
                        + " (object_id, concept_mnemonic, calculation_type, functional_nature,"
                        + "  payslip_order_code, execution_scope, created_at, updated_at)"
                        + " values (?, ?, 'RATE_BY_QUANTITY', 'EARNING', ?, 'SEGMENT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                id, "PLUS_" + conceptCode, conceptCode);

        // Con operandos, para que no se pueda decir que esta a medio declarar: los dos existen y
        // los dos son alcanzables por otro lado.
        jdbcTemplate.update(
                "insert into payroll_engine.payroll_concept_operand"
                        + " (target_object_id, operand_role, source_object_id, created_at, updated_at)"
                        + " values (?, 'QUANTITY', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                id, objectId("D01"));
        jdbcTemplate.update(
                "insert into payroll_engine.payroll_concept_operand"
                        + " (target_object_id, operand_role, source_object_id, created_at, updated_at)"
                        + " values (?, 'RATE', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                id, objectId("P02"));

        jdbcTemplate.update(
                "insert into payroll_engine.payroll_concept_feed_relation"
                        + " (source_object_id, target_object_id, feed_mode, feed_value, invert_sign,"
                        + "  effective_from, effective_to, created_at, updated_at)"
                        + " values (?, ?, 'FEED_BY_SOURCE', null, false, DATE '2025-01-01', null,"
                        + "         CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                id, objectId("970"));
    }

    private Long objectId(String conceptCode) {
        return jdbcTemplate.queryForObject(
                "select id from payroll_engine.payroll_object"
                        + " where rule_system_code = ? and object_type_code = 'CONCEPT' and object_code = ?",
                Long.class, ruleSystem, conceptCode);
    }

    private String altaYCalculo() {
        String employeeNumber = "UR" + (System.nanoTime() % 1_000_000_000L);
        long employeeId = fixtures.insertEmployee(ruleSystem, EMPLOYEE_TYPE, employeeNumber);
        fixtures.insertPresence(employeeId, 1, PERIOD_START, null);
        fixtures.insertLaborClassification(employeeId, PERIOD_START);
        fixtures.insertWorkingTime(employeeId, new BigDecimal("100.00"), PERIOD_START, null);
        return employeeNumber;
    }

    private CalculationRun lanzar(String employeeNumber) {
        return launchPayrollCalculationUseCase.launch(new LaunchPayrollCalculationCommand(
                ruleSystem, PERIOD, PAYROLL_TYPE, "ENGINE", "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, employeeNumber),
                        null),
                null));
    }

    private List<Map<String, Object>> avisosDe(CalculationRun run) {
        return jdbcTemplate.queryForList(
                "select message_code, severity_code, message, details_json::text as details_json"
                        + " from payroll.calculation_run_message"
                        + " where run_id = ? and message_code = ?",
                run.id(), AVISO);
    }

    /** Todos los mensajes de la ejecucion, para que un fallo diga que dijo de verdad. */
    private List<String> mensajesDe(CalculationRun run) {
        return jdbcTemplate.queryForList(
                "select message_code || ': ' || message from payroll.calculation_run_message"
                        + " where run_id = ? order by id",
                String.class, run.id());
    }
}

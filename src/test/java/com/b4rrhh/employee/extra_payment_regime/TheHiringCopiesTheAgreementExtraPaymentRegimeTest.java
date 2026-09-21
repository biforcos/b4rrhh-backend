package com.b4rrhh.employee.extra_payment_regime;

import com.b4rrhh.B4rrhhBackendApplication;
import com.b4rrhh.employee.extra_payment_regime.application.usecase.ListEmployeeExtraPaymentRegimesCommand;
import com.b4rrhh.employee.extra_payment_regime.application.usecase.ListEmployeeExtraPaymentRegimesUseCase;
import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegime;
import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.HireEmployeeResult;
import com.b4rrhh.employee.lifecycle.application.usecase.HireEmployeeUseCase;
import com.b4rrhh.support.EsquemaRealInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La contratacion rellena la vertical de regimen de pagas extras copiando el testigo del convenio
 * ({@code backend#118}, segundo tercio del paso 4 de {@code b4rrhh/workspace#9}).
 *
 * <h2>Nadie elige el regimen al contratar</h2>
 *
 * <p>El alta no lleva el dato: lo dice el convenio, y el empleado entra con el. Quien quiera otra
 * cosa lo pide, y eso es una fila mas en su vertical, no un caso especial del alta.
 *
 * <h2>Es una copia, no un enlace</h2>
 *
 * <p>Es la decision que este test defiende y la que no se ve mirando el codigo: si el convenio
 * cambia de opinion el ano que viene, los que ya estan <b>no cambian solos</b>. Un enlace seria mas
 * corto de escribir y reescribiria la historia: el recibo de marzo pasaria a decir algo que no se
 * pago. Cambiar a los que ya estan se hace cambiando sus filas, en masa si hace falta, y eso deja
 * rastro con su vigencia.
 *
 * <p>El {@code @SpringBootTest} y el inicializador son EXACTAMENTE los de
 * {@code HireEmployeeBaselineFlywayIntegrationTest} para compartir contexto con el: cualquier
 * anotacion de mas seria otro clon de la base y otros segundos de arranque.
 */
@SpringBootTest(
        classes = B4rrhhBackendApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.flyway.enabled=false"
        }
)
@ContextConfiguration(initializers = EsquemaRealInitializer.class)
class TheHiringCopiesTheAgreementExtraPaymentRegimeTest {

    /** El de la demo. Su testigo es «no se prorratea»: el prorrateo se pide (V144). */
    private static final String CONVENIO_DE_LA_DEMO = "99002405011982";
    private static final String CATEGORIA_DE_LA_DEMO = "99002405-G2";

    /** Uno inventado para este test, con el testigo al reves. */
    private static final String CONVENIO_QUE_PRORRATEA = "TST-PRORRATEA-118";
    private static final String CATEGORIA_QUE_PRORRATEA = "TST-PRORRATEA-118-G1";

    private static final LocalDate ALTA = LocalDate.of(2025, 3, 1);

    @Autowired
    private HireEmployeeUseCase hireEmployeeUseCase;

    @Autowired
    private ListEmployeeExtraPaymentRegimesUseCase listExtraPaymentRegimes;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void hiringWithoutSayingAnythingCopiesTheAgreementFlag_whichHereMeansNotProrated() {
        String empleado = contratarCon(CONVENIO_DE_LA_DEMO, CATEGORIA_DE_LA_DEMO);

        List<ExtraPaymentRegime> serie = regimenesDe(empleado);

        assertThat(serie)
                .as("contratar crea la primera fila de la vertical, sin que nadie la pida")
                .hasSize(1);
        assertThat(serie.get(0).isProrated())
                .as("el convenio de grandes almacenes permite el prorrateo por acuerdo, asi que "
                        + "su valor por omision es que las extras se pagan en su mes")
                .isFalse();
        assertThat(serie.get(0).getStartDate())
                .as("desde el alta, que es cuando empieza la presencia de la que deriva")
                .isEqualTo(ALTA);
        assertThat(serie.get(0).getEndDate())
                .as("y abierta: el regimen no caduca, se cambia")
                .isNull();
    }

    /**
     * El mismo alta contra un convenio con el testigo al reves. Sin este caso, el de arriba pasaria
     * igual si la contratacion pusiera {@code false} a fuerza y no mirara el convenio.
     */
    @Test
    void hiringAgainstAnAgreementThatProratesCopiesTheOtherValue() {
        sembrarConvenioQueProrratea();

        String empleado = contratarCon(CONVENIO_QUE_PRORRATEA, CATEGORIA_QUE_PRORRATEA);

        assertThat(regimenesDe(empleado))
                .singleElement()
                .extracting(ExtraPaymentRegime::isProrated)
                .as("lo que se copia es el testigo del convenio, no una constante")
                .isEqualTo(true);
    }

    /**
     * La decision de la copia, comprobada por el unico sitio donde se ve: el convenio cambia
     * <b>despues</b> de contratar y el empleado no se entera.
     */
    @Test
    void anAgreementThatChangesItsMindLaterDoesNotMoveTheEmployeesAlreadyHired() {
        sembrarConvenioQueProrratea();
        String empleado = contratarCon(CONVENIO_QUE_PRORRATEA, CATEGORIA_QUE_PRORRATEA);
        assertThat(regimenesDe(empleado)).singleElement()
                .extracting(ExtraPaymentRegime::isProrated).isEqualTo(true);

        jdbc.update("""
                update rulesystem.agreement_profile p
                   set extra_payments_prorated = false, updated_at = now()
                  from rulesystem.rule_entity e
                 where e.id = p.agreement_rule_entity_id
                   and e.rule_system_code = 'ESP'
                   and e.rule_entity_type_code = 'AGREEMENT'
                   and e.code = ?
                """, CONVENIO_QUE_PRORRATEA);

        assertThat(regimenesDe(empleado))
                .singleElement()
                .extracting(ExtraPaymentRegime::isProrated)
                .as("la fila del empleado es una COPIA del testigo, no un enlace a el: si cambiara "
                        + "sola, el recibo del mes pasado dejaria de poder explicarse")
                .isEqualTo(true);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private List<ExtraPaymentRegime> regimenesDe(String employeeNumber) {
        return listExtraPaymentRegimes.listByEmployeeBusinessKey(
                new ListEmployeeExtraPaymentRegimesCommand("ESP", "INTERNAL", employeeNumber));
    }

    private String contratarCon(String agreementCode, String agreementCategoryCode) {
        HireEmployeeResult result = hireEmployeeUseCase.hire(new HireEmployeeCommand(
                "ESP",
                "INTERNAL",
                "Prorrata",
                "Pagas",
                null,
                null,
                ALTA,
                "HIRING",
                "ES01",
                "MAIN_OFFICE",
                new HireEmployeeCommand.HireEmployeeContractCommand("100", "01"),
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand(
                        agreementCode, agreementCategoryCode),
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("100.00"))
        ));

        return result.employee().employeeNumber();
    }

    /**
     * Un convenio con el testigo al reves, con su categoria y su relacion, que es lo minimo que el
     * alta valida. Las horas anuales hacen falta porque la jornada las deriva de ahi.
     */
    private void sembrarConvenioQueProrratea() {
        Integer yaEsta = jdbc.queryForObject("""
                select count(*) from rulesystem.rule_entity
                 where rule_system_code = 'ESP' and rule_entity_type_code = 'AGREEMENT' and code = ?
                """, Integer.class, CONVENIO_QUE_PRORRATEA);
        if (yaEsta != null && yaEsta > 0) {
            return;
        }

        jdbc.update("""
                insert into rulesystem.rule_entity
                    (rule_system_code, rule_entity_type_code, code, name, description, active,
                     start_date, end_date)
                values ('ESP', 'AGREEMENT', ?, 'Convenio que prorratea', null, true,
                        DATE '2000-01-01', null)
                """, CONVENIO_QUE_PRORRATEA);

        jdbc.update("""
                insert into rulesystem.rule_entity
                    (rule_system_code, rule_entity_type_code, code, name, description, active,
                     start_date, end_date)
                values ('ESP', 'AGREEMENT_CATEGORY', ?, 'Grupo unico', null, true,
                        DATE '2000-01-01', null)
                """, CATEGORIA_QUE_PRORRATEA);

        jdbc.update("""
                insert into rulesystem.agreement_category_relation
                    (rule_system_id, agreement_rule_entity_id, category_rule_entity_id,
                     start_date, end_date, is_active)
                select rs.id, a.id, c.id, DATE '2000-01-01', null, true
                  from rulesystem.rule_system rs,
                       rulesystem.rule_entity a,
                       rulesystem.rule_entity c
                 where rs.code = 'ESP'
                   and a.rule_system_code = 'ESP' and a.rule_entity_type_code = 'AGREEMENT' and a.code = ?
                   and c.rule_system_code = 'ESP' and c.rule_entity_type_code = 'AGREEMENT_CATEGORY' and c.code = ?
                """, CONVENIO_QUE_PRORRATEA, CATEGORIA_QUE_PRORRATEA);

        jdbc.update("""
                insert into rulesystem.agreement_profile
                    (agreement_rule_entity_id, official_agreement_number, display_name, short_name,
                     annual_hours, extra_payments_prorated, is_active, created_at, updated_at)
                select a.id, ?, 'Convenio que prorratea', 'PRORRATA', 1736.00, true, true, now(), now()
                  from rulesystem.rule_entity a
                 where a.rule_system_code = 'ESP' and a.rule_entity_type_code = 'AGREEMENT' and a.code = ?
                """, CONVENIO_QUE_PRORRATEA, CONVENIO_QUE_PRORRATEA);
    }
}

package com.b4rrhh.payroll;

import com.b4rrhh.payroll.application.service.PayrollRuleFreshness;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.port.PayrollRepository;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Una regla editada desde otro huso sigue marcando el recibo ({@code backend#116}).
 *
 * <h2>El fallo que esto reproduce</h2>
 *
 * <p>{@code payroll.payroll.calculated_at} y los siete {@code updated_at} con los que se compara
 * eran {@code timestamp without time zone}: cada quien escribía <b>su hora de pared</b>. La
 * semilla la fabrica el loader en una máquina en {@code Europe/Madrid}; la CT de la demo corre en
 * {@code UTC}. Dos relojes, y el número que se guardaba no decía cuál.
 *
 * <p>Consecuencia, medida en el {@code backend#108}: durante las dos horas siguientes a cada
 * resiembra, una regla editada en la demo escribía un {@code updated_at} <b>anterior</b> al
 * {@code calculated_at} de todos los recibos, y el recibo decía «vigente» con una regla cambiada
 * debajo. Un fallo presentado como un hecho.
 *
 * <h2>Cómo se reproduce sin dos máquinas</h2>
 *
 * <p>Con dos husos en la misma sesión, que es lo mismo visto desde la base. {@code localtimestamp}
 * devuelve el {@code now()} de la transacción <b>renderizado en el huso de la sesión</b>, así que
 * dentro de una sola transacción:
 *
 * <ul>
 *   <li>en {@code Europe/Madrid} el recibo se calcula «a las 12:00»,
 *   <li>en {@code UTC} la regla se edita «a las 10:00:01» — un segundo <b>después</b> en tiempo
 *       real, dos horas antes en el número.
 * </ul>
 *
 * <p>Que las dos lecturas salgan del mismo {@code now()} no es una simplificación: lo hace
 * determinista. No hay carrera posible, y la diferencia que queda es exactamente la del huso.
 *
 * <h2>Por qué la bandera no valía como arreglo</h2>
 *
 * <p>{@code -Duser.timezone=UTC} o {@code hibernate.jdbc.time_zone} habrían tapado esto y son una
 * convención: se rompen el día que alguien ejecuta el loader sin la bandera, y no se nota. El
 * tipo de la columna no se puede olvidar.
 */
@TestSobreEsquemaReal
class ARuleEditedFromAnotherTimeZoneStillMarksTheReceiptTest {

    private static final String RULE_SYSTEM   = "ESP";
    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD        = "202609";
    private static final String PAYROLL_TYPE  = "NORMAL";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PayrollRepository payrollRepository;

    @Autowired
    private PayrollRuleFreshness freshness;

    @Test
    void aRuleEditedInUtcOneSecondAfterAReceiptCalculatedInMadrid() {
        String employeeNumber = "TZ" + (System.nanoTime() % 1_000_000_000L);

        // 1. El recibo lo escribe una sesion en Europe/Madrid: el loader, el portatil.
        //    'set local' y no 'set': la conexion es de un pool y el huso tiene que morir con
        //    la transaccion, o el siguiente test hereda un reloj que no pidio.
        jdbc.execute("set local time zone 'Europe/Madrid'");
        jdbc.update("""
                insert into payroll.payroll
                    (rule_system_code, employee_type_code, employee_number, payroll_period_code,
                     payroll_type_code, presence_number, status, calculated_at,
                     calculation_engine_code, calculation_engine_version, created_at, updated_at)
                values (?, ?, ?, ?, ?, 1, 'CALCULATED', localtimestamp,
                        'GRAPH', '1.0', localtimestamp, localtimestamp)
                """,
                RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber, PERIOD, PAYROLL_TYPE);

        // 2. La regla la edita una sesion en UTC: la CT de la demo. Un segundo despues en tiempo
        //    real -- y dos horas antes si lo unico que se guarda es la hora de pared.
        jdbc.execute("set local time zone 'UTC'");
        int tocadas = jdbc.update("""
                update payroll_engine.payroll_object
                   set updated_at = localtimestamp + interval '1 second'
                 where rule_system_code = ?
                """, RULE_SYSTEM);
        assertTrue(tocadas > 0, "el escenario necesita objetos de ESP que editar");

        jdbc.execute("set local time zone 'Europe/Madrid'");

        Payroll recibo = payrollRepository.findByBusinessKey(
                        RULE_SYSTEM, EMPLOYEE_TYPE, employeeNumber, PERIOD, PAYROLL_TYPE, 1)
                .orElseThrow(() -> new AssertionError("el recibo del escenario no se ha escrito"));

        assertTrue(freshness.rulesChangedSinceCalculation(recibo),
                """
                La regla se edito DESPUES de calcularse el recibo y el recibo dice que sigue \
                vigente.

                Es el fallo del backend#116: el que escribe pone su hora de pared, y una regla \
                editada en UTC parece dos horas mas vieja que un recibo calculado en Madrid. \
                Durante esas dos horas el aviso no sale y nadie se entera.""");
    }
}

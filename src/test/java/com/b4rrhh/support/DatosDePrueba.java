package com.b4rrhh.support;

import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Las filas minimas que hacen falta para que el esquema real deje trabajar.
 *
 * Con el DDL a mano los tests inventaban un employee_id y listo, porque no
 * habia clave ajena que lo desmintiera. Con el esquema de produccion el
 * empleado tiene que existir, y ademas la base pone el id ella misma
 * ('generated always'), asi que hay que leerlo de vuelta.
 *
 * El numero de empleado se genera aqui, distinto en cada llamada: la mayoria de
 * los tests van en transaccion y no se pisan, pero los de atomicidad escriben
 * de verdad y comparten base con el resto de su contexto.
 */
public final class DatosDePrueba {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    private DatosDePrueba() {
    }

    private static final AtomicInteger SECUENCIA_DNI = new AtomicInteger(90_000_000);

    /**
     * Un DNI valido y distinto en cada llamada (b4rrhh/backend#141): el alta lo exige y se
     * niega si ya es de otro empleado, asi que dos altas de un mismo test no pueden compartirlo.
     * Empieza en 90.000.000 para no pisar los de la semilla del loader, que cuentan desde 1.
     */
    public static String dni() {
        int numero = SECUENCIA_DNI.incrementAndGet();
        return String.format("%08d", numero) + "TRWAGMYFPDXBNJZSQVHLCKE".charAt(numero % 23);
    }

    /** El cuerpo JSON de un alta con un DNI nuevo dentro, para los tests que no van de eso. */
    public static String conDni(String cuerpoDelAlta) {
        int llave = cuerpoDelAlta.indexOf('{');
        return cuerpoDelAlta.substring(0, llave + 1)
                + " \"identifier\": { \"identifierTypeCode\": \"NATIONAL_ID\", \"identifierValue\": \""
                + dni() + "\", \"issuingCountryCode\": \"ESP\" },"
                + cuerpoDelAlta.substring(llave + 1);
    }

    /** Un empleado cualquiera, con numero unico. */
    public static Long empleado(JdbcTemplate jdbcTemplate) {
        return empleado(jdbcTemplate, "T" + String.format("%08d", SECUENCIA.incrementAndGet()));
    }

    public static Long empleado(JdbcTemplate jdbcTemplate, String employeeNumber) {
        return jdbcTemplate.queryForObject("""
                insert into employee.employee (
                    rule_system_code, employee_type_code, employee_number, first_name, last_name_1
                ) values ('ESP', 'INTERNAL', ?, 'Test', 'Test')
                returning id
                """, Long.class, employeeNumber);
    }

    /**
     * Una entidad de catalogo. Sirve para colgar de ella los perfiles
     * (empresa, centro) que en el esquema real tienen clave ajena a
     * rulesystem.rule_entity, cosa que el DDL a mano no declaraba.
     *
     * El codigo tiene que ser uno que no siembren las migraciones: la unica
     * uk_rule_entity_business es (rule_system_code, rule_entity_type_code, code).
     */
    public static Long ruleEntity(
            JdbcTemplate jdbcTemplate,
            String ruleEntityTypeCode,
            String code,
            String name,
            LocalDate startDate,
            LocalDate endDate) {
        return jdbcTemplate.queryForObject("""
                insert into rulesystem.rule_entity (
                    rule_system_code, rule_entity_type_code, code, name, active, start_date, end_date
                ) values ('ESP', ?, ?, ?, true, ?, ?)
                returning id
                """, Long.class, ruleEntityTypeCode, code, name, startDate, endDate);
    }

    /**
     * Una presencia del empleado. company_code es varchar(4) en produccion
     * (ES01 es una de las empresas que siembran las migraciones), y el check de
     * fechas es estricto: end_date, si viene, tiene que ser posterior.
     */
    public static Long presencia(
            JdbcTemplate jdbcTemplate,
            Long employeeId,
            int presenceNumber,
            LocalDate startDate,
            LocalDate endDate) {
        return jdbcTemplate.queryForObject("""
                insert into employee.presence (
                    employee_id, presence_number, company_code,
                    entry_reason_code, exit_reason_code, start_date, end_date
                ) values (?, ?, 'ES01', 'HIRING', ?, ?, ?)
                returning id
                """,
                Long.class,
                employeeId,
                presenceNumber,
                endDate == null ? null : "TERMINATION",
                startDate,
                endDate);
    }
}

package com.b4rrhh.payroll.scenario;

import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PayrollScenarioFixtures {

    static final String AGREEMENT_CODE     = "99002405011982";
    static final String CATEGORY_CODE      = "99002405-G2";
    static final String TABLE_CODE         = "P02_99002405011982";
    static final BigDecimal DAILY_RATE     = new BigDecimal("47.50");

    private final JdbcTemplate jdbc;

    public PayrollScenarioFixtures(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Seeds the full concept graph: rule_system, 15 concepts + 1 TABLE object,
     * operands, feed relations, concept assignments, binding,
     * table row, and the agreement_category_profile.
     */
    public void seedConceptGraph(String ruleSystemCode) {
        jdbc.update(
                "insert into rulesystem.rule_system (code, name, country_code, active, created_at, updated_at)" +
                " values (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                ruleSystemCode, ruleSystemCode, ruleSystemCode, true);

        for (String code : new String[]{"101","D01","J01","P01","P02","B01",
                "P_SS_CC","P_SS_DESEMPLEO","P_IRPF","700","703","800","970","980","990"}) {
            jdbc.update(
                    "insert into payroll_engine.payroll_object (rule_system_code, object_type_code, object_code, created_at, updated_at)" +
                    " values (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    ruleSystemCode, "CONCEPT", code);
        }
        jdbc.update(
                "insert into payroll_engine.payroll_object (rule_system_code, object_type_code, object_code, created_at, updated_at)" +
                " values (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                ruleSystemCode, "TABLE", "P02_DAILY_AMOUNT_TABLE");

        Long id101         = objectId(ruleSystemCode, "CONCEPT", "101");
        Long idD01         = objectId(ruleSystemCode, "CONCEPT", "D01");
        Long idJ01         = objectId(ruleSystemCode, "CONCEPT", "J01");
        Long idP01         = objectId(ruleSystemCode, "CONCEPT", "P01");
        Long idP02         = objectId(ruleSystemCode, "CONCEPT", "P02");
        Long idB01         = objectId(ruleSystemCode, "CONCEPT", "B01");
        Long idPSSCC       = objectId(ruleSystemCode, "CONCEPT", "P_SS_CC");
        Long idPSSDESEMP   = objectId(ruleSystemCode, "CONCEPT", "P_SS_DESEMPLEO");
        Long idPIRPF       = objectId(ruleSystemCode, "CONCEPT", "P_IRPF");
        Long id700         = objectId(ruleSystemCode, "CONCEPT", "700");
        Long id703         = objectId(ruleSystemCode, "CONCEPT", "703");
        Long id800         = objectId(ruleSystemCode, "CONCEPT", "800");
        Long id970         = objectId(ruleSystemCode, "CONCEPT", "970");
        Long id980         = objectId(ruleSystemCode, "CONCEPT", "980");
        Long id990         = objectId(ruleSystemCode, "CONCEPT", "990");
        Long idP02Table    = objectId(ruleSystemCode, "TABLE",   "P02_DAILY_AMOUNT_TABLE");

        String cSql = "insert into payroll_engine.payroll_concept" +
                " (object_id, concept_mnemonic, calculation_type, functional_nature," +
                "  payslip_order_code, execution_scope, created_at, updated_at)" +
                " values (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        // Los mismos ambitos que la semilla de ESP tras backend#64: lo que depende del tramo
        // (dias devengados, jornada, precio por jornada y el salario que los multiplica) es
        // SEGMENT; todo lo demas se evalua una vez sobre el periodo.
        jdbc.update(cSql, id101,       "SALARIO_BASE",            "RATE_BY_QUANTITY", "EARNING",         "101",  "SEGMENT");
        jdbc.update(cSql, idD01,       "DIAS_DEVENGO",            "ENGINE_PROVIDED",  "TECHNICAL",       null,  "SEGMENT");
        jdbc.update(cSql, idJ01,       "COEFICIENTE_JORNADA",     "ENGINE_PROVIDED",  "TECHNICAL",       null,  "SEGMENT");
        jdbc.update(cSql, idP01,       "PRECIO_DIA",              "RATE_BY_QUANTITY", "BASE",            null,  "SEGMENT");
        jdbc.update(cSql, idP02,       "PRECIO_DIA_PLENO",        "DIRECT_AMOUNT",    "BASE",            null,  "PERIOD");
        jdbc.update(cSql, idB01,       "BASE_COTIZABLE",          "AGGREGATE",        "BASE",            null,  "PERIOD");
        jdbc.update(cSql, idPSSCC,     "TIPO_CC_TRABAJADOR",      "ENGINE_PROVIDED",  "TECHNICAL",       null,  "PERIOD");
        jdbc.update(cSql, idPSSDESEMP, "TIPO_DESEMPLEO_TRABAJADOR","ENGINE_PROVIDED", "TECHNICAL",       null,  "PERIOD");
        jdbc.update(cSql, idPIRPF,     "TIPO_IRPF",               "ENGINE_PROVIDED",  "TECHNICAL",       null,  "PERIOD");
        jdbc.update(cSql, id700,       "CC_TRABAJADOR",           "PERCENTAGE",       "DEDUCTION",       "700",  "PERIOD");
        jdbc.update(cSql, id703,       "DESEMPLEO_TRABAJADOR",    "PERCENTAGE",       "DEDUCTION",       "703",  "PERIOD");
        jdbc.update(cSql, id800,       "RETENCION_IRPF",          "PERCENTAGE",       "DEDUCTION",       "800",  "PERIOD");
        jdbc.update(cSql, id970,       "TOTAL_DEVENGOS",          "AGGREGATE",        "TOTAL_EARNING",   "970",  "PERIOD");
        jdbc.update(cSql, id980,       "TOTAL_DEDUCCIONES",       "AGGREGATE",        "TOTAL_DEDUCTION", "980",  "PERIOD");
        jdbc.update(cSql, id990,       "LIQUIDO_A_PAGAR",         "AGGREGATE",        "NET_PAY",         "990",  "PERIOD");

        String oSql = "insert into payroll_engine.payroll_concept_operand" +
                " (target_object_id, operand_role, source_object_id, created_at, updated_at)" +
                " values (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        jdbc.update(oSql, id101, "QUANTITY",   idD01);
        jdbc.update(oSql, id101, "RATE",       idP01);
        jdbc.update(oSql, idP01, "QUANTITY",   idJ01);
        jdbc.update(oSql, idP01, "RATE",       idP02);
        jdbc.update(oSql, id700, "BASE",       idB01);
        jdbc.update(oSql, id700, "PERCENTAGE", idPSSCC);
        jdbc.update(oSql, id703, "BASE",       idB01);
        jdbc.update(oSql, id703, "PERCENTAGE", idPSSDESEMP);
        jdbc.update(oSql, id800, "BASE",       idB01);
        jdbc.update(oSql, id800, "PERCENTAGE", idPIRPF);

        String fSql = "insert into payroll_engine.payroll_concept_feed_relation" +
                " (source_object_id, target_object_id, feed_mode, feed_value, invert_sign," +
                "  effective_from, effective_to, created_at, updated_at)" +
                " values (?, ?, ?, ?, false, DATE '2025-01-01', null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        String fInv = "insert into payroll_engine.payroll_concept_feed_relation" +
                " (source_object_id, target_object_id, feed_mode, feed_value, invert_sign," +
                "  effective_from, effective_to, created_at, updated_at)" +
                " values (?, ?, ?, ?, true, DATE '2025-01-01', null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        jdbc.update(fSql,  idP02Table, idP02, "FEED_BY_SOURCE", null);
        jdbc.update(fSql,  id101,      idB01, "FEED_BY_SOURCE", null);
        jdbc.update(fSql,  id101,      id970, "FEED_BY_SOURCE", null);
        jdbc.update(fSql,  id700,      id980, "FEED_BY_SOURCE", null);
        jdbc.update(fSql,  id703,      id980, "FEED_BY_SOURCE", null);
        jdbc.update(fSql,  id800,      id980, "FEED_BY_SOURCE", null);
        jdbc.update(fSql,  id970,      id990, "FEED_BY_SOURCE", null);
        jdbc.update(fInv,  id980,      id990, "FEED_BY_SOURCE", null);

        String aSql = "insert into payroll_engine.concept_assignment" +
                " (rule_system_code, concept_code, company_code, agreement_code, employee_type_code," +
                "  valid_from, valid_to, priority, created_at, updated_at)" +
                " values (?, ?, ?, ?, ?, DATE '2025-01-01', null, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        jdbc.update(aSql, ruleSystemCode, "101", null, AGREEMENT_CODE, null, 10);
        jdbc.update(aSql, ruleSystemCode, "700", null, AGREEMENT_CODE, null, 700);
        jdbc.update(aSql, ruleSystemCode, "703", null, AGREEMENT_CODE, null, 703);
        jdbc.update(aSql, ruleSystemCode, "800", null, AGREEMENT_CODE, null, 800);
        jdbc.update(aSql, ruleSystemCode, "970", null, AGREEMENT_CODE, null, 970);
        jdbc.update(aSql, ruleSystemCode, "980", null, AGREEMENT_CODE, null, 980);
        jdbc.update(aSql, ruleSystemCode, "990", null, AGREEMENT_CODE, null, 990);

        jdbc.update(
                "insert into payroll.payroll_object_binding" +
                " (rule_system_code, owner_type_code, owner_code, binding_role_code, bound_object_type_code, bound_object_code, active)" +
                " values (?, ?, ?, ?, ?, ?, ?)",
                ruleSystemCode, "AGREEMENT", AGREEMENT_CODE, "P02_DAILY_AMOUNT_TABLE", "TABLE", TABLE_CODE, true);
        jdbc.update(
                "insert into payroll.payroll_table_row (rule_system_code, table_code, search_code, start_date, end_date, daily_value, active)" +
                " values (?, ?, ?, DATE '2025-01-01', null, ?, ?)",
                ruleSystemCode, TABLE_CODE, CATEGORY_CODE, DAILY_RATE, true);

        jdbc.update(
                "insert into rulesystem.rule_entity" +
                " (rule_system_code, rule_entity_type_code, code, name, active, start_date, created_at, updated_at)" +
                " values (?, ?, ?, ?, ?, DATE '2025-01-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                ruleSystemCode, "AGREEMENT_CATEGORY", CATEGORY_CODE, "G2", true);
        Long categoryId = jdbc.queryForObject(
                "select id from rulesystem.rule_entity where rule_system_code = ? and rule_entity_type_code = ? and code = ?",
                Long.class, ruleSystemCode, "AGREEMENT_CATEGORY", CATEGORY_CODE);
        jdbc.update(
                "insert into rulesystem.agreement_category_profile" +
                " (agreement_category_rule_entity_id, grupo_cotizacion_code, tipo_nomina, created_at, updated_at)" +
                " values (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                categoryId, "05", "MENSUAL");
    }

    /** Inserts one employee row; returns the generated surrogate id. */
    public long insertEmployee(String ruleSystemCode, String employeeTypeCode, String employeeNumber) {
        jdbc.update(
                "insert into employee.employee" +
                " (rule_system_code, employee_type_code, employee_number, first_name, last_name_1, status, created_at, updated_at)" +
                " values (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                ruleSystemCode, employeeTypeCode, employeeNumber, "Test", "Employee", "ACTIVE");
        return jdbc.queryForObject(
                "select id from employee.employee where rule_system_code = ? and employee_type_code = ? and employee_number = ?",
                Long.class, ruleSystemCode, employeeTypeCode, employeeNumber);
    }

    /**
     * Inserts one presence row; endDate may be null for an open-ended presence.
     * Returns the generated surrogate id.
     *
     * HIRING es el motivo de entrada que siembran las migraciones; el "HIRE" que
     * habia aqui no existe en ningun catalogo, y solo colaba porque
     * employee.presence no tiene clave ajena al motivo (#2).
     */
    public long insertPresence(long employeeId, int presenceNumber, LocalDate startDate, LocalDate endDate) {
        jdbc.update(
                "insert into employee.presence" +
                " (employee_id, presence_number, company_code, entry_reason_code, start_date, end_date, created_at, updated_at)" +
                " values (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                employeeId, presenceNumber, "ES01", "HIRING", startDate, endDate);
        return jdbc.queryForObject(
                "select id from employee.presence where employee_id = ? and presence_number = ?",
                Long.class, employeeId, presenceNumber);
    }

    /** Inserts a labor classification row (open-ended). */
    public void insertLaborClassification(long employeeId, LocalDate from) {
        jdbc.update(
                "insert into employee.labor_classification" +
                " (employee_id, agreement_code, agreement_category_code, start_date, end_date, created_at, updated_at)" +
                " values (?, ?, ?, ?, null, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                employeeId, AGREEMENT_CODE, CATEGORY_CODE, from);
    }

    /**
     * Inserts a working-time row. The working_time_number is auto-assigned as
     * max(existing) + 1 for the employee. {@code to} may be null (open-ended).
     *
     * WARNING: an open-ended (to=null) working-time record extends to the payroll
     * period end and will be clipped by buildSegments() to any presence that overlaps
     * the period. If the employee has multiple presences, set an explicit end date to
     * prevent bleed-through into subsequent presences.
     */
    public void insertWorkingTime(long employeeId, BigDecimal percentage, LocalDate from, LocalDate to) {
        Integer max = jdbc.queryForObject(
                "select coalesce(max(working_time_number), 0) from employee.working_time where employee_id = ?",
                Integer.class, employeeId);
        int nextNum = (max == null ? 0 : max) + 1;
        jdbc.update(
                "insert into employee.working_time" +
                " (employee_id, working_time_number, start_date, end_date, working_time_percentage," +
                "  weekly_hours, daily_hours, monthly_hours, created_at, updated_at)" +
                " values (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                employeeId, nextNum, from, to, percentage,
                new BigDecimal("40.00"), new BigDecimal("8.00"), new BigDecimal("173.33"));
    }

    /**
     * Anade al grafo la cadena del tope y el suelo de cotizacion, igual que la siembra
     * ESP (V88): B_CC_MAX = LEAST(B01, P_TOPE_MAX), B_CC = GREATEST(B_CC_MAX, P_TOPE_MIN),
     * y los porcentajes 700 y 703 pasan a leer B_CC en vez de B01. Los cuatro conceptos
     * son PERIOD: el tope se aplica una vez sobre el mes, no por tramos (ADR-058).
     *
     * Requiere {@link #seedConceptGraph} antes. El tope y el suelo del grupo 05 MENSUAL
     * son los que se pasan, para que cada test elija si muerden o no.
     */
    public void seedContributionCaps(String ruleSystemCode, BigDecimal baseMin, BigDecimal baseMax) {
        for (String code : new String[]{"P_TOPE_MAX", "P_TOPE_MIN", "B_CC_MAX", "B_CC"}) {
            jdbc.update(
                    "insert into payroll_engine.payroll_object (rule_system_code, object_type_code, object_code, created_at, updated_at)" +
                    " values (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    ruleSystemCode, "CONCEPT", code);
        }
        Long idB01     = objectId(ruleSystemCode, "CONCEPT", "B01");
        Long id700     = objectId(ruleSystemCode, "CONCEPT", "700");
        Long id703     = objectId(ruleSystemCode, "CONCEPT", "703");
        Long idTopeMax = objectId(ruleSystemCode, "CONCEPT", "P_TOPE_MAX");
        Long idTopeMin = objectId(ruleSystemCode, "CONCEPT", "P_TOPE_MIN");
        Long idBccMax  = objectId(ruleSystemCode, "CONCEPT", "B_CC_MAX");
        Long idBcc     = objectId(ruleSystemCode, "CONCEPT", "B_CC");

        String cSql = "insert into payroll_engine.payroll_concept" +
                " (object_id, concept_mnemonic, calculation_type, functional_nature," +
                "  payslip_order_code, execution_scope, created_at, updated_at)" +
                " values (?, ?, ?, ?, null, 'PERIOD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        jdbc.update(cSql, idTopeMax, "TOPE_MAX_COTIZACION",   "ENGINE_PROVIDED", "TECHNICAL");
        jdbc.update(cSql, idTopeMin, "TOPE_MIN_COTIZACION",   "ENGINE_PROVIDED", "TECHNICAL");
        jdbc.update(cSql, idBccMax,  "BASE_COTIZACION_MAX",   "LEAST",           "BASE");
        jdbc.update(cSql, idBcc,     "BASE_COTIZACION_COTIZ", "GREATEST",        "BASE");

        String oSql = "insert into payroll_engine.payroll_concept_operand" +
                " (target_object_id, operand_role, source_object_id, created_at, updated_at)" +
                " values (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        jdbc.update(oSql, idBccMax, "LEFT",  idB01);
        jdbc.update(oSql, idBccMax, "RIGHT", idTopeMax);
        jdbc.update(oSql, idBcc,    "LEFT",  idBccMax);
        jdbc.update(oSql, idBcc,    "RIGHT", idTopeMin);
        jdbc.update("update payroll_engine.payroll_concept_operand set source_object_id = ?" +
                        " where target_object_id in (?, ?) and operand_role = 'BASE'",
                idBcc, id700, id703);

        jdbc.update(
                "insert into payroll_engine.ss_cotizacion_topes" +
                " (rule_system_code, grupo_code, period_type, base_min, base_max, valid_from, valid_to)" +
                " values (?, ?, ?, ?, ?, DATE '2025-01-01', null)",
                ruleSystemCode, "05", "MENSUAL", baseMin, baseMax);
    }

    /** Cambia el precio diario pleno de la categoria del fixture (la fila de P02). */
    public void setDailyRate(String ruleSystemCode, BigDecimal dailyRate) {
        jdbc.update(
                "update payroll.payroll_table_row set daily_value = ?" +
                " where rule_system_code = ? and table_code = ? and search_code = ?",
                dailyRate, ruleSystemCode, TABLE_CODE, CATEGORY_CODE);
    }

    /** Declara el ambito de ejecucion de los conceptos dados. */
    public void setExecutionScope(String ruleSystemCode, String executionScope, String... conceptCodes) {
        for (String code : conceptCodes) {
            jdbc.update(
                    "update payroll_engine.payroll_concept set execution_scope = ? where object_id = ?",
                    executionScope, objectId(ruleSystemCode, "CONCEPT", code));
        }
    }

    private Long objectId(String ruleSystemCode, String objectTypeCode, String objectCode) {
        return jdbc.queryForObject(
                "select id from payroll_engine.payroll_object" +
                " where rule_system_code = ? and object_type_code = ? and object_code = ?",
                Long.class, ruleSystemCode, objectTypeCode, objectCode);
    }
}

package com.b4rrhh.payroll.basesalary.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Output port for resolving payroll table rows.
 * Answers: "What is the salary value for this table and search key on this date?"
 */
public interface PayrollTableRowLookupPort {

    /**
     * La fila aplicable, entera.
     *
     * <p>Es la busqueda de verdad y las dos de abajo son proyecciones suyas. Devuelve la fila y no
     * el numero porque quien calcula tiene que poder guardar <b>de que fila</b> leyo
     * ({@code backend#107}): con un {@code BigDecimal} la procedencia se pierde en el acto y
     * recuperarla despues obliga a repetir la busqueda, que ya no contesta lo mismo si entre medias
     * se movio una vigencia.
     *
     * @param ruleSystemCode the rule system code (e.g., "ESP")
     * @param tableCode the table code (e.g., "SB_99002405011982")
     * @param searchCode the search key (e.g., category code "99002405-G2")
     * @param effectiveDate the date for which to find the applicable row
     * @return la fila aplicable, o vacio si no hay ninguna
     */
    Optional<PayrollTableRowRead> findApplicableRow(
            String ruleSystemCode,
            String tableCode,
            String searchCode,
            LocalDate effectiveDate
    );

    /**
     * Resolve the monthly base salary from a table row.
     *
     * @return the monthly value, or empty if not found
     */
    default Optional<BigDecimal> resolveMonthlyValue(
            String ruleSystemCode,
            String tableCode,
            String searchCode,
            LocalDate effectiveDate
    ) {
        return findApplicableRow(ruleSystemCode, tableCode, searchCode, effectiveDate)
                .map(PayrollTableRowRead::monthlyValue);
    }

    default Optional<BigDecimal> resolveDailyValue(
            String ruleSystemCode,
            String tableCode,
            String searchCode,
            LocalDate effectiveDate
    ) {
        return findApplicableRow(ruleSystemCode, tableCode, searchCode, effectiveDate)
                .map(PayrollTableRowRead::dailyValue);
    }
}

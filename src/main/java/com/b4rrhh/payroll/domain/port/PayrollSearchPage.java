package com.b4rrhh.payroll.domain.port;

import com.b4rrhh.payroll.domain.model.Payroll;

import java.util.List;
import java.util.Set;

/**
 * Una página de la búsqueda de recibos, con el total de todos los que cumplen los filtros
 * ({@code b4rrhh/frontend#93}).
 *
 * <p>El total es lo que distingue «no hay más» de «no caben más en esta página». Antes la búsqueda
 * cortaba en 500 sin decirlo, y con la semilla de nueve meses la pantalla afirmaba «500 nóminas
 * encontradas» de 7.908.
 *
 * <p>{@code sharedMonths} son los meses de la página que tienen recibo en más de una presencia del
 * empleado ({@code b4rrhh/frontend#104}): cesado y readmitido en el mismo período. Se cuentan en
 * la base y no en la página, porque la hermana puede estar en otra página o fuera del filtro.
 */
public record PayrollSearchPage(List<Payroll> items, int page, int size, long total, Set<Month> sharedMonths) {

    public PayrollSearchPage {
        items = List.copyOf(items);
        sharedMonths = Set.copyOf(sharedMonths);
    }

    /** Si otra presencia del mismo empleado tiene recibo de este período y tipo. */
    public boolean sharesItsMonth(Payroll payroll) {
        return sharedMonths.contains(Month.of(payroll));
    }

    /** Un recibo sin su presencia: lo que dos presencias del mismo mes tienen en común. */
    public record Month(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String payrollPeriodCode,
            String payrollTypeCode) {

        public static Month of(Payroll payroll) {
            return new Month(
                    payroll.getRuleSystemCode(),
                    payroll.getEmployeeTypeCode(),
                    payroll.getEmployeeNumber(),
                    payroll.getPayrollPeriodCode(),
                    payroll.getPayrollTypeCode());
        }
    }
}

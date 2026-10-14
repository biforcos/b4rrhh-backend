package com.b4rrhh.payroll.domain.port;

import com.b4rrhh.payroll.domain.model.Payroll;

import java.util.List;
import java.util.Map;
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
 *
 * <p>{@code employeeNames} es el nombre de cada empleado de la página ({@code b4rrhh/frontend#143}):
 * al buscar por nombre, la lista tiene que enseñar por qué entra cada fila. El recibo no lo guarda.
 */
public record PayrollSearchPage(
        List<Payroll> items,
        int page,
        int size,
        long total,
        Set<Month> sharedMonths,
        Map<Employee, String> employeeNames) {

    public PayrollSearchPage {
        items = List.copyOf(items);
        sharedMonths = Set.copyOf(sharedMonths);
        employeeNames = Map.copyOf(employeeNames);
    }

    /** Sin nombres: un repositorio que no sabe de empleados. */
    public PayrollSearchPage(List<Payroll> items, int page, int size, long total, Set<Month> sharedMonths) {
        this(items, page, size, total, sharedMonths, Map.of());
    }

    /** El nombre del empleado del recibo, si se conoce. */
    public String employeeNameOf(Payroll payroll) {
        return employeeNames.get(Employee.of(payroll));
    }

    /** Un empleado por su clave de negocio. */
    public record Employee(String ruleSystemCode, String employeeTypeCode, String employeeNumber) {

        public static Employee of(Payroll payroll) {
            return new Employee(payroll.getRuleSystemCode(), payroll.getEmployeeTypeCode(), payroll.getEmployeeNumber());
        }
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

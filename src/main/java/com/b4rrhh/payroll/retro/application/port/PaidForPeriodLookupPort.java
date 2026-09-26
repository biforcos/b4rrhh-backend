package com.b4rrhh.payroll.retro.application.port;

/**
 * Cuanto se ha pagado ya por un mes ({@code backend#133}).
 *
 * <p>Es la segunda lectura de otro periodo que tiene este motor, y la primera fue la base reguladora del
 * {@code backend#128}. Son dos puertos y no uno porque son dos preguntas distintas —una pide un numero
 * de un mes, la otra pide <b>todo lo que se ha pagado atribuido</b> a un mes— y las dos filtran por
 * {@code DEFINITIVE} con el filtro escrito en la consulta (ADR-069 §2, ADR-074). Las dos pasan por el
 * mismo repositorio, que es el que el candado
 * {@code OnlyOnePortReadsAPayrollOfAnotherPeriodTest} vigila.
 *
 * <p><b>Por empleado y no por presencia</b>, a proposito. «Lo cobrado por agosto» es dinero del empleado:
 * si ceso y volvio el mismo mes tiene dos recibos de agosto y los dos son suyos. Que la linea de atraso
 * acabe en el recibo de una presencia o de otra es un detalle documental, y la invariante del
 * {@code #133} se enuncia sobre el empleado por lo mismo — igual que la base de cotizacion de un mes es
 * la del mes y no la de una presencia (ADR-074 §3).
 */
public interface PaidForPeriodLookupPort {

    /**
     * @param periodCode el mes por el que se pregunta, {@code 202608}
     * @return lo pagado por concepto: las lineas propias de los recibos cerrados de ese mes mas las
     *         lineas con {@code origin_period_code} de ese mes en cualquier recibo cerrado
     */
    PaidForPeriod findByEmployeeAndPeriod(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String payrollTypeCode,
            String periodCode
    );
}

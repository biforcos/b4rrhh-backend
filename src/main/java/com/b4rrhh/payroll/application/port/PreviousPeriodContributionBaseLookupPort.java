package com.b4rrhh.payroll.application.port;

/**
 * <b>El unico sitio por donde un calculo lee un recibo de otro periodo</b> ({@code backend#128},
 * ADR-074).
 *
 * <p>Es la primera vez que el motor mira fuera de su periodo, y la regla que se pone aqui la heredan
 * los atrasos del paso 6 de {@code b4rrhh/workspace#9}. Por eso es un puerto y no una consulta mas
 * dentro del calculo: <b>si la lectura pasa por un solo sitio, el filtro se escribe una vez y se
 * puede vigilar</b>. Hay un candado que lo comprueba
 * ({@code OnlyOnePortReadsAPayrollOfAnotherPeriodTest}).
 *
 * <p>Lo que se lee es la <b>base de contingencias comunes del recibo</b>, y del recibo y no de los
 * pasos: el recibo es el documento (ADR-062), y lo que la ley llama base de cotizacion del mes
 * anterior es lo que el recibo de aquel mes dice que fue.
 */
public interface PreviousPeriodContributionBaseLookupPort {

    /**
     * La base de contingencias comunes del periodo anterior de este empleado.
     *
     * <p><b>Solo de recibos {@code DEFINITIVE}</b>, y el filtro va en la consulta y no despues
     * (ADR-069 §2). Si el empleado tiene mas de un recibo en aquel periodo —cese y readmision el
     * mismo mes— se suman: la base de cotizacion de un mes es la del mes, no la de una presencia.
     *
     * @param previousPeriodCode el periodo anterior, ya calculado por quien llama: {@code 202608}
     *        para {@code 202609}. Se pasa hecho y no se deduce aqui porque «el mes anterior» es una
     *        regla de negocio y no una consulta
     */
    PreviousPeriodContributionBase findByEmployeeAndPeriod(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String payrollTypeCode,
            String previousPeriodCode
    );
}

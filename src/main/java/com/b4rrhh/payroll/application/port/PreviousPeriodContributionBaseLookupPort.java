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
     * <p>Si el empleado tiene mas de un recibo en aquel periodo —cese y readmision el mismo mes— se
     * suman: la base de cotizacion de un mes es la del mes, no la de una presencia.
     *
     * @param previousPeriodCode el periodo anterior, ya calculado por quien llama: {@code 202608}
     *        para {@code 202609}. Se pasa hecho y no se deduce aqui porque «el mes anterior» es una
     *        regla de negocio y no una consulta
     * @param source de donde se lee ({@code backend#131}, ADR-076 §3). En el calculo normal, solo del
     *        recibo cerrado, con el filtro escrito en la consulta y no despues (ADR-069 §2). En el
     *        modo retro, del vigente si existe y del recibo cerrado si no: cuando le toca a agosto, el
     *        vigente de julio ya existe y es lo que julio vale <i>ahora</i>
     */
    PreviousPeriodContributionBase findByEmployeeAndPeriod(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String payrollTypeCode,
            String previousPeriodCode,
            PreviousPeriodSource source
    );
}

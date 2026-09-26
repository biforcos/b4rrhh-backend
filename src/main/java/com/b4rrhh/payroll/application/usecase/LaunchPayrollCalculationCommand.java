package com.b4rrhh.payroll.application.usecase;

/**
 * @param retro hasta donde atras recalcula este lanzamiento ({@code backend#132}). Nunca nulo: un
 *              lanzamiento que no hace retro lleva {@link PayrollRetroRequest#none()}
 */
public record LaunchPayrollCalculationCommand(
        String ruleSystemCode,
        String payrollPeriodCode,
        String payrollTypeCode,
        String calculationEngineCode,
        String calculationEngineVersion,
        PayrollLaunchTargetSelection targetSelection,
        // Quien pide la ejecucion. Lo rellena la capa web con el sujeto del token; los
        // lanzamientos en proceso (tests y escenarios) lo dejan nulo porque no hay nadie
        // detras. No se valida contra el modelo de usuarios: es traza, no identidad.
        String requestedBy,
        PayrollRetroRequest retro
) {

    public LaunchPayrollCalculationCommand {
        // Nunca nulo, para que nadie tenga que preguntarse si nulo quiere decir «sin retro» o
        // «me he olvidado». Sin retro se dice con PayrollRetroRequest.none().
        if (retro == null) {
            retro = PayrollRetroRequest.none();
        }
    }

    /**
     * El lanzamiento de siempre: <b>sin retro</b>.
     *
     * <p>Existe para que los treinta y nueve sitios que ya construian este mandato no tengan que hablar
     * de retroactividad para decir que no la usan. Lo que un lector ve en un escenario de otro tema es
     * lo mismo que veia antes.
     */
    public LaunchPayrollCalculationCommand(
            String ruleSystemCode,
            String payrollPeriodCode,
            String payrollTypeCode,
            String calculationEngineCode,
            String calculationEngineVersion,
            PayrollLaunchTargetSelection targetSelection,
            String requestedBy
    ) {
        this(ruleSystemCode, payrollPeriodCode, payrollTypeCode, calculationEngineCode,
                calculationEngineVersion, targetSelection, requestedBy, PayrollRetroRequest.none());
    }
}

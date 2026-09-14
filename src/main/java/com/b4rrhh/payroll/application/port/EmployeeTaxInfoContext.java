package com.b4rrhh.payroll.application.port;

/**
 * La situación fiscal con la que se calcula un recibo, y de dónde salió.
 *
 * <p>El {@code source} no es decorativo ni opcional: va delante porque es lo primero que hay que
 * saber para leer los demás campos. {@code SINGLE_OR_OTHER, 0, 0} significa cosas distintas según
 * lo declarara el empleado o no lo declarara nadie (`backend#92`).
 */
public record EmployeeTaxInfoContext(
    EmployeeTaxInfoSource source,
    String familySituation,
    int descendantsCount,
    int ascendantsCount,
    String disabilityDegree,
    boolean pensionCompensatoria,
    boolean geographicMobility,
    boolean habitualResidenceLoan,
    String taxTerritory
) {
    public EmployeeTaxInfoContext {
        java.util.Objects.requireNonNull(source, "source");
        java.util.Objects.requireNonNull(familySituation, "familySituation");
        java.util.Objects.requireNonNull(disabilityDegree, "disabilityDegree");
        java.util.Objects.requireNonNull(taxTerritory, "taxTerritory");
    }

    /** La situación declarada por el empleado, leída de {@code employee.employee_tax_information}. */
    public static EmployeeTaxInfoContext ofDeclared(
        String familySituation,
        int descendantsCount,
        int ascendantsCount,
        String disabilityDegree,
        boolean pensionCompensatoria,
        boolean geographicMobility,
        boolean habitualResidenceLoan,
        String taxTerritory
    ) {
        return new EmployeeTaxInfoContext(
            EmployeeTaxInfoSource.DECLARED,
            familySituation,
            descendantsCount,
            ascendantsCount,
            disabilityDegree,
            pensionCompensatoria,
            geographicMobility,
            habitualResidenceLoan,
            taxTerritory);
    }

    /**
     * La situación por omisión: soltero, sin descendientes ni ascendientes, sin discapacidad y
     * territorio común.
     *
     * <p><b>Aquí vive la política fiscal por omisión, y es deliberado que viva en código.</b>
     * Decidida en el {@code backend#92}: a quien no tiene declaración vigente se le calcula como
     * soltero sin descendientes ni ascendientes, que es lo que hace cualquier nómina real con
     * quien no ha presentado su modelo 145. Retirarla no es una opción: hoy los 873 recibos de la
     * demo descansan sobre ella y sin valor por omisión serían 873 errores.
     *
     * <p><b>El valor por omisión es silencioso para el cálculo, y ruidoso para el lector.</b> El
     * importe sale igual que antes del {@code backend#92}; lo único que cambia es que el snapshot
     * dice que fue por omisión, porque el snapshot de ADR-059 es <em>lo que el motor tuvo
     * delante</em> y una situación supuesta no puede guardarse con la misma forma que una
     * declarada.
     *
     * <p><b>No es un {@code payroll_warning}, y no por descuido.</b> Con
     * {@code employee.employee_tax_information} vacía el aviso saldría en el 100 % de los recibos,
     * y un aviso que sale siempre no avisa de nada. El día que la semilla traiga datos fiscales el
     * aviso distinguiría, y ese día merece la pena volver a mirarlo.
     *
     * <p><b>No hace falta versionar estos ocho valores.</b> Si mañana cambia la política, los
     * recibos viejos seguirán diciendo cuál era la de entonces: el snapshot guarda los ocho campos
     * al lado del {@code source}, así que la foto de hoy ya dice cuál era el defecto de hoy. Tiene
     * la forma del hash de la reglamentación del {@code workspace#2}, pero no su problema.
     */
    public static EmployeeTaxInfoContext ofDefault() {
        return new EmployeeTaxInfoContext(
            EmployeeTaxInfoSource.DEFAULT_NO_DECLARATION,
            "SINGLE_OR_OTHER", 0, 0, "NONE", false, false, false, "COMUN");
    }
}

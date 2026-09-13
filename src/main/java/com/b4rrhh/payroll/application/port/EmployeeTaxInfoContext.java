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
     * <p>Se usa cuando no hay declaración vigente, y desde el {@code backend#92} se distingue de
     * una declaración que dijera lo mismo.
     */
    public static EmployeeTaxInfoContext ofDefault() {
        return new EmployeeTaxInfoContext(
            EmployeeTaxInfoSource.DEFAULT_NO_DECLARATION,
            "SINGLE_OR_OTHER", 0, 0, "NONE", false, false, false, "COMUN");
    }
}

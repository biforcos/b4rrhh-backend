package com.b4rrhh.employee.extra_payment_regime.application.port;

/**
 * El testigo de prorrateo del convenio ({@code backend#117}): lo que dice el convenio cuando nadie
 * dice otra cosa.
 *
 * <p>Se lee UNA vez, al contratar, y se copia a la vertical del empleado. Es una copia y no un
 * enlace: si el convenio cambia el ano que viene, los que ya estan no cambian solos — se cambian
 * sus filas, en masa si hace falta, y eso deja rastro con su vigencia.
 */
public interface AgreementExtraPaymentProrationLookupPort {

    /**
     * @param ruleSystemCode sistema de reglas del convenio
     * @param agreementCode  clave de negocio del convenio
     * @return si el convenio prorratea las pagas extras por defecto
     * @throws IllegalStateException si el convenio o su perfil no existen
     */
    boolean resolveProratedByDefault(String ruleSystemCode, String agreementCode);
}

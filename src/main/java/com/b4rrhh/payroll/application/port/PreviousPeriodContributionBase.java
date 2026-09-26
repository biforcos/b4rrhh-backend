package com.b4rrhh.payroll.application.port;

import java.math.BigDecimal;

/**
 * Lo que se encontro al mirar el recibo del mes anterior ({@code backend#128}).
 *
 * <p>Son <b>tres respuestas y no dos</b>, y ahi esta la decision del paso: «no hay recibo» y «hay uno
 * que todavia puede cambiar» son cosas distintas y se tratan distinto. Un {@code Optional} solo sabe
 * decir dos, y la que se perderia es justo la que no puede pasar desapercibida.
 *
 * @param outcome que se encontro
 * @param contributionBase la base de contingencias comunes del recibo cerrado, o {@code null} en las
 *        otras dos respuestas. No es la base diaria: dividirla es de quien sabe si la nomina es
 *        mensual o diaria
 */
public record PreviousPeriodContributionBase(
        Outcome outcome,
        BigDecimal contributionBase
) {

    public enum Outcome {
        /** Hay recibo del mes anterior y esta cerrado: su base es la que se lee. */
        DEFINITIVE,
        /**
         * Hay recibo del mes anterior y <b>no</b> esta cerrado.
         *
         * <p>Su base puede cambiar, asi que no se lee: el recibo de este mes no se calcula y queda
         * {@code NOT_VALID} con su motivo, como cuando falta una entrada (ADR-074).
         */
        NOT_DEFINITIVE,
        /**
         * No hay recibo del mes anterior en el sistema.
         *
         * <p>Es la semilla y es la empresa que acaba de empezar a usar B4RRHH. Se usa la base teorica
         * de este mes y el recibo <b>lo dice</b>, salvo que el empleado entrara este mes, que
         * entonces es lo que manda la ley y no hay nada que avisar.
         */
        NONE
    }

    public PreviousPeriodContributionBase {
        if (outcome == null) {
            throw new IllegalArgumentException("outcome must not be null");
        }
        if (outcome == Outcome.DEFINITIVE && contributionBase == null) {
            throw new IllegalArgumentException(
                    "Un recibo cerrado tiene base: DEFINITIVE sin contributionBase no es una"
                            + " respuesta, es un hueco");
        }
        if (outcome != Outcome.DEFINITIVE && contributionBase != null) {
            throw new IllegalArgumentException(
                    "Solo se lee la base de un recibo cerrado (ADR-069, ADR-074): " + outcome
                            + " no puede traer una");
        }
    }

    public static PreviousPeriodContributionBase definitive(BigDecimal contributionBase) {
        return new PreviousPeriodContributionBase(Outcome.DEFINITIVE, contributionBase);
    }

    public static PreviousPeriodContributionBase notDefinitive() {
        return new PreviousPeriodContributionBase(Outcome.NOT_DEFINITIVE, null);
    }

    public static PreviousPeriodContributionBase none() {
        return new PreviousPeriodContributionBase(Outcome.NONE, null);
    }
}

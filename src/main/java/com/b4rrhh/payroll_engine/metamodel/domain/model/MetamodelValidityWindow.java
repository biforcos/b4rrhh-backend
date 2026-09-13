package com.b4rrhh.payroll_engine.metamodel.domain.model;

import com.b4rrhh.payroll_engine.metamodel.domain.exception.ValidityWindowDoesNotCoverWholePeriodsException;

import java.time.LocalDate;

/**
 * La vigencia de una pieza de la reglamentacion, que tiene que cubrir periodos naturales
 * enteros.
 *
 * <p><b>Por que.</b> La reglamentacion de una ejecucion se carga <b>una vez</b>, preguntando
 * por una sola fecha, y esa fecha es el fin del periodo (ADR-061,
 * {@link RuleSystemMetamodel}). Mientras toda vigencia cubra meses enteros, preguntar a fin
 * de periodo y preguntar a cualquier otro dia del periodo devuelven lo mismo, y la carga
 * unica es correcta. En cuanto una vigencia corta un periodo por la mitad deja de serlo: la
 * unidad que se fue el dia 12 se calcularia con la foto del dia 30, que ya no es la suya, y
 * sin que nada avise.
 *
 * <p><b>Por que rechazar y no soportar.</b> Hoy la alternativa a rechazar no es soportarlo:
 * es calcular mal en silencio. Soportar vigencias dentro del periodo significa que la carga
 * siga a los segmentos, y eso es {@code backend#47}. El dia que se haga, se levanta esta
 * validacion. Una validacion se relaja cuando quieras; una nomina calculada con las reglas
 * equivocadas no se arregla hacia atras (decision en {@code backend#89}).
 *
 * <p><b>El periodo es el mes natural</b>, que es lo que declara {@code payrollPeriodCode} en
 * formato {@code YYYYMM}. Asi que la apertura cae en el dia 1 de un mes y el cierre, si lo
 * hay, en el ultimo dia de un mes. Un cierre nulo es una vigencia abierta y siempre vale.
 */
public record MetamodelValidityWindow(LocalDate from, LocalDate to) {

    /**
     * Comprueba una vigencia antes de escribirla.
     *
     * @param what      que se esta escribiendo, para el mensaje: "la asignacion del concepto 101"
     * @param fromField nombre real del campo de apertura en ese camino: {@code validFrom},
     *                  {@code effectiveFrom}, {@code startDate}
     * @param toField   nombre real del campo de cierre: {@code validTo}, {@code effectiveTo},
     *                  {@code endDate}
     */
    public static MetamodelValidityWindow requireWholePeriods(
            LocalDate from,
            LocalDate to,
            String what,
            String fromField,
            String toField
    ) {
        if (from == null) {
            throw new ValidityWindowDoesNotCoverWholePeriodsException(
                    fromField + " es obligatorio en " + what + ".");
        }
        if (to != null && to.isBefore(from)) {
            throw new ValidityWindowDoesNotCoverWholePeriodsException(
                    toField + " (" + to + ") es anterior a " + fromField + " (" + from + ") en "
                            + what + ".");
        }
        if (from.getDayOfMonth() != 1) {
            throw new ValidityWindowDoesNotCoverWholePeriodsException(
                    "La reglamentacion de una ejecucion es una sola y se pregunta una vez por "
                            + "periodo, asi que su vigencia tiene que cubrir meses naturales "
                            + "enteros (ADR-061). " + fromField + " de " + what + " vale " + from
                            + ", y se esperaba el primer dia de un mes: " + from.withDayOfMonth(1)
                            + ".");
        }
        if (to != null && !to.equals(to.withDayOfMonth(to.lengthOfMonth()))) {
            throw new ValidityWindowDoesNotCoverWholePeriodsException(
                    "La reglamentacion de una ejecucion es una sola y se pregunta una vez por "
                            + "periodo, asi que su vigencia tiene que cubrir meses naturales "
                            + "enteros (ADR-061). " + toField + " de " + what + " vale " + to
                            + ", y se esperaba el ultimo dia de un mes: "
                            + to.withDayOfMonth(to.lengthOfMonth())
                            + ". Dejalo a nulo si la vigencia no se cierra.");
        }
        return new MetamodelValidityWindow(from, to);
    }
}

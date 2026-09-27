package com.b4rrhh.payroll.retro.application.port;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Lo que <b>ya se ha pagado</b> por un mes, por concepto ({@code backend#133}).
 *
 * <p>No es lo que su recibo dice: es su recibo <b>mas todos los atrasos de ese mes que ya se pagaron en
 * recibos posteriores</b>. Esa suma es la que hace que el caso que decide el diseno salga bien:
 *
 * <blockquote>
 * Agosto se cerro con cero horas. En septiembre alguien dice que tenia diez, y se pagan diez. En octubre
 * dice que eran veinte, y se pagan <b>diez mas</b>, no veinte.
 * </blockquote>
 *
 * <p>Octubre sabe que son diez y no veinte porque el atraso <b>no se calcula contra el recibo cerrado de
 * agosto, sino contra lo que ya se ha pagado por agosto</b>. Calcularlo contra el recibo daria veinte, y
 * el empleado cobraria treinta por unas horas que valen veinte.
 *
 * @param amountsByConcept importe pagado por concepto. Un concepto que no esta es un concepto por el que
 *        no se ha pagado nada, que es lo mismo que cero y se trata como cero: la regla del cero no
 *        imprime una linea que valga cero (backend#104), asi que «no hay linea» no distingue
 * @param linesByConcept <b>como se pago</b> cada concepto ({@code backend#137}): la identidad de la linea
 *        pagada. Es de donde sale la linea de atraso que devuelve un concepto que ya no esta en el
 *        vigente, y sale de aqui y no del catalogo porque el concepto puede haber cambiado desde
 *        entonces y lo que se devuelve es lo que se cobro
 */
public record PaidForPeriod(Map<String, BigDecimal> amountsByConcept, Map<String, PaidLine> linesByConcept) {

    public PaidForPeriod {
        amountsByConcept = amountsByConcept == null ? Map.of() : Map.copyOf(amountsByConcept);
        linesByConcept = linesByConcept == null ? Map.of() : Map.copyOf(linesByConcept);
    }

    public static PaidForPeriod nothing() {
        return new PaidForPeriod(Map.of(), Map.of());
    }

    public BigDecimal of(String conceptCode) {
        return amountsByConcept.getOrDefault(conceptCode, BigDecimal.ZERO);
    }

    public Optional<PaidLine> lineOf(String conceptCode) {
        return Optional.ofNullable(linesByConcept.get(conceptCode));
    }

    /**
     * La identidad de una linea tal como se pago. Si se pago en varios recibos -el propio y atrasos
     * posteriores- manda la del recibo del propio mes, que es la que el empleado recibio como suya; si
     * no la hay, la del ultimo recibo que pago algo.
     */
    public record PaidLine(
            String conceptMnemonic,
            String conceptLabel,
            String conceptNatureCode,
            Integer displayOrder,
            String payslipSectionCode,
            String payslipSubsectionCode
    ) {
    }
}

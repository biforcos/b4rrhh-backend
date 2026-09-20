package com.b4rrhh.payroll.document.application.service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Un numero del recibo, escrito como lo escribe el folio ({@code backend#112}).
 *
 * <p>La regla de fondo es la del {@code backend#106} y no cambia: <b>se ensena la precision que se
 * uso</b>, dos decimales como minimo y seis como techo, que es el que el esquema le pone a
 * {@code rounding_scale}. Lo prohibido es enseñar {@code 33,33} habiendo usado {@code 33,333333}.
 *
 * <h2>Por que la agrupacion no es la que Java pone por defecto</h2>
 *
 * <p>Porque no coincide con la de la pantalla, y el criterio 5 del issue es que las dos salidas
 * digan lo mismo. El folio formatea con {@code Intl.NumberFormat('es-ES')}, que sigue CLDR, y CLDR
 * declara para el español {@code minimumGroupingDigits = 2}: <b>no se separan los miles hasta que
 * hay cinco digitos enteros</b>. {@code DecimalFormat} no implementa esa regla y agrupa desde
 * cuatro. La base de cotizacion de {@code EMP000001} lo enseña a la primera:
 *
 * <pre>
 *   folio   1323,00      PDF con el defecto de Java   1.323,00
 * </pre>
 *
 * <p>Son el mismo numero y no son el mismo texto, y un papel que se entrega a una persona no puede
 * escribir las cifras de otra manera que la pantalla desde la que se cerro. Asi que la agrupacion
 * se enciende a mano y solo a partir de diez mil, que es exactamente lo que dice esa regla.
 */
public final class PayslipNumbers {

    private static final Locale ES = Locale.forLanguageTag("es-ES");

    /** El umbral de CLDR para el español: cinco digitos enteros. */
    private static final BigDecimal DESDE_DIEZ_MIL = new BigDecimal("10000");

    private PayslipNumbers() {
    }

    /** Un importe, una cantidad o una tarifa. Nulo se escribe como el folio lo pinta: un guion. */
    public static String valor(BigDecimal value) {
        if (value == null) {
            return "—";
        }
        NumberFormat format = NumberFormat.getNumberInstance(ES);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(6);
        format.setGroupingUsed(value.abs().compareTo(DESDE_DIEZ_MIL) >= 0);
        return format.format(value);
    }

    /** Un importe con su moneda, para los totales y el liquido. */
    public static String importeConMoneda(BigDecimal value) {
        return valor(value) + " €";
    }
}

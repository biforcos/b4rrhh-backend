package com.b4rrhh.shared.infrastructure.search;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Buscar por parte de la matricula (b4rrhh/frontend#141): «esto de tener que buscar con toda la
 * matricula (EMP000025) en vez de subcadenas (0025 por ejemplo) es una castaña».
 *
 * <p>Sin prefijo, «25» o «0025», basta el {@code like} de siempre: contiene lo escrito, ceros
 * incluidos, que en «0025» dicen algo. Lo que el {@code like} no encuentra es la matricula escrita
 * con su prefijo y sin los ceros de relleno: «EMP25» o «emp25» por EMP000025. Para eso sale
 * {@link #prefix()} y {@link #digits()}: empieza por el prefijo y lo que queda, sin los ceros de
 * delante, son esos digitos y nada mas, para que «EMP25» no
 * traiga EMP000250 ni EMP000125, y para que la matricula entera del salto desde la ficha siga
 * siendo esa y solo esa. Lo que no tiene forma de matricula —un nombre— no da nada.</p>
 *
 * <p>Lo usan el directorio y la busqueda de recibos, que son dos contextos: por eso vive aqui y no
 * en uno de ellos.</p>
 */
public record EmployeeNumberSearch(String prefix, String digits) {

    private static final Pattern NUMBER = Pattern.compile("^([A-Z]*)0*([0-9]+)$");

    /** Lo escrito leido como matricula, o {@code null} si no lo parece. */
    public static EmployeeNumberSearch of(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = NUMBER.matcher(text.trim().toUpperCase(Locale.ROOT));
        if (!matcher.matches()) {
            return null;
        }
        String prefix = matcher.group(1);
        String digits = matcher.group(2);
        return new EmployeeNumberSearch(prefix.isEmpty() ? null : prefix, digits);
    }

    /** El prefijo de una matricula escrita con el; {@code null} sin prefijo o si no es una matricula. */
    public static String prefixOf(String text) {
        EmployeeNumberSearch search = of(text);
        return search == null ? null : search.prefix();
    }

    public static String digitsOf(String text) {
        EmployeeNumberSearch search = of(text);
        return search == null ? null : search.digits();
    }
}

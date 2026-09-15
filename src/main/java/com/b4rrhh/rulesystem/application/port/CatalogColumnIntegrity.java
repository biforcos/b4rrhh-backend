package com.b4rrhh.rulesystem.application.port;

import java.util.Map;

/**
 * Lo que se ha visto en una columna de catálogo declarada: cuántas filas se miraron y cuántas
 * apuntan a un código que no existe en {@code rulesystem.rule_entity} (backend#44).
 *
 * <p><b>El denominador es parte del resultado, no adorno.</b> Una columna con cero huérfanos y
 * cero filas y una columna con cero huérfanos y treinta mil filas dicen cosas muy distintas, y
 * hasta el {@code backend#43} la comprobación solo sabía decir «vacío». En el pipeline, sobre una
 * base recién migrada, {@code rows} es cero en todas: eso <b>es</b> el hallazgo, y por eso viaja
 * en el resultado en vez de quedarse en un verde sin cifras.
 *
 * @param qualifiedColumn    {@code tabla.columna}, como la nombra el participante
 * @param ruleEntityTypeCode el tipo de catálogo que esa columna guarda
 * @param rows               filas con la columna no nula, vigentes o no
 * @param orphanRows         de esas, las que apuntan a un código inexistente en su reglamentación
 * @param orphanCodes        cada {@code reglamentación/código} huérfano con su recuento
 */
public record CatalogColumnIntegrity(
        String qualifiedColumn,
        String ruleEntityTypeCode,
        long rows,
        long orphanRows,
        Map<String, Long> orphanCodes
) {

    public CatalogColumnIntegrity {
        orphanCodes = Map.copyOf(orphanCodes);
    }

    public boolean hasOrphans() {
        return orphanRows > 0;
    }

    /** Una columna sin una sola fila no prueba nada: no hay dato que comprobar. */
    public boolean isEmpty() {
        return rows == 0;
    }
}

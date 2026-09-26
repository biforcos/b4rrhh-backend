package com.b4rrhh.payroll.retro.domain.model;

/**
 * De donde sale una marca: la vertical, la tabla y la fila ({@code backend#130}).
 *
 * <p>Son dos formas de decir «que fila» y no una, porque este arbol tiene las dos: casi todas las
 * verticales se identifican por un id surrogado, y las entradas de nomina por su clave de negocio
 * —concepto y periodo— sin id en el modelo de dominio y a proposito. Sin {@code rowKey}, la marca de
 * unas horas extra no podria decir de que concepto eran.
 *
 * @param verticalCode la vertical, que es lo que se ensena y por lo que se agrupa
 * @param table        la tabla con esquema
 * @param rowId        la fila, o nulo si la escritura fue un borrado —entonces la marca es lo unico
 *                     que queda de ella— o si la vertical no tiene id
 * @param rowKey       la clave de negocio de la fila, en texto, cuando el id no la identifica
 */
public record RetroMarkSource(String verticalCode, String table, Long rowId, String rowKey) {

    public RetroMarkSource {
        if (verticalCode == null || verticalCode.isBlank()) {
            throw new IllegalArgumentException("verticalCode es obligatorio en una marca de retroactividad");
        }
        if (table == null || table.isBlank()) {
            throw new IllegalArgumentException("table es obligatoria en una marca de retroactividad");
        }
    }
}

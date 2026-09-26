package com.b4rrhh.payroll.retro.domain.model;

/**
 * Los tres estados de una marca de retroactividad ({@code backend#130}).
 *
 * <p>No hay un cuarto y no hay borrado. Una marca que desaparece se lleva con ella la unica prueba de
 * que hubo una correccion conocida, y el recibo tiene que poder contar que la habia aunque alguien
 * decidiera no pagarla.
 */
public enum RetroMarkStatus {

    /** Pendiente: el motor la va a tener en cuenta en el proximo lanzamiento. */
    ACTIVE,

    /** Alguien decidio que no se paga, con su nombre y su motivo. Sigue visible. */
    DISCARDED,

    /** Ya la pago un recibo definitivo, y la fila dice cual. */
    CONSUMED;

    public boolean isActive() {
        return this == ACTIVE;
    }
}

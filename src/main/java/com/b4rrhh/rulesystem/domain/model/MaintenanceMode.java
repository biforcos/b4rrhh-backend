package com.b4rrhh.rulesystem.domain.model;

/**
 * El modo de mantenimiento de un tipo de entidad (ADR-054 §4): qué puede hacer el usuario
 * con sus filas. No es un booleano de visibilidad — la diferencia entre {@link #REFERENCE}
 * y {@link #CLOSED} es justo la que un booleano borra. No hay valor «oculto» a propósito:
 * se añadirá el día que exista un tipo que de verdad no deba salir en ninguna pantalla.
 */
public enum MaintenanceMode {

    /** El usuario da de alta y edita. */
    MAINTAINED,

    /** Sólo lectura para el usuario, pero una fila nueva es sólo un dato más. */
    REFERENCE,

    /**
     * Sólo lectura y una fila nueva exige que alguien le dé significado, porque cada valor
     * lleva comportamiento aparejado — en datos, no en constantes: las filas de ámbito de
     * {@code concept_assignment} deciden qué conceptos de nómina aplican por tipo de
     * empleado. Sembrar un código sin decidir eso deja una pregunta abierta en el motor.
     */
    CLOSED
}

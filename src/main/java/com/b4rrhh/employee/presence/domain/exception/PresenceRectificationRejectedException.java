package com.b4rrhh.employee.presence.domain.exception;

/**
 * Una rectificación de presencia que no se hace, con el porqué en palabras (backend#172).
 */
public class PresenceRectificationRejectedException extends RuntimeException {

    /** Por qué, en un código que la pantalla puede distinguir. */
    public enum Reason {
        /** El empleado o la presencia no existen. */
        NOT_FOUND,
        /** La fecha pedida no vale: la misma, o un cese que no es posterior al alta, o una presencia abierta. */
        INVALID,
        /** Pisaría otra presencia del mismo empleado. */
        OVERLAP,
        /** La fecha ya se pagó: hay recibos definitivos. */
        PAID,
        /** Algo de la presencia no se puede mover con ella. */
        CONFLICT
    }

    private final Reason reason;

    public PresenceRectificationRejectedException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }

    public String code() {
        return "PRESENCE_RECTIFICATION_" + reason.name();
    }
}

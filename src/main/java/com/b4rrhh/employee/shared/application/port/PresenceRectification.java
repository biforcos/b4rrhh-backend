package com.b4rrhh.employee.shared.application.port;

import java.time.LocalDate;

/**
 * La rectificación de una fecha de una presencia (backend#172): la de alta o la de cese, de la fecha
 * que tiene a la que debía tener. No es una edición de campo: es el alta (o el cese) que se tecleó mal.
 *
 * @param presenceStart el inicio de la presencia antes de rectificar
 * @param presenceEnd   el fin de la presencia antes de rectificar; null si sigue abierta
 */
public record PresenceRectification(
        Long employeeId,
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        Integer presenceNumber,
        Field field,
        LocalDate presenceStart,
        LocalDate presenceEnd,
        LocalDate newDate
) {

    /** Qué fecha se rectifica. */
    public enum Field {
        START_DATE,
        END_DATE
    }

    /** La fecha que había. */
    public LocalDate oldDate() {
        return field == Field.START_DATE ? presenceStart : presenceEnd;
    }

    /** El inicio de la presencia después de rectificar. */
    public LocalDate newStart() {
        return field == Field.START_DATE ? newDate : presenceStart;
    }

    /** El fin de la presencia después de rectificar; null si sigue abierta. */
    public LocalDate newEnd() {
        return field == Field.END_DATE ? newDate : presenceEnd;
    }

    /** Si una fila con estas fechas cae en la presencia tal como era: es de esta presencia. */
    public boolean belongsToThePresence(LocalDate start, LocalDate end) {
        boolean startsBeforeItEnds = presenceEnd == null || !start.isAfter(presenceEnd);
        boolean endsAfterItStarts = end == null || !end.isBefore(presenceStart);
        return startsBeforeItEnds && endsAfterItStarts;
    }
}

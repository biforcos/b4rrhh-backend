package com.b4rrhh.employee.shared.application.port;

/**
 * Un vertical que tiene algo que decir cuando se rectifica la fecha de alta o de cese de una presencia
 * (backend#172). El mismo patrón que {@code HireParticipant} y {@code TerminationParticipant}
 * (ADR-047): el orquestador no conoce los verticales; cada uno se engancha implementándolo.
 *
 * <p>Primero se pide el plan a todos, y sólo si ninguno pone un conflicto se aplica: un vertical que
 * no puede moverse para la rectificación entera, no la deja a medias. El plan dice también lo que se
 * movería, que es lo que enseña la pantalla antes de confirmar.</p>
 */
public interface PresenceRectificationParticipant {

    /** Lo que movería y lo que impide moverlo; no escribe nada. */
    PresenceRectificationPlan plan(PresenceRectification rectification);

    /** Mueve lo que dijo el plan. Sólo se llama si ningún participante puso un conflicto. */
    void apply(PresenceRectification rectification);
}

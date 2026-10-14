package com.b4rrhh.employee.presence.application.port;

import com.b4rrhh.employee.shared.application.port.PresenceRectification;

/** El rastro de una rectificación de presencia (backend#172): quién, de qué fecha a cuál y qué movió. */
public interface PresenceRectificationTrailPort {

    void record(PresenceRectification rectification, String rectifiedBy, String moved);
}

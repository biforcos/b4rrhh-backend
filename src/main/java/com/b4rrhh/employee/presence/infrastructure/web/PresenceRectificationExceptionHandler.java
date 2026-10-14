package com.b4rrhh.employee.presence.infrastructure.web;

import com.b4rrhh.employee.presence.domain.exception.PresenceRectificationRejectedException;
import com.b4rrhh.employee.presence.infrastructure.web.dto.PresenceRectificationErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Las negativas de la rectificación, con su código y el porqué en palabras (backend#172). */
@RestControllerAdvice(assignableTypes = PresenceRectificationController.class)
public class PresenceRectificationExceptionHandler {

    @ExceptionHandler(PresenceRectificationRejectedException.class)
    public ResponseEntity<PresenceRectificationErrorResponse> handleRejected(PresenceRectificationRejectedException ex) {
        HttpStatus status = switch (ex.reason()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID -> HttpStatus.BAD_REQUEST;
            case OVERLAP, PAID, CONFLICT -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(status).body(new PresenceRectificationErrorResponse(ex.code(), ex.getMessage()));
    }
}

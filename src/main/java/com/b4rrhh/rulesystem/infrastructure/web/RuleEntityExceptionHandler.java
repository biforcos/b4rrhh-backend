package com.b4rrhh.rulesystem.infrastructure.web;

import com.b4rrhh.rulesystem.domain.exception.RuleEntityAlreadyClosedException;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityInUseException;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityInvalidDateRangeException;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityNotFoundException;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityOverlapException;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityTypeIsMaintainedByItsOwnEndpointException;
import com.b4rrhh.rulesystem.infrastructure.web.dto.RuleEntityErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {RuleEntityController.class, RuleEntityBusinessKeyController.class})
public class RuleEntityExceptionHandler {

    @ExceptionHandler(RuleEntityNotFoundException.class)
    public ResponseEntity<RuleEntityErrorResponse> handleNotFound(RuleEntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new RuleEntityErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(RuleEntityInUseException.class)
    public ResponseEntity<RuleEntityErrorResponse> handleInUse(RuleEntityInUseException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new RuleEntityErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler({
            RuleEntityAlreadyClosedException.class,
            RuleEntityInvalidDateRangeException.class,
            RuleEntityOverlapException.class
    })
    public ResponseEntity<RuleEntityErrorResponse> handleConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new RuleEntityErrorResponse(ex.getMessage()));
    }

    // 400 y no 409: no hay conflicto con nada que exista, la peticion esta mal dirigida
    // (backend#88). El mensaje dice a donde ir, asi que no se recorta.
    @ExceptionHandler(RuleEntityTypeIsMaintainedByItsOwnEndpointException.class)
    public ResponseEntity<RuleEntityErrorResponse> handleMaintainedByItsOwnEndpoint(
            RuleEntityTypeIsMaintainedByItsOwnEndpointException ex
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RuleEntityErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RuleEntityErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RuleEntityErrorResponse(ex.getMessage()));
    }
}

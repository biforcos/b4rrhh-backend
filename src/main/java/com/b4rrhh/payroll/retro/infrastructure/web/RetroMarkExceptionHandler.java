package com.b4rrhh.payroll.retro.infrastructure.web;

import com.b4rrhh.payroll.retro.domain.exception.RetroMarkNotActiveException;
import com.b4rrhh.payroll.retro.domain.exception.RetroMarkNotFoundException;
import com.b4rrhh.payroll.retro.infrastructure.web.dto.RetroMarkErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {RetroMarkController.class, EmployeeRetroMarkController.class})
public class RetroMarkExceptionHandler {

    @ExceptionHandler(RetroMarkNotFoundException.class)
    public ResponseEntity<RetroMarkErrorResponse> handleNotFound(RetroMarkNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new RetroMarkErrorResponse("RETRO_MARK_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(RetroMarkNotActiveException.class)
    public ResponseEntity<RetroMarkErrorResponse> handleNotActive(RetroMarkNotActiveException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new RetroMarkErrorResponse("RETRO_MARK_NOT_ACTIVE", ex.getMessage()));
    }

    /** Un descarte sin motivo. El dominio lo rechaza y aqui se cuenta con el codigo que le toca. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RetroMarkErrorResponse> handleMissingReason(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new RetroMarkErrorResponse("RETRO_MARK_DISCARD_REASON_REQUIRED", ex.getMessage()));
    }
}

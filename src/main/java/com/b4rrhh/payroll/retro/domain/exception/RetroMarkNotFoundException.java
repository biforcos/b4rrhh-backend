package com.b4rrhh.payroll.retro.domain.exception;

public class RetroMarkNotFoundException extends RuntimeException {
    public RetroMarkNotFoundException(Long id) {
        super("No existe la marca de retroactividad " + id);
    }
}

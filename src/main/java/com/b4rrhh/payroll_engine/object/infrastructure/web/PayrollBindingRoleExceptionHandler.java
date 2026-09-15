package com.b4rrhh.payroll_engine.object.infrastructure.web;

import com.b4rrhh.payroll_engine.object.domain.exception.BindingRoleAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = PayrollBindingRoleController.class)
public class PayrollBindingRoleExceptionHandler {

    @ExceptionHandler(BindingRoleAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleAlreadyExists(BindingRoleAlreadyExistsException e) {
        return Map.of("error", e.getMessage());
    }
}

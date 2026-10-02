package com.b4rrhh.geo.territory.infrastructure.web;

import com.b4rrhh.geo.territory.domain.exception.InvalidTerritoryQueryException;
import com.b4rrhh.geo.territory.domain.exception.TerritoryRuleSystemNotFoundException;
import com.b4rrhh.geo.territory.infrastructure.web.dto.TerritoryErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = TerritoryBusinessKeyController.class)
public class TerritoryExceptionHandler {

    @ExceptionHandler(TerritoryRuleSystemNotFoundException.class)
    public ResponseEntity<TerritoryErrorResponse> handleNotFound(TerritoryRuleSystemNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new TerritoryErrorResponse("TERRITORY_RULE_SYSTEM_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler({
            InvalidTerritoryQueryException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<TerritoryErrorResponse> handleBadRequest(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new TerritoryErrorResponse("TERRITORY_INVALID_QUERY", ex.getMessage()));
    }
}

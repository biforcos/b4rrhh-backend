package com.b4rrhh.rulesystem.company.infrastructure.web;

import com.b4rrhh.geo.address.domain.exception.PostalAddressRejectedException;
import com.b4rrhh.rulesystem.company.domain.exception.CompanyAlreadyExistsException;
import com.b4rrhh.rulesystem.company.domain.exception.CompanyNotApplicableException;
import com.b4rrhh.rulesystem.company.domain.exception.CompanyNotFoundException;
import com.b4rrhh.rulesystem.company.infrastructure.web.dto.CompanyErrorResponse;
import com.b4rrhh.rulesystem.companyprofile.domain.exception.CompanyProfileNotFoundException;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityIsMaintainedFromItsLayerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CompanyController.class)
public class CompanyExceptionHandler {

    @ExceptionHandler({
            CompanyNotFoundException.class,
            CompanyProfileNotFoundException.class
    })
    public ResponseEntity<CompanyErrorResponse> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new CompanyErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler({
            CompanyAlreadyExistsException.class,
            CompanyNotApplicableException.class,
            RuleEntityIsMaintainedFromItsLayerException.class
    })
    public ResponseEntity<CompanyErrorResponse> handleConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new CompanyErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler({
            PostalAddressRejectedException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<CompanyErrorResponse> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new CompanyErrorResponse(ex.getMessage()));
    }
}

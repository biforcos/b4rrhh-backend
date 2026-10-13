package com.b4rrhh.rulesystem.companyprofile.infrastructure.web;

import com.b4rrhh.geo.address.domain.exception.PostalAddressRejectedException;
import com.b4rrhh.rulesystem.companyprofile.domain.exception.CompanyProfileCompanyNotFoundException;
import com.b4rrhh.rulesystem.companyprofile.domain.exception.CompanyProfileCompanyNotApplicableException;
import com.b4rrhh.rulesystem.companyprofile.domain.exception.CompanyProfileNotFoundException;
import com.b4rrhh.rulesystem.companyprofile.infrastructure.web.dto.CompanyProfileErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CompanyProfileController.class)
public class CompanyProfileExceptionHandler {

    @ExceptionHandler({
            CompanyProfileCompanyNotFoundException.class,
            CompanyProfileCompanyNotApplicableException.class,
            CompanyProfileNotFoundException.class
    })
    public ResponseEntity<CompanyProfileErrorResponse> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new CompanyProfileErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler({
            PostalAddressRejectedException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<CompanyProfileErrorResponse> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new CompanyProfileErrorResponse(ex.getMessage()));
    }
}
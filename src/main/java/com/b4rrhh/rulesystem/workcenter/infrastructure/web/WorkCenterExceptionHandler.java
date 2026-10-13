package com.b4rrhh.rulesystem.workcenter.infrastructure.web;

import com.b4rrhh.geo.address.domain.exception.PostalAddressRejectedException;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityIsMaintainedFromItsLayerException;
import com.b4rrhh.rulesystem.workcenter.domain.exception.WorkCenterAlreadyExistsException;
import com.b4rrhh.rulesystem.workcenter.domain.exception.WorkCenterCompanyInvalidException;
import com.b4rrhh.rulesystem.workcenter.domain.exception.WorkCenterContactAlreadyExistsException;
import com.b4rrhh.rulesystem.workcenter.domain.exception.WorkCenterContactNotFoundException;
import com.b4rrhh.rulesystem.workcenter.domain.exception.WorkCenterContactTypeInvalidException;
import com.b4rrhh.rulesystem.workcenter.domain.exception.WorkCenterNotApplicableException;
import com.b4rrhh.rulesystem.workcenter.domain.exception.WorkCenterNotFoundException;
import com.b4rrhh.rulesystem.workcenter.infrastructure.web.dto.WorkCenterErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(name = "rulesystemWorkCenterExceptionHandler", assignableTypes = {WorkCenterController.class, WorkCenterContactController.class})
public class WorkCenterExceptionHandler {

    @ExceptionHandler({
            WorkCenterNotFoundException.class,
            WorkCenterContactNotFoundException.class
    })
    public ResponseEntity<WorkCenterErrorResponse> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new WorkCenterErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler({
            WorkCenterAlreadyExistsException.class,
            WorkCenterNotApplicableException.class,
            WorkCenterContactAlreadyExistsException.class,
            RuleEntityIsMaintainedFromItsLayerException.class
    })
    public ResponseEntity<WorkCenterErrorResponse> handleConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new WorkCenterErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler({
            WorkCenterCompanyInvalidException.class,
            PostalAddressRejectedException.class,
            WorkCenterContactTypeInvalidException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<WorkCenterErrorResponse> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new WorkCenterErrorResponse(ex.getMessage()));
    }
}
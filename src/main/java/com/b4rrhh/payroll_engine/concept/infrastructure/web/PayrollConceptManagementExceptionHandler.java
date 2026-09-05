package com.b4rrhh.payroll_engine.concept.infrastructure.web;

import com.b4rrhh.payroll_engine.concept.domain.exception.OperandCrossesSegmentToPeriodException;
import com.b4rrhh.payroll_engine.concept.domain.exception.PayrollConceptAlreadyExistsException;
import com.b4rrhh.payroll_engine.concept.domain.exception.PayrollConceptNotFoundException;
import com.b4rrhh.payroll_engine.object.domain.exception.PayrollObjectNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.b4rrhh.payroll_engine")
public class PayrollConceptManagementExceptionHandler {

    @ExceptionHandler(PayrollConceptAlreadyExistsException.class)
    public ResponseEntity<PayrollConceptErrorResponse> handleAlreadyExists(PayrollConceptAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new PayrollConceptErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(PayrollConceptNotFoundException.class)
    public ResponseEntity<PayrollConceptErrorResponse> handleNotFound(PayrollConceptNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new PayrollConceptErrorResponse(ex.getMessage()));
    }

    /**
     * Surfaces missing-source-object problems raised by the wiring layer (operands and
     * feeds) as a 422: the caller's business intent is known but cannot be satisfied
     * because a referenced upstream object does not exist in the target rule system.
     */
    @ExceptionHandler(PayrollObjectNotFoundException.class)
    public ResponseEntity<PayrollConceptErrorResponse> handlePayrollObjectNotFound(PayrollObjectNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new PayrollConceptErrorResponse(ex.getMessage()));
    }

    /**
     * An operand edge that crosses from SEGMENT to PERIOD (ADR-058) is a well-formed
     * request the graph cannot accept: 422, with the operand and both concepts named.
     */
    @ExceptionHandler(OperandCrossesSegmentToPeriodException.class)
    public ResponseEntity<PayrollConceptErrorResponse> handleOperandCrossesSegmentToPeriod(
            OperandCrossesSegmentToPeriodException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new PayrollConceptErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<PayrollConceptErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new PayrollConceptErrorResponse(ex.getMessage()));
    }
}

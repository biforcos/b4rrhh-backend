package com.b4rrhh.employee.address.infrastructure.web;

import com.b4rrhh.employee.address.domain.exception.AddressAlreadyClosedException;
import com.b4rrhh.employee.address.domain.exception.AddressCatalogValueInvalidException;
import com.b4rrhh.employee.address.domain.exception.AddressCoverageGapException;
import com.b4rrhh.employee.address.domain.exception.AddressEmployeeNotFoundException;
import com.b4rrhh.employee.address.domain.exception.AddressIsACorrectionException;
import com.b4rrhh.employee.address.domain.exception.AddressNotFoundException;
import com.b4rrhh.employee.address.domain.exception.AddressOverlapException;
import com.b4rrhh.employee.address.domain.exception.AddressRuleSystemNotFoundException;
import com.b4rrhh.employee.address.domain.exception.AddressTypeCoverageNotDeclaredException;
import com.b4rrhh.employee.address.domain.exception.InvalidAddressDateRangeException;
import com.b4rrhh.employee.address.domain.model.AddressOccurrence;
import com.b4rrhh.employee.address.domain.model.AddressPeriod;
import com.b4rrhh.employee.address.infrastructure.web.dto.AddressErrorResponse;
import com.b4rrhh.employee.address.infrastructure.web.dto.AddressOccurrenceResponse;
import com.b4rrhh.employee.address.infrastructure.web.dto.AddressPeriodResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice(assignableTypes = AddressBusinessKeyController.class)
public class AddressExceptionHandler {

    @ExceptionHandler({
            AddressEmployeeNotFoundException.class,
            AddressNotFoundException.class,
            AddressRuleSystemNotFoundException.class
    })
    public ResponseEntity<AddressErrorResponse> handleNotFound(RuntimeException ex) {
        return respond(HttpStatus.NOT_FOUND, "ADDRESS_NOT_FOUND", ex.getMessage(), null);
    }

    @ExceptionHandler({
            AddressCatalogValueInvalidException.class,
            InvalidAddressDateRangeException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<AddressErrorResponse> handleBadRequest(RuntimeException ex) {
        return respond(HttpStatus.BAD_REQUEST, "ADDRESS_INVALID_REQUEST", ex.getMessage(), null);
    }

    @ExceptionHandler({
            AddressAlreadyClosedException.class,
            AddressOverlapException.class,
            AddressCoverageGapException.class,
            AddressIsACorrectionException.class,
            AddressTypeCoverageNotDeclaredException.class
    })
    public ResponseEntity<AddressErrorResponse> handleConflict(RuntimeException ex) {
        if (ex instanceof AddressIsACorrectionException correction) {
            AddressOccurrence corrected = correction.correctedOccurrence();
            return respond(
                    HttpStatus.CONFLICT,
                    "ADDRESS_IS_A_CORRECTION",
                    "La fecha de inicio coincide con la de la dirección n.º " + corrected.addressNumber()
                            + ": esto no añade una dirección, corrige esa. Confírmalo como corrección.",
                    Map.of("correctedOccurrence", occurrences(List.of(corrected)).get(0))
            );
        }
        if (ex instanceof AddressOverlapException overlap) {
            return respond(
                    HttpStatus.CONFLICT,
                    "ADDRESS_OVERLAP",
                    "El periodo informado se solapa con otra dirección del mismo tipo del empleado.",
                    overlap.overlaps().isEmpty() ? null : Map.of("overlaps", periods(overlap.overlaps()))
            );
        }
        if (ex instanceof AddressCoverageGapException gap) {
            return respond(
                    HttpStatus.CONFLICT,
                    "ADDRESS_COVERAGE_GAP",
                    "La dirección dejaría sin domicilio un tramo de la presencia del empleado.",
                    Map.of(
                            "addressTypeCode", gap.addressTypeCode(),
                            "gaps", periods(gap.gaps()),
                            "stretchCandidates", occurrences(gap.stretchCandidates())
                    )
            );
        }
        if (ex instanceof AddressTypeCoverageNotDeclaredException notDeclared) {
            return respond(
                    HttpStatus.CONFLICT,
                    "ADDRESS_TYPE_COVERAGE_NOT_DECLARED",
                    "El catálogo no dice si el tipo de dirección " + notDeclared.addressTypeCode()
                            + " es obligatorio u opcional; hay que declararlo antes de usarlo.",
                    Map.of("addressTypeCode", notDeclared.addressTypeCode())
            );
        }

        return respond(HttpStatus.CONFLICT, "ADDRESS_ALREADY_CLOSED", ex.getMessage(), null);
    }

    private static List<AddressPeriodResponse> periods(List<AddressPeriod> periods) {
        return periods.stream()
                .map(period -> new AddressPeriodResponse(period.startDate(), period.endDate()))
                .toList();
    }

    private static List<AddressOccurrenceResponse> occurrences(List<AddressOccurrence> occurrences) {
        return occurrences.stream()
                .map(occurrence -> new AddressOccurrenceResponse(
                        occurrence.addressNumber(),
                        occurrence.startDate(),
                        occurrence.endDate()
                ))
                .toList();
    }

    private static ResponseEntity<AddressErrorResponse> respond(
            HttpStatus status,
            String code,
            String message,
            Map<String, Object> details
    ) {
        return ResponseEntity.status(status).body(new AddressErrorResponse(code, message, details));
    }
}

package com.b4rrhh.payroll.infrastructure.web;

import com.b4rrhh.payroll.application.usecase.PayrollLaunchInputMissingException;
import com.b4rrhh.payroll.domain.exception.PayrollCalculationFailedException;
import com.b4rrhh.payroll.domain.exception.PayrollTypeInvalidException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PayrollExceptionHandlerTest {

    private final PayrollExceptionHandler handler = new PayrollExceptionHandler();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsPayrollTypeInvalidExceptionToBadRequest() {
        var ex = new PayrollTypeInvalidException("ORD");

        var response = handler.handleBadRequest(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid payrollTypeCode: 'ORD'", response.getBody().message());
    }

    /**
     * Un calculo que falla ya no contesta un 500 mudo: contesta 422 con el codigo con el que el
     * lanzamiento masivo llama a esto mismo, y con el mensaje del motor, que es el que nombra lo
     * que faltaba (#100).
     */
    @Test
    void mapsCalculationFailureToUnprocessableWithTheRunMessageCode() {
        var ex = new PayrollCalculationFailedException(new IllegalStateException(
                "Configuration error: No table binding found for agreement 99002405011982"
                        + " and role P02_DAILY_AMOUNT_TABLE"));

        var response = handler.handleCalculationFailed(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals("UNIT_CALCULATION_ERROR", response.getBody().code());
        assertThat(response.getBody().message()).contains("P02_DAILY_AMOUNT_TABLE");
        assertEquals(Map.of("exceptionType", "IllegalStateException"), response.getBody().details());
    }

    /** Y la unidad elegible a la que le falta un dato no se confunde con la que fallo calculando. */
    @Test
    void keepsTheMissingInputFailureApartFromTheCalculationError() {
        var ex = new PayrollLaunchInputMissingException(
                "MISSING_WORKING_TIME", "No working time for the segment", Map.of("segment", "1"));

        var response = handler.handleInputMissing(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals("UNIT_ELIGIBLE_REAL_SKIPPED_MISSING_INPUT", response.getBody().code());
        assertThat(response.getBody().details())
                .containsEntry("reasonCode", "MISSING_WORKING_TIME")
                .containsEntry("segment", "1");
    }

    /**
     * Y los errores que ya existian contestan exactamente lo mismo que antes. Ensanchar el cuerpo
     * habria sido cambiarlo para todos si los nulos viajaran: el {@code NON_NULL} es lo que hace
     * que no viajen, y esto es lo que se pone rojo si alguien lo quita.
     */
    @Test
    void theErrorsThatAlreadyExistedAnswerTheSameBytes() throws Exception {
        var response = handler.handleBadRequest(new PayrollTypeInvalidException("ORD"));

        assertEquals(
                "{\"message\":\"Invalid payrollTypeCode: 'ORD'\"}",
                objectMapper.writeValueAsString(response.getBody()));
    }
}

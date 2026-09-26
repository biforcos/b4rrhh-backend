package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.model.TechnicalConceptSegmentData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * {@code A_DED}: uno de los tres totales de atrasos que este recibo paga ({@code backend#133}, V162).
 *
 * <p>Alimenta a un agregado que ya existia, asi que las lineas de atraso llegan a los totales <b>dentro
 * del grafo</b>. Sumarlas en Java despues de calcular habria dejado un total que vale una cosa en la tabla
 * y otra en el grafo, o sea un recibo que no se puede explicar.
 *
 * <p>Lo que <b>no</b> hace esta clase es leer nada ni calcular nada: recibe el numero hecho, que la unidad
 * resuelve antes de ejecutar el grafo. Es la excepcion que el ADR-074 §4 deja escrita para los
 * calculadores tecnicos, y solo vale porque este valor se <b>busca</b> y no se calcula.
 */
@Component
public class ArrearsEmployeeDeductionsConceptCalculator implements TechnicalConceptCalculator {

    @Override
    public String conceptCode() {
        return "A_DED";
    }

    @Override
    public BigDecimal resolve(TechnicalConceptSegmentData context) {
        BigDecimal total = context.retroArrears().employeeDeductions();
        return total == null ? BigDecimal.ZERO : total;
    }
}

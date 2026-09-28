package com.b4rrhh.payroll.domain.port;

import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollStatus;

import java.util.List;
import java.util.Optional;

public interface PayrollRepository {

    Optional<Payroll> findByBusinessKey(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String payrollPeriodCode,
            String payrollTypeCode,
            Integer presenceNumber
    );

    /**
     * Una página de los recibos que cumplen los filtros, con el total. El orden es el período más
     * reciente primero y, dentro de un período, lo que no está cerrado antes que lo cerrado: así el
     * primero de una búsqueda sin filtros es del período abierto, y la lista de un empleado empieza
     * por su mes en curso ({@code b4rrhh/frontend#93}).
     */
    PayrollSearchPage findPageByFilters(
            String ruleSystemCode,
            String payrollPeriodCode,
            String employeeNumber,
            PayrollStatus status,
            int page,
            int size);

    Payroll save(Payroll payroll);

    void deleteById(Long id);

    void flush();

}
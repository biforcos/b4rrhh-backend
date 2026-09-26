package com.b4rrhh.payroll.retro.infrastructure.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface SpringDataCurrentCalculationRepository extends JpaRepository<CurrentCalculationEntity, Long> {

    @EntityGraph(attributePaths = {"concepts"})
    Optional<CurrentCalculationEntity> findByRuleSystemCodeAndEmployeeTypeCodeAndEmployeeNumberAndPayrollPeriodCodeAndPayrollTypeCodeAndPresenceNumber(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String payrollPeriodCode,
            String payrollTypeCode,
            Integer presenceNumber
    );

    /**
     * La suma de una linea del vigente entre las presencias de ese mes ({@code backend#131}).
     *
     * <p>Se suma por lo mismo que en el recibo: un empleado puede tener dos presencias en el mismo mes
     * —cese y readmision— y la base de cotizacion del mes es la del mes. Devuelve {@code null} si no
     * hay ningun vigente de ese mes, que es lo que distingue «no hay vigente» de «hay uno y esa linea
     * vale cero».
     */
    @Query("select sum(c.amount) from CurrentCalculationEntity v join v.concepts c"
            + " where v.ruleSystemCode = :ruleSystemCode"
            + "   and v.employeeTypeCode = :employeeTypeCode"
            + "   and v.employeeNumber = :employeeNumber"
            + "   and v.payrollPeriodCode = :payrollPeriodCode"
            + "   and v.payrollTypeCode = :payrollTypeCode"
            + "   and c.conceptCode = :conceptCode")
    BigDecimal sumConceptAmount(
            @Param("ruleSystemCode") String ruleSystemCode,
            @Param("employeeTypeCode") String employeeTypeCode,
            @Param("employeeNumber") String employeeNumber,
            @Param("payrollPeriodCode") String payrollPeriodCode,
            @Param("payrollTypeCode") String payrollTypeCode,
            @Param("conceptCode") String conceptCode
    );

    /** Si hay algun vigente de ese mes, sea cual sea su contenido. */
    @Query("select count(v) from CurrentCalculationEntity v"
            + " where v.ruleSystemCode = :ruleSystemCode"
            + "   and v.employeeTypeCode = :employeeTypeCode"
            + "   and v.employeeNumber = :employeeNumber"
            + "   and v.payrollPeriodCode = :payrollPeriodCode"
            + "   and v.payrollTypeCode = :payrollTypeCode")
    long countByEmployeeAndPeriod(
            @Param("ruleSystemCode") String ruleSystemCode,
            @Param("employeeTypeCode") String employeeTypeCode,
            @Param("employeeNumber") String employeeNumber,
            @Param("payrollPeriodCode") String payrollPeriodCode,
            @Param("payrollTypeCode") String payrollTypeCode
    );
}

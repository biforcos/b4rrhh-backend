package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.payroll.domain.model.PayrollStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SpringDataPayrollRepository extends JpaRepository<PayrollEntity, Long> {

    @EntityGraph(attributePaths = {"concepts", "contextSnapshots", "warnings", "segments"})
    Optional<PayrollEntity> findByRuleSystemCodeAndEmployeeTypeCodeAndEmployeeNumberAndPayrollPeriodCodeAndPayrollTypeCodeAndPresenceNumber(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String payrollPeriodCode,
            String payrollTypeCode,
            Integer presenceNumber
    );

    @Query("SELECT p FROM PayrollEntity p WHERE " +
           "(:ruleSystemCode IS NULL OR p.ruleSystemCode = :ruleSystemCode) AND " +
           "(:payrollPeriodCode IS NULL OR p.payrollPeriodCode = :payrollPeriodCode) AND " +
           "(:employeeNumber IS NULL OR p.employeeNumber = :employeeNumber) AND " +
           "(:status IS NULL OR p.status = :status) " +
           "ORDER BY p.calculatedAt DESC")
    List<PayrollEntity> findByFilters(
            @Param("ruleSystemCode") String ruleSystemCode,
            @Param("payrollPeriodCode") String payrollPeriodCode,
            @Param("employeeNumber") String employeeNumber,
            @Param("status") PayrollStatus status,
            Pageable pageable
    );

    /**
     * En que estado esta cada recibo que este empleado tiene de un periodo ({@code backend#128}).
     *
     * <p>Sin filtrar por estado <b>a proposito</b>: la pregunta es si hay alguno y como esta, y un
     * recibo {@code CALCULATED} del mes anterior no es «nada», es «todavia puede cambiar». Quien lee
     * la base es el metodo de abajo, y ese si filtra.
     */
    @Query("select p.status from PayrollEntity p"
            + " where p.ruleSystemCode = :ruleSystemCode"
            + "   and p.employeeTypeCode = :employeeTypeCode"
            + "   and p.employeeNumber = :employeeNumber"
            + "   and p.payrollPeriodCode = :payrollPeriodCode"
            + "   and p.payrollTypeCode = :payrollTypeCode")
    List<PayrollStatus> findStatusesByEmployeeAndPeriod(
            @Param("ruleSystemCode") String ruleSystemCode,
            @Param("employeeTypeCode") String employeeTypeCode,
            @Param("employeeNumber") String employeeNumber,
            @Param("payrollPeriodCode") String payrollPeriodCode,
            @Param("payrollTypeCode") String payrollTypeCode
    );

    /**
     * La suma de una linea del recibo entre los recibos <b>cerrados</b> de un empleado y un periodo
     * ({@code backend#128}).
     *
     * <p>El filtro por {@code PayrollStatus.DEFINITIVE} va <b>aqui, en la consulta</b>, y esta escrito
     * y no parametrizado: un parametro se puede pasar mal desde otro sitio, y esta lectura no admite
     * otro estado (ADR-069 §2, ADR-074).
     *
     * <p>Se suma porque un empleado puede tener dos recibos del mismo mes —cese y readmision— y la
     * base de cotizacion del mes es la del mes. Devuelve {@code null} si no hay ninguna linea, que es
     * lo que distingue «cerrado y sin esa linea» de «cerrado con cero».
     */
    @Query("select sum(c.amount) from PayrollEntity p join p.concepts c"
            + " where p.ruleSystemCode = :ruleSystemCode"
            + "   and p.employeeTypeCode = :employeeTypeCode"
            + "   and p.employeeNumber = :employeeNumber"
            + "   and p.payrollPeriodCode = :payrollPeriodCode"
            + "   and p.payrollTypeCode = :payrollTypeCode"
            + "   and p.status = com.b4rrhh.payroll.domain.model.PayrollStatus.DEFINITIVE"
            + "   and c.conceptCode = :conceptCode")
    BigDecimal sumDefinitiveConceptAmount(
            @Param("ruleSystemCode") String ruleSystemCode,
            @Param("employeeTypeCode") String employeeTypeCode,
            @Param("employeeNumber") String employeeNumber,
            @Param("payrollPeriodCode") String payrollPeriodCode,
            @Param("payrollTypeCode") String payrollTypeCode,
            @Param("conceptCode") String conceptCode
    );
}

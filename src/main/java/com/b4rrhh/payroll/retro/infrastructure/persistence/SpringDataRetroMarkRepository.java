package com.b4rrhh.payroll.retro.infrastructure.persistence;

import com.b4rrhh.payroll.retro.domain.model.RetroMarkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataRetroMarkRepository extends JpaRepository<RetroMarkEntity, Long> {

    /** Todas las del empleado, de la mas reciente a la mas antigua: la ficha las ensena asi. */
    @Query("select m from RetroMarkEntity m"
            + " where m.ruleSystemCode = :ruleSystemCode"
            + "   and m.employeeTypeCode = :employeeTypeCode"
            + "   and m.employeeNumber = :employeeNumber"
            + " order by m.createdAt desc, m.id desc")
    List<RetroMarkEntity> findByEmployee(
            @Param("ruleSystemCode") String ruleSystemCode,
            @Param("employeeTypeCode") String employeeTypeCode,
            @Param("employeeNumber") String employeeNumber
    );

    /**
     * Las activas, de la mas antigua a la mas reciente por periodo.
     *
     * <p>Ordenadas por {@code fromPeriodCode} a proposito: quien pregunta es el lanzamiento, y lo que
     * quiere es el minimo. Con el orden puesto aqui, el minimo es la primera y no hace falta recorrer.
     */
    @Query("select m from RetroMarkEntity m"
            + " where m.ruleSystemCode = :ruleSystemCode"
            + "   and m.employeeTypeCode = :employeeTypeCode"
            + "   and m.employeeNumber = :employeeNumber"
            + "   and m.status = :status"
            + " order by m.fromPeriodCode asc, m.id asc")
    List<RetroMarkEntity> findByEmployeeAndStatus(
            @Param("ruleSystemCode") String ruleSystemCode,
            @Param("employeeTypeCode") String employeeTypeCode,
            @Param("employeeNumber") String employeeNumber,
            @Param("status") RetroMarkStatus status
    );
}

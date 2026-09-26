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

    /**
     * Que presencias de este empleado tienen ya un recibo <b>entregado</b> de un periodo
     * ({@code backend#130}).
     *
     * <p>No lee ningun importe ni ninguna linea: solo el numero de presencia. Es la pregunta que
     * decide si una escritura con fecha deja marca de retroactividad, y por eso <b>no es una lectura
     * de calculo</b> —no alimenta a nadie— aunque toque la misma tabla que las dos de arriba.
     *
     * <p>El filtro por {@code DEFINITIVE} va escrito en la consulta y no parametrizado, como el de
     * {@link #sumDefinitiveConceptAmount} y por la misma razon (ADR-069 §2): un mes calculado y sin
     * cerrar <b>no es pasado</b>, se vuelve a calcular y no se le ha contado a nadie.
     */
    @Query("select distinct p.presenceNumber from PayrollEntity p"
            + " where p.ruleSystemCode = :ruleSystemCode"
            + "   and p.employeeTypeCode = :employeeTypeCode"
            + "   and p.employeeNumber = :employeeNumber"
            + "   and p.payrollPeriodCode = :payrollPeriodCode"
            + "   and p.payrollTypeCode = :payrollTypeCode"
            + "   and p.status = com.b4rrhh.payroll.domain.model.PayrollStatus.DEFINITIVE")
    List<Integer> findPresenceNumbersWithDefinitivePayroll(
            @Param("ruleSystemCode") String ruleSystemCode,
            @Param("employeeTypeCode") String employeeTypeCode,
            @Param("employeeNumber") String employeeNumber,
            @Param("payrollPeriodCode") String payrollPeriodCode,
            @Param("payrollTypeCode") String payrollTypeCode
    );

    /**
     * Lo que ya se ha pagado por un mes, por concepto ({@code backend#133}).
     *
     * <p><b>Una sola condicion</b>, y eso es porque en este arbol <b>toda</b> linea lleva su periodo en
     * {@code origin_period_code}: las propias el del recibo, y las de atraso el del mes al que
     * pertenecen. Comprobado en la semilla, donde las 22.372 lineas llevan {@code 202609}. Asi que
     * «lo pagado atribuido a agosto» es sumar las lineas con origen agosto, esten en el recibo de agosto
     * o en el de cualquier mes posterior, y es esa suma la que hace que el segundo atraso del mismo mes
     * pague la diferencia y no el total otra vez.
     *
     * <p>Si algun dia las lineas propias dejaran de llevar su periodo, esta consulta habria que
     * reescribirla: diria que por agosto no se ha pagado nada y el atraso saldria por el total.
     *
     * <p>Solo de recibos {@code DEFINITIVE}, con el filtro escrito y no parametrizado (ADR-069 §2): un
     * recibo que todavia puede cambiar no se le ha pagado a nadie, asi que contarlo como pagado dejaria
     * al empleado sin ese dinero el dia que ese recibo se recalcule.
     *
     * <p>Por empleado y no por presencia, como la base de cotizacion de un mes (ADR-074 §3).
     */
    @Query("select c.conceptCode, sum(c.amount) from PayrollEntity p join p.concepts c"
            + " where p.ruleSystemCode = :ruleSystemCode"
            + "   and p.employeeTypeCode = :employeeTypeCode"
            + "   and p.employeeNumber = :employeeNumber"
            + "   and p.payrollTypeCode = :payrollTypeCode"
            + "   and p.status = com.b4rrhh.payroll.domain.model.PayrollStatus.DEFINITIVE"
            + "   and c.originPeriodCode = :periodCode"
            + " group by c.conceptCode")
    List<Object[]> sumPaidByConceptForPeriod(
            @Param("ruleSystemCode") String ruleSystemCode,
            @Param("employeeTypeCode") String employeeTypeCode,
            @Param("employeeNumber") String employeeNumber,
            @Param("payrollTypeCode") String payrollTypeCode,
            @Param("periodCode") String periodCode
    );
}

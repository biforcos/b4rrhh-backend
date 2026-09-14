package com.b4rrhh.payroll.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataPayrollCalculationStepRepository
        extends JpaRepository<PayrollCalculationStepEntity, PayrollCalculationStepEntityId> {

    /**
     * Los pasos de un recibo en orden de ejecución.
     *
     * <p>El {@code OrderByExecutionOrderAsc} va en la consulta y no en el que la llama: el orden es
     * lo único que estos pasos aportan sobre el recibo, y ordenar en memoria sería dejar que el
     * primero que olvide hacerlo sirva una explicación desordenada sin que nada se entere
     * ({@code backend#97}).
     */
    List<PayrollCalculationStepEntity> findByPayrollIdOrderByExecutionOrderAsc(Long payrollId);
}

package com.b4rrhh.payroll.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;

/** La identidad de un paso: el recibo y el orden en que se ejecutó dentro de él ({@code backend#93}). */
public class PayrollCalculationStepEntityId implements Serializable {

    private Long payrollId;
    private Integer executionOrder;

    public PayrollCalculationStepEntityId() {
    }

    public PayrollCalculationStepEntityId(Long payrollId, Integer executionOrder) {
        this.payrollId = payrollId;
        this.executionOrder = executionOrder;
    }

    public Long getPayrollId() { return payrollId; }
    public void setPayrollId(Long payrollId) { this.payrollId = payrollId; }
    public Integer getExecutionOrder() { return executionOrder; }
    public void setExecutionOrder(Integer executionOrder) { this.executionOrder = executionOrder; }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PayrollCalculationStepEntityId that)) {
            return false;
        }
        return Objects.equals(payrollId, that.payrollId)
                && Objects.equals(executionOrder, that.executionOrder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(payrollId, executionOrder);
    }
}

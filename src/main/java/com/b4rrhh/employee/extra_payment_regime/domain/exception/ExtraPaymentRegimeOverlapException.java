package com.b4rrhh.employee.extra_payment_regime.domain.exception;

import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegimePeriod;

import java.time.LocalDate;
import java.util.List;

public final class ExtraPaymentRegimeOverlapException extends ExtraPaymentRegimeSeriesInvariantException {

    private final List<ExtraPaymentRegimePeriod> overlaps;

    public ExtraPaymentRegimeOverlapException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            LocalDate startDate,
            LocalDate endDate
    ) {
        this(ruleSystemCode, employeeTypeCode, employeeNumber, startDate, endDate, List.of());
    }

    public ExtraPaymentRegimeOverlapException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            LocalDate startDate,
            LocalDate endDate,
            List<ExtraPaymentRegimePeriod> overlaps
    ) {
        super("Extra payment regime period overlaps for ruleSystemCode="
                + ruleSystemCode
                + ", employeeTypeCode="
                + employeeTypeCode
                + ", employeeNumber="
                + employeeNumber
                + ", periodStart="
                + startDate
                + ", periodEnd="
                + endDate
                + ", overlaps="
                + overlaps);
        this.overlaps = List.copyOf(overlaps);
    }

    /** The stretches of dates the rejected occurrence would share with existing ones. */
    public List<ExtraPaymentRegimePeriod> overlaps() {
        return overlaps;
    }
}
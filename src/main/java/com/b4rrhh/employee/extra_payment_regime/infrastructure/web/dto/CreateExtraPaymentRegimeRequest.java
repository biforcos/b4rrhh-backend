package com.b4rrhh.employee.extra_payment_regime.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.time.LocalDate;

public class CreateExtraPaymentRegimeRequest {

    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean prorated;

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Boolean getProrated() {
        return prorated;
    }

    public void setProrated(Boolean prorated) {
        this.prorated = prorated;
    }

    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object value) {
        throw new IllegalArgumentException("Unexpected field: " + fieldName);
    }
}

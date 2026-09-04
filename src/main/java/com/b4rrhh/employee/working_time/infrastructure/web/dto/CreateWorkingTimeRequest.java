package com.b4rrhh.employee.working_time.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateWorkingTimeRequest {

    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal workingTimePercentage;

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

    public BigDecimal getWorkingTimePercentage() {
        return workingTimePercentage;
    }

    public void setWorkingTimePercentage(BigDecimal workingTimePercentage) {
        this.workingTimePercentage = workingTimePercentage;
    }

    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object value) {
        throw new IllegalArgumentException("Unexpected field: " + fieldName);
    }
}
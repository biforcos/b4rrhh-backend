package com.b4rrhh.payroll.retro.infrastructure.web.dto;

import java.math.BigDecimal;

public record ArrearPaidInResponse(String payrollPeriodCode, BigDecimal amount) {
}

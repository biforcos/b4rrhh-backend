package com.b4rrhh.payroll.domain.model;

import com.b4rrhh.payroll.domain.exception.InvalidPayrollArgumentException;
import com.b4rrhh.payroll.domain.exception.PayrollInvalidStateTransitionException;
import com.b4rrhh.payroll.domain.exception.PayrollTypeInvalidException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayrollTest {

    @Test
    void invalidateMovesCalculatedPayrollToNotValid() {
        Payroll payroll = persistedPayroll(PayrollStatus.CALCULATED);

        Payroll invalidated = payroll.invalidate("USER_INVALIDATED");

        assertEquals(7L, invalidated.getId());
        assertEquals(PayrollStatus.NOT_VALID, invalidated.getStatus());
        assertEquals("USER_INVALIDATED", invalidated.getStatusReasonCode());
        assertTrue(invalidated.canBeRecalculated());
    }

    @Test
    void validateMovesCalculatedPayrollToExplicitValidated() {
        Payroll payroll = persistedPayroll(PayrollStatus.CALCULATED);

        Payroll validated = payroll.validateExplicitly();

        assertEquals(7L, validated.getId());
        assertEquals(PayrollStatus.EXPLICIT_VALIDATED, validated.getStatus());
        assertFalse(validated.canBeRecalculated());
    }

    @Test
    void finalizeMovesExplicitValidatedPayrollToDefinitive() {
        Payroll payroll = persistedPayroll(PayrollStatus.EXPLICIT_VALIDATED);

        Payroll definitive = payroll.finalizePayroll();

        assertEquals(7L, definitive.getId());
        assertEquals(PayrollStatus.DEFINITIVE, definitive.getStatus());
        assertFalse(definitive.canBeRecalculated());
    }

    @Test
    void rejectsValidateFromNotValid() {
        Payroll payroll = payroll(PayrollStatus.NOT_VALID);

        assertThrows(PayrollInvalidStateTransitionException.class, payroll::validateExplicitly);
    }

    @Test
    void rejectsFinalizeFromDefinitive() {
        Payroll payroll = payroll(PayrollStatus.DEFINITIVE);

        assertThrows(PayrollInvalidStateTransitionException.class, payroll::finalizePayroll);
    }

    @Test
    void rejectsConceptWithNonPositiveLineNumber() {
        assertThrows(
                InvalidPayrollArgumentException.class,
                () -> new PayrollConcept(
                        0,
                        "BASE",
                        "SALARIO_BASE",
                        "Base salary",
                        new BigDecimal("100.00"),
                        null,
                        null,
                        "EARNING",
                        null,
                        1
                )
        );
    }

    @Test
    void rejectsInvalidPayrollTypeCode() {
        assertThrows(
                PayrollTypeInvalidException.class,
                () -> Payroll.create(
                        "ESP",
                        "INTERNAL",
                        "0001",
                        "202501",
                        "ORD",   // invalid — must be NORMAL or EXTRA
                        1,
                        PayrollStatus.CALCULATED,
                        null,
                        Instant.parse("2026-01-31T10:15:00Z"),
                        "PAYROLL_ENGINE",
                        "1.0.0",
                        List.of(
                                new PayrollConcept(
                                        1,
                                        "BASE",
                                        "SALARIO_BASE",
                                        "Base salary",
                                        new BigDecimal("1000.00"),
                                        new BigDecimal("1.00"),
                                        new BigDecimal("1000.00"),
                                        "EARNING",
                                        "202501",
                                        1
                                )
                        ),
                        List.of(
                                new PayrollContextSnapshot(
                                        "PRESENCE",
                                        "EMPLOYEE",
                                        "{\"presenceNumber\":1}",
                                        "{\"companyCode\":\"ES01\"}"
                                )
                        )
                )
        );
    }

    private Payroll payroll(PayrollStatus status) {
        return Payroll.create(
                "ESP",
                "INTERNAL",
                "0001",
                "202501",
                "NORMAL",
                1,
                status,
                status == PayrollStatus.NOT_VALID ? "ENGINE_INVALID" : null,
                Instant.parse("2026-01-31T10:15:00Z"),
                "PAYROLL_ENGINE",
                "1.0.0",
                List.of(
                        new PayrollConcept(
                                1,
                                "BASE",
                                "SALARIO_BASE",
                                "Base salary",
                                new BigDecimal("1000.00"),
                                new BigDecimal("1.00"),
                                new BigDecimal("1000.00"),
                                "EARNING",
                                "202501",
                                1
                        )
                ),
                List.of(
                        new PayrollContextSnapshot(
                                "PRESENCE",
                                "EMPLOYEE",
                                "{\"presenceNumber\":1}",
                                "{\"companyCode\":\"ES01\"}"
                        )
                )
        );
    }

    private Payroll persistedPayroll(PayrollStatus status) {
        Payroll transientPayroll = payroll(status);
        return Payroll.rehydrate(
                7L,
                transientPayroll.getRuleSystemCode(),
                transientPayroll.getEmployeeTypeCode(),
                transientPayroll.getEmployeeNumber(),
                transientPayroll.getPayrollPeriodCode(),
                transientPayroll.getPayrollTypeCode(),
                transientPayroll.getPresenceNumber(),
                transientPayroll.getStatus(),
                transientPayroll.getStatusReasonCode(),
                transientPayroll.getCalculatedAt(),
                transientPayroll.getCalculationEngineCode(),
                transientPayroll.getCalculationEngineVersion(),
                transientPayroll.getConcepts(),
                transientPayroll.getContextSnapshots(),
                LocalDateTime.of(2026, 1, 31, 10, 15),
                LocalDateTime.of(2026, 1, 31, 10, 15)
        );
    }
}
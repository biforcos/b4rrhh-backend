package com.b4rrhh.payroll_engine.segment.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SegmentCalculationContextTest {

    private static final LocalDate APR_01 = LocalDate.of(2026, 4, 1);
    private static final LocalDate APR_14 = LocalDate.of(2026, 4, 14);
    private static final LocalDate APR_30 = LocalDate.of(2026, 4, 30);

    private SegmentCalculationContext valid() {
        return new SegmentCalculationContext(
                "ESP", "EMP", "EMP0001",
                APR_01, APR_30,
                APR_01, APR_14,
                true, false,
                30L, 14L,
                new BigDecimal("100"),
                new BigDecimal("2000.00"),
                Map.of(),
                "G02",
                "MENSUAL",
                Map.of(), false
        );
    }

    @Test
    void validContextDoesNotThrow() {
        assertDoesNotThrow(this::valid);
    }

    @Test
    void nullRuleSystemCodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
                null, "EMP", "EMP0001",
                APR_01, APR_30, APR_01, APR_14,
                true, false, 30L, 14L,
                new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
                "G02", "MENSUAL", Map.of(), false));
    }

    @Test
    void blankRuleSystemCodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
                "  ", "EMP", "EMP0001",
                APR_01, APR_30, APR_01, APR_14,
                true, false, 30L, 14L,
                new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
                "G02", "MENSUAL", Map.of(), false));
    }

    @Test
    void nullEmployeeTypeCodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
                "ESP", null, "EMP0001",
                APR_01, APR_30, APR_01, APR_14,
                true, false, 30L, 14L,
                new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
                "G02", "MENSUAL", Map.of(), false));
    }

    @Test
    void nullEmployeeNumberIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
                "ESP", "EMP", null,
                APR_01, APR_30, APR_01, APR_14,
                true, false, 30L, 14L,
                new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
                "G02", "MENSUAL", Map.of(), false));
    }

    @Test
    void nullPeriodStartIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
                "ESP", "EMP", "EMP0001",
                null, APR_30, APR_01, APR_14,
                true, false, 30L, 14L,
                new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
                "G02", "MENSUAL", Map.of(), false));
    }

    @Test
    void nullPeriodEndIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
                "ESP", "EMP", "EMP0001",
                APR_01, null, APR_01, APR_14,
                true, false, 30L, 14L,
                new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
                "G02", "MENSUAL", Map.of(), false));
    }

    @Test
    void nullSegmentStartIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
                "ESP", "EMP", "EMP0001",
                APR_01, APR_30, null, APR_14,
                true, false, 30L, 14L,
                new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
                "G02", "MENSUAL", Map.of(), false));
    }

    @Test
    void nullSegmentEndIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
                "ESP", "EMP", "EMP0001",
                APR_01, APR_30, APR_01, null,
                true, false, 30L, 14L,
                new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
                "G02", "MENSUAL", Map.of(), false));
    }

    @Test
    void periodEndBeforePeriodStartIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
                "ESP", "EMP", "EMP0001",
                APR_30, APR_01, APR_01, APR_01,
                true, true, 1L, 1L,
                new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
                "G02", "MENSUAL", Map.of(), false));
    }

    @Test
    void segmentEndBeforeSegmentStartIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
                "ESP", "EMP", "EMP0001",
                APR_01, APR_30, APR_14, APR_01,
                true, false, 30L, 1L,
                new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
                "G02", "MENSUAL", Map.of(), false));
    }

    @Test
    void segmentStartBeforePeriodStartIsRejected() {
        LocalDate mar31 = LocalDate.of(2026, 3, 31);
        assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
                "ESP", "EMP", "EMP0001",
                APR_01, APR_30, mar31, APR_14,
                true, false, 30L, 15L,
                new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
                "G02", "MENSUAL", Map.of(), false));
    }

    @Test
    void segmentEndAfterPeriodEndIsRejected() {
        LocalDate may01 = LocalDate.of(2026, 5, 1);
        assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
                "ESP", "EMP", "EMP0001",
                APR_01, APR_30, APR_01, may01,
                true, true, 30L, 31L,
                new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
                "G02", "MENSUAL", Map.of(), false));
    }

    @Test
    void zeroDaysInPeriodIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
                "ESP", "EMP", "EMP0001",
                APR_01, APR_30, APR_01, APR_14,
                true, false, 0L, 14L,
                new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
                "G02", "MENSUAL", Map.of(), false));
    }

    @Test
    void negativeDaysInPeriodIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
                "ESP", "EMP", "EMP0001",
                APR_01, APR_30, APR_01, APR_14,
                true, false, -1L, 14L,
                new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
                "G02", "MENSUAL", Map.of(), false));
    }

    @Test
    void zeroDaysInSegmentIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
                "ESP", "EMP", "EMP0001",
                APR_01, APR_30, APR_01, APR_14,
                true, false, 30L, 0L,
                new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
                "G02", "MENSUAL", Map.of(), false));
    }

    @Test
    void nullWorkingTimePercentageIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
                "ESP", "EMP", "EMP0001",
                APR_01, APR_30, APR_01, APR_14,
                true, false, 30L, 14L,
                null, new BigDecimal("2000.00"), Map.of(),
                "G02", "MENSUAL", Map.of(), false));
    }

    @Test
    void nullMonthlySalaryAmountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
                "ESP", "EMP", "EMP0001",
                APR_01, APR_30, APR_01, APR_14,
                true, false, 30L, 14L,
                new BigDecimal("100"), null, Map.of(),
                "G02", "MENSUAL", Map.of(), false));
    }

    @Test
    void nullEmployeeInputsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
                "ESP", "EMP", "EMP0001",
                APR_01, APR_30, APR_01, APR_14,
                true, false, 30L, 14L,
                new BigDecimal("100"), new BigDecimal("2000.00"), null,
                "G02", "MENSUAL", Map.of(), false));
    }

    @Test
    void nullGrupoCotizacionCodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
                "ESP", "EMP", "EMP0001",
                APR_01, APR_30, APR_01, APR_14,
                true, false, 30L, 14L,
                new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
                null, "MENSUAL", Map.of(), false));
    }

    @Test
    void blankGrupoCotizacionCodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
                "ESP", "EMP", "EMP0001",
                APR_01, APR_30, APR_01, APR_14,
                true, false, 30L, 14L,
                new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
                "  ", "MENSUAL", Map.of(), false));
    }

    @Test
    void nullTipoNominaIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
                "ESP", "EMP", "EMP0001",
                APR_01, APR_30, APR_01, APR_14,
                true, false, 30L, 14L,
                new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
                "G02", null, Map.of(), false));
    }

    @Test
    void blankTipoNominaIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
                "ESP", "EMP", "EMP0001",
                APR_01, APR_30, APR_01, APR_14,
                true, false, 30L, 14L,
                new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
                "G02", "  ", Map.of(), false));
    }

    @Test
    void nullPrecomputedDirectAmountsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
                "ESP", "EMP", "EMP0001",
                APR_01, APR_30, APR_01, APR_14,
                true, false, 30L, 14L,
                new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
                "G02", "MENSUAL", null, false));
    }
}

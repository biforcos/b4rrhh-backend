package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.domain.exception.InvalidPayrollArgumentException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Las tres normalizaciones que todos los casos de uso de /payrolls repiten: un codigo, un texto y
 * el mes a fechas.
 *
 * <p>Se saca aqui al abrir el tercer verbo masivo ({@code backend#102}), no antes, porque hasta
 * ahora eran dos copias y ahora serian tres — y las tres tienen que dar el mismo error al mismo
 * campo para que el 400 diga lo mismo por las tres puertas.
 */
final class PayrollFieldNormalizer {

    private static final DateTimeFormatter PAYROLL_PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private PayrollFieldNormalizer() {
    }

    static String code(String value, String fieldName, int maxLength) {
        return text(value, fieldName, maxLength).toUpperCase();
    }

    static String text(String value, String fieldName, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidPayrollArgumentException(fieldName + " is required");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new InvalidPayrollArgumentException(fieldName + " exceeds max length " + maxLength);
        }
        return normalized;
    }

    /** El primer y el ultimo dia del mes del periodo. */
    static LocalDate[] periodBounds(String payrollPeriodCode) {
        try {
            YearMonth yearMonth = YearMonth.parse(payrollPeriodCode, PAYROLL_PERIOD_FORMATTER);
            return new LocalDate[]{yearMonth.atDay(1), yearMonth.atEndOfMonth()};
        } catch (DateTimeParseException ex) {
            throw new InvalidPayrollArgumentException(
                    "payrollPeriodCode must be in yyyyMM format, got: " + payrollPeriodCode);
        }
    }
}

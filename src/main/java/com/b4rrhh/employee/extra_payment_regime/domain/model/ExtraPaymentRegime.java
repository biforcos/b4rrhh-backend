package com.b4rrhh.employee.extra_payment_regime.domain.model;

import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeAlreadyClosedException;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.InvalidExtraPaymentRegimeDateRangeException;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Si al empleado se le prorratean las pagas extras, y desde cuando (backend#118).
 *
 * <h2>Por que es una vertical y no una columna</h2>
 *
 * <p>Porque el regimen cambia, y cuando cambia hay que poder explicar el recibo de marzo. Una
 * columna en {@code contract} o en {@code employee} no tiene vigencia: cambiarla reescribiria la
 * historia, y el recibo de marzo pasaria a decir algo que no se pago.
 *
 * <p>Con vigencia, ademas, el cambio a mitad de mes parte el periodo como lo parte la jornada
 * (ADR-068), y la prorrata de cada tramo entra por la puerta que le toca ({@code backend#119}).
 *
 * <h2>Un solo dato</h2>
 *
 * <p>{@code prorated}: si se prorratean o no. No es un catalogo —son dos valores y no hay un
 * tercero— y no admite nulos: un empleado esta en un regimen o en el otro, y «no se sabe» no es
 * un regimen del que se pueda calcular una nomina.
 */
public class ExtraPaymentRegime {

    private final Long id;
    private final Long employeeId;
    private final Integer extraPaymentRegimeNumber;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final boolean prorated;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private ExtraPaymentRegime(
            Long id,
            Long employeeId,
            Integer extraPaymentRegimeNumber,
            LocalDate startDate,
            LocalDate endDate,
            boolean prorated,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.employeeId = employeeId;
        this.extraPaymentRegimeNumber = extraPaymentRegimeNumber;
        this.startDate = startDate;
        this.endDate = endDate;
        this.prorated = prorated;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ExtraPaymentRegime create(
            Long employeeId,
            Integer extraPaymentRegimeNumber,
            LocalDate startDate,
            LocalDate endDate,
            boolean prorated
    ) {
        return rehydrate(
                null,
                employeeId,
                extraPaymentRegimeNumber,
                startDate,
                endDate,
                prorated,
                null,
                null
        );
    }

    public static ExtraPaymentRegime rehydrate(
            Long id,
            Long employeeId,
            Integer extraPaymentRegimeNumber,
            LocalDate startDate,
            LocalDate endDate,
            boolean prorated,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        validateDateRange(startDate, endDate);

        return new ExtraPaymentRegime(
                id,
                employeeId,
                normalizeExtraPaymentRegimeNumber(extraPaymentRegimeNumber),
                startDate,
                endDate,
                prorated,
                createdAt,
                updatedAt
        );
    }

    public ExtraPaymentRegime close(LocalDate closeDate) {
        if (!isActive()) {
            throw new ExtraPaymentRegimeAlreadyClosedException(extraPaymentRegimeNumber);
        }
        if (closeDate == null || closeDate.isBefore(startDate)) {
            throw new InvalidExtraPaymentRegimeDateRangeException(
                    "endDate must be greater than or equal to startDate");
        }

        return new ExtraPaymentRegime(
                id,
                employeeId,
                extraPaymentRegimeNumber,
                startDate,
                closeDate,
                prorated,
                createdAt,
                updatedAt
        );
    }

    public boolean isActive() {
        return endDate == null;
    }

    public ExtraPaymentRegime adjustEndDate(LocalDate newEndDate) {
        return ExtraPaymentRegime.rehydrate(
                id,
                employeeId,
                extraPaymentRegimeNumber,
                startDate,
                newEndDate,
                prorated,
                createdAt,
                null
        );
    }

    private static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new InvalidExtraPaymentRegimeDateRangeException("startDate is required");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidExtraPaymentRegimeDateRangeException(
                    "endDate must be greater than or equal to startDate");
        }
    }

    private static Integer normalizeExtraPaymentRegimeNumber(Integer value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("extraPaymentRegimeNumber must be a positive integer");
        }

        return value;
    }

    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public Integer getExtraPaymentRegimeNumber() {
        return extraPaymentRegimeNumber;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    /** Si en este periodo las pagas extras se prorratean. */
    public boolean isProrated() {
        return prorated;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

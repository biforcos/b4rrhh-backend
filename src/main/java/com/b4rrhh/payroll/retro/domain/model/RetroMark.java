package com.b4rrhh.payroll.retro.domain.model;

import java.time.Instant;

/**
 * Una marca de retroactividad: <b>el pasado de este empleado ha cambiado desde tal mes</b>
 * ({@code backend#130}, paso 6 de {@code b4rrhh/workspace#9}).
 *
 * <p>Es un registro y no un indicador. Varias por empleado y mes son lo normal —una ausencia
 * olvidada al mes anterior y unas horas a dos meses son dos hechos distintos— y <b>ninguna se edita
 * en sitio ni se borra</b>: descartar cambia el estado y deja escrito quien y por que; irse a otro
 * mes es una fila nueva.
 *
 * <p>Lo que decide esa forma es el recibo: si descartar borrara la fila, el recibo no podria contar
 * que <i>habia</i> una correccion conocida que alguien decidio no pagar, y eso es justo lo que un
 * empleado pregunta.
 */
public class RetroMark {

    private final Long id;
    private final String ruleSystemCode;
    private final String employeeTypeCode;
    private final String employeeNumber;
    private final Integer presenceNumber;

    /**
     * El periodo al que la escritura fue, y desde el que se recalcula <b>hacia delante</b>. Leido del
     * otro lado es «hasta que mes alcanza esta marca», que es como lo dice la ficha del empleado.
     */
    private final String fromPeriodCode;

    private final RetroMarkStatus status;
    private final Instant createdAt;
    private final RetroMarkSource source;

    private final Instant discardedAt;
    private final String discardedBy;
    private final String discardReason;

    private final Instant consumedAt;
    private final String consumedPeriodCode;
    private final Long consumedRunId;

    private RetroMark(
            Long id,
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            Integer presenceNumber,
            String fromPeriodCode,
            RetroMarkStatus status,
            Instant createdAt,
            RetroMarkSource source,
            Instant discardedAt,
            String discardedBy,
            String discardReason,
            Instant consumedAt,
            String consumedPeriodCode,
            Long consumedRunId
    ) {
        this.id = id;
        this.ruleSystemCode = ruleSystemCode;
        this.employeeTypeCode = employeeTypeCode;
        this.employeeNumber = employeeNumber;
        this.presenceNumber = presenceNumber;
        this.fromPeriodCode = fromPeriodCode;
        this.status = status;
        this.createdAt = createdAt;
        this.source = source;
        this.discardedAt = discardedAt;
        this.discardedBy = discardedBy;
        this.discardReason = discardReason;
        this.consumedAt = consumedAt;
        this.consumedPeriodCode = consumedPeriodCode;
        this.consumedRunId = consumedRunId;
    }

    /** Una marca nueva, siempre activa: una marca nace pendiente o no nace. */
    public static RetroMark create(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            Integer presenceNumber,
            String fromPeriodCode,
            RetroMarkSource source,
            Instant createdAt
    ) {
        if (fromPeriodCode == null || fromPeriodCode.isBlank()) {
            throw new IllegalArgumentException("fromPeriodCode es obligatorio en una marca de retroactividad");
        }
        if (presenceNumber == null) {
            throw new IllegalArgumentException("presenceNumber es obligatorio en una marca de retroactividad");
        }
        return new RetroMark(null, ruleSystemCode, employeeTypeCode, employeeNumber, presenceNumber,
                fromPeriodCode, RetroMarkStatus.ACTIVE, createdAt, source,
                null, null, null, null, null, null);
    }

    /** Reconstruccion desde la base. */
    public static RetroMark rehydrate(
            Long id,
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            Integer presenceNumber,
            String fromPeriodCode,
            RetroMarkStatus status,
            Instant createdAt,
            RetroMarkSource source,
            Instant discardedAt,
            String discardedBy,
            String discardReason,
            Instant consumedAt,
            String consumedPeriodCode,
            Long consumedRunId
    ) {
        return new RetroMark(id, ruleSystemCode, employeeTypeCode, employeeNumber, presenceNumber,
                fromPeriodCode, status, createdAt, source,
                discardedAt, discardedBy, discardReason, consumedAt, consumedPeriodCode, consumedRunId);
    }

    /**
     * Descartar: <b>otra version de la misma fila</b>, no una fila menos.
     *
     * <p>Solo desde {@code ACTIVE}. Descartar una ya pagada seria reescribir la historia de un recibo
     * entregado, y descartar una ya descartada dejaria dos motivos y ningun modo de saber cual valia.
     */
    public RetroMark discard(String by, String reason, Instant at) {
        if (status != RetroMarkStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Solo se descarta una marca activa; esta esta en " + status);
        }
        if (by == null || by.isBlank()) {
            throw new IllegalArgumentException("Un descarte sin quien no se puede contar en el recibo");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Un descarte sin motivo no se puede contar en el recibo");
        }
        return new RetroMark(id, ruleSystemCode, employeeTypeCode, employeeNumber, presenceNumber,
                fromPeriodCode, RetroMarkStatus.DISCARDED, createdAt, source,
                at, by, reason.trim(), null, null, null);
    }

    /**
     * Consumir: la pago este run de este periodo.
     *
     * <p>Ocurre al cerrar el periodo y no al calcularlo ({@code backend#133}): una nomina de prueba
     * no consume nada, y si el periodo se invalida las marcas siguen vivas.
     */
    public RetroMark consume(String periodCode, Long runId, Instant at) {
        if (status != RetroMarkStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Solo se consume una marca activa; esta esta en " + status);
        }
        return new RetroMark(id, ruleSystemCode, employeeTypeCode, employeeNumber, presenceNumber,
                fromPeriodCode, RetroMarkStatus.CONSUMED, createdAt, source,
                null, null, null, at, periodCode, runId);
    }

    public Long getId() { return id; }
    public String getRuleSystemCode() { return ruleSystemCode; }
    public String getEmployeeTypeCode() { return employeeTypeCode; }
    public String getEmployeeNumber() { return employeeNumber; }
    public Integer getPresenceNumber() { return presenceNumber; }
    public String getFromPeriodCode() { return fromPeriodCode; }
    public RetroMarkStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public RetroMarkSource getSource() { return source; }
    public Instant getDiscardedAt() { return discardedAt; }
    public String getDiscardedBy() { return discardedBy; }
    public String getDiscardReason() { return discardReason; }
    public Instant getConsumedAt() { return consumedAt; }
    public String getConsumedPeriodCode() { return consumedPeriodCode; }
    public Long getConsumedRunId() { return consumedRunId; }
}

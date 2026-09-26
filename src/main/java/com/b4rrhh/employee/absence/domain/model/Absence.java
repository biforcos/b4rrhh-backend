package com.b4rrhh.employee.absence.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Absence {

    private final Long id;
    private final Long employeeId;
    private final String absenceTypeCode;
    private final LocalDate startDate;
    private final int startTime;
    private final LocalDate endDate;
    private final Integer endTime;
    /**
     * Si la baja lleva derecho a prestacion ({@code backend#129}).
     *
     * <p>La carencia -180 dias cotizados en los cinco anos anteriores, art. 172.a) de la LGSS- es la
     * vida del empleado <b>fuera de esta empresa</b>, y la decide el INSS. La nomina no la puede
     * calcular y no lo intenta: esto es un <b>dato</b> que pone quien registra la baja con la
     * resolucion delante, no una regla.
     *
     * <p>Con derecho por omision, que es el caso normal. Sin el, la baja quita dias y no paga nada.
     */
    private final boolean benefitEntitled;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private Absence(Long id, Long employeeId, String absenceTypeCode,
                    LocalDate startDate, int startTime,
                    LocalDate endDate, Integer endTime, boolean benefitEntitled,
                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.absenceTypeCode = absenceTypeCode;
        this.startDate = startDate;
        this.startTime = startTime;
        this.endDate = endDate;
        this.endTime = endTime;
        this.benefitEntitled = benefitEntitled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Absence create(Long employeeId, String absenceTypeCode,
                                  LocalDate startDate, int startTime,
                                  LocalDate endDate, Integer endTime,
                                  boolean benefitEntitled) {
        LocalDateTime now = LocalDateTime.now();
        return new Absence(null, employeeId, absenceTypeCode,
            startDate, startTime, endDate, endTime, benefitEntitled, now, now);
    }

    public static Absence rehydrate(Long id, Long employeeId, String absenceTypeCode,
                                     LocalDate startDate, int startTime,
                                     LocalDate endDate, Integer endTime,
                                     boolean benefitEntitled,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Absence(id, employeeId, absenceTypeCode,
            startDate, startTime, endDate, endTime, benefitEntitled, createdAt, updatedAt);
    }

    public Absence update(LocalDate newEndDate, Integer newEndTime, boolean newBenefitEntitled) {
        return new Absence(id, employeeId, absenceTypeCode,
            startDate, startTime, newEndDate, newEndTime, newBenefitEntitled,
            createdAt, LocalDateTime.now());
    }

    /**
     * Cerrar una baja abierta al cesar no toca el derecho: el cese no cambia lo que el INSS resolvio.
     */
    public Absence closeAt(LocalDate terminationDate) {
        return new Absence(id, employeeId, absenceTypeCode,
            startDate, startTime, terminationDate, null, benefitEntitled,
            createdAt, LocalDateTime.now());
    }

    public boolean isOpen() {
        return endDate == null;
    }

    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public String getAbsenceTypeCode() { return absenceTypeCode; }
    public LocalDate getStartDate() { return startDate; }
    public int getStartTime() { return startTime; }
    public LocalDate getEndDate() { return endDate; }
    public Integer getEndTime() { return endTime; }
    public boolean isBenefitEntitled() { return benefitEntitled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

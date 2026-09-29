package com.b4rrhh.employee.presence.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Lo que el test de extremo a extremo no alcanza de {@link EmployeeStanding} (b4rrhh/backend#148). */
class EmployeeStandingTest {

    private static PresencePeriod period(String start, String end) {
        return new PresencePeriod(LocalDate.parse(start), end == null ? null : LocalDate.parse(end));
    }

    @Test
    void presencesWithoutAGapAreOneRunForTheDatesItSays() {
        List<PresencePeriod> presences = List.of(
                period("2026-01-01", "2026-03-31"),
                period("2026-04-01", "2026-06-30"),
                period("2026-07-01", "2026-09-30"));

        EmployeeStanding standing = EmployeeStanding.on(LocalDate.parse("2026-05-15"), presences);

        assertEquals(EmployeeStatus.ACTIVE, standing.status());
        assertEquals(LocalDate.parse("2026-01-01"), standing.statusSince());
        assertEquals(LocalDate.parse("2026-09-30"), standing.plannedTerminationDate());
    }

    @Test
    void anOpenPresenceAtTheEndOfTheRunHasNoPlannedTermination() {
        List<PresencePeriod> presences = List.of(
                period("2026-01-01", "2026-03-31"),
                period("2026-04-01", null));

        EmployeeStanding standing = EmployeeStanding.on(LocalDate.parse("2026-02-01"), presences);

        assertEquals(EmployeeStatus.ACTIVE, standing.status());
        assertNull(standing.plannedTerminationDate());
    }

    @Test
    void theOrderInWhichThePresencesArriveDoesNotMatter() {
        List<PresencePeriod> presences = List.of(
                period("2026-07-01", null),
                period("2026-01-01", "2026-03-31"));

        EmployeeStanding standing = EmployeeStanding.on(LocalDate.parse("2026-05-01"), presences);

        assertEquals(EmployeeStatus.TERMINATED, standing.status());
        assertEquals(LocalDate.parse("2026-04-01"), standing.statusSince());
        assertEquals(LocalDate.parse("2026-07-01"), standing.plannedHireDate());
    }

    @Test
    void anEmployeeWithoutPresencesIsNotHired() {
        EmployeeStanding standing = EmployeeStanding.on(LocalDate.parse("2026-05-01"), List.of());

        assertEquals(EmployeeStatus.NOT_HIRED, standing.status());
        assertNull(standing.statusSince());
        assertNull(standing.plannedHireDate());
    }
}

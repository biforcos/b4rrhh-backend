package com.b4rrhh.employee.absence.domain.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class AbsenceTest {

    @Test
    void createSetsFieldsAndNullsId() {
        Absence a = Absence.create(1L, "VACATION", LocalDate.of(2026, 5, 14), 0, null, null);
        assertNull(a.getId());
        assertEquals(1L, a.getEmployeeId());
        assertEquals("VACATION", a.getAbsenceTypeCode());
        assertEquals(LocalDate.of(2026, 5, 14), a.getStartDate());
        assertEquals(0, a.getStartTime());
        assertNull(a.getEndDate());
        assertNull(a.getEndTime());
        assertNotNull(a.getCreatedAt());
        assertNotNull(a.getUpdatedAt());
    }

    @Test
    void rehydratePreservesAllFields() {
        LocalDateTime ts = LocalDateTime.of(2026, 5, 1, 9, 0);
        Absence a = Absence.rehydrate(42L, 1L, "VACATION",
            LocalDate.of(2026, 5, 14), 540,
            LocalDate.of(2026, 5, 18), null, ts, ts);
        assertEquals(42L, a.getId());
        assertEquals(540, a.getStartTime());
        assertEquals(LocalDate.of(2026, 5, 18), a.getEndDate());
        assertEquals(ts, a.getCreatedAt());
    }

    @Test
    void updateReturnsNewAbsenceWithEndFields() {
        Absence original = Absence.create(1L, "VACATION", LocalDate.of(2026, 5, 14), 0, null, null);
        Absence updated = original.update(LocalDate.of(2026, 5, 20), null);
        assertEquals(LocalDate.of(2026, 5, 20), updated.getEndDate());
        assertNull(updated.getEndTime());
        assertNull(original.getEndDate()); // original unchanged
    }

    @Test
    void closeAtReturnsNewAbsenceWithTerminationDate() {
        Absence original = Absence.create(1L, "VACATION", LocalDate.of(2026, 5, 14), 0, null, null);
        Absence closed = original.closeAt(LocalDate.of(2026, 5, 31));
        assertEquals(LocalDate.of(2026, 5, 31), closed.getEndDate());
        assertNull(closed.getEndTime());
        assertNull(original.getEndDate()); // original unchanged
    }

    @Test
    void isOpenReturnsTrueWhenEndDateNull() {
        Absence a = Absence.create(1L, "VACATION", LocalDate.of(2026, 5, 14), 0, null, null);
        assertTrue(a.isOpen());
    }

    @Test
    void isOpenReturnsFalseWhenEndDateSet() {
        Absence a = Absence.create(1L, "VACATION", LocalDate.of(2026, 5, 14), 0,
            LocalDate.of(2026, 5, 18), null);
        assertFalse(a.isOpen());
    }
}

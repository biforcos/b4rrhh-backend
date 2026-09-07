package com.b4rrhh.employee.workcenter.domain.model;

import com.b4rrhh.employee.workcenter.domain.exception.InvalidWorkCenterDateRangeException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkCenterTest {

    /**
     * The only net this invariant had was WorkCenterAssignmentTest, on the twin
     * model that went away with the unreachable stack (backend#72). WorkCenter
     * writes the same rule and nobody was exercising it.
     */
    @Test
    void rejectsEndDateBeforeStartDate() {
        assertThrows(
                InvalidWorkCenterDateRangeException.class,
                () -> new WorkCenter(
                        1L,
                        10L,
                        1,
                        "MADRID_HQ",
                        LocalDate.of(2026, 2, 1),
                        LocalDate.of(2026, 1, 31),
                        LocalDateTime.now(),
                        LocalDateTime.now()
                )
        );
    }
}

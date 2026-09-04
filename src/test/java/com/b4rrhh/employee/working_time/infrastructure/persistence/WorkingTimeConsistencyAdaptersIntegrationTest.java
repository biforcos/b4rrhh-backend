package com.b4rrhh.employee.working_time.infrastructure.persistence;

import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestSobreEsquemaReal;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestSobreEsquemaReal
class WorkingTimeConsistencyAdaptersIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private WorkingTimePresenceConsistencyAdapter presenceConsistencyAdapter;

    @BeforeEach
    void setUp() {
        presenceConsistencyAdapter = new WorkingTimePresenceConsistencyAdapter(entityManager);
    }

    @Test
    void openEndedWorkingTimeIsInsideOpenEndedPresence() {
        Long empleado = DatosDePrueba.empleado(jdbcTemplate);
        DatosDePrueba.presencia(jdbcTemplate, empleado, 1, LocalDate.of(2020, 1, 5), null);

        boolean inside = presenceConsistencyAdapter.existsPresenceContainingPeriod(
                empleado,
                LocalDate.of(2020, 1, 5),
                null
        );

        assertTrue(inside);
    }

    @Test
    void openEndedWorkingTimeIsOutsideClosedPresence() {
        Long empleado = DatosDePrueba.empleado(jdbcTemplate);
        DatosDePrueba.presencia(jdbcTemplate, empleado, 1, LocalDate.of(2020, 1, 5), LocalDate.of(2020, 12, 31));

        boolean outside = presenceConsistencyAdapter.existsPresenceContainingPeriod(
                empleado,
                LocalDate.of(2020, 1, 5),
                null
        );

        assertFalse(outside);
    }

    @Test
    void listsEveryPresencePeriodOldestFirst() {
        Long empleado = DatosDePrueba.empleado(jdbcTemplate);
        DatosDePrueba.presencia(jdbcTemplate, empleado, 2, LocalDate.of(2023, 3, 1), null);
        DatosDePrueba.presencia(jdbcTemplate, empleado, 1, LocalDate.of(2020, 1, 5), LocalDate.of(2021, 12, 31));

        List<DateRange> periods = presenceConsistencyAdapter.findPresencePeriodsByEmployeeIdOrderByStartDate(empleado);

        assertEquals(
                List.of(
                        new DateRange(LocalDate.of(2020, 1, 5), LocalDate.of(2021, 12, 31)),
                        new DateRange(LocalDate.of(2023, 3, 1), null)
                ),
                periods
        );
    }
}
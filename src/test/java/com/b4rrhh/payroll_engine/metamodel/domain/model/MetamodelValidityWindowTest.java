package com.b4rrhh.payroll_engine.metamodel.domain.model;

import com.b4rrhh.payroll_engine.metamodel.domain.exception.ValidityWindowDoesNotCoverWholePeriodsException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetamodelValidityWindowTest {

    private static final String QUE = "la asignacion del concepto 101";

    @Test
    void anOpenWindowThatStartsOnTheFirstOfAMonthIsAccepted() {
        assertDoesNotThrow(() -> MetamodelValidityWindow.requireWholePeriods(
                LocalDate.of(2025, 1, 1), null, QUE, "validFrom", "validTo"));
    }

    @Test
    void aClosedWindowThatCoversWholeMonthsIsAccepted() {
        assertDoesNotThrow(() -> MetamodelValidityWindow.requireWholePeriods(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 9, 30), QUE, "validFrom", "validTo"));
    }

    /** Un solo mes tambien es un periodo entero. */
    @Test
    void aWindowThatCoversExactlyOneMonthIsAccepted() {
        assertDoesNotThrow(() -> MetamodelValidityWindow.requireWholePeriods(
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), QUE, "validFrom", "validTo"));
    }

    /** Febrero de un bisiesto acaba el 29, y la regla es "el ultimo dia", no "el 28". */
    @Test
    void theLastDayOfFebruaryInALeapYearIsTheTwentyNinth() {
        assertDoesNotThrow(() -> MetamodelValidityWindow.requireWholePeriods(
                LocalDate.of(2028, 1, 1), LocalDate.of(2028, 2, 29), QUE, "validFrom", "validTo"));

        ValidityWindowDoesNotCoverWholePeriodsException ex = assertThrows(
                ValidityWindowDoesNotCoverWholePeriodsException.class,
                () -> MetamodelValidityWindow.requireWholePeriods(
                        LocalDate.of(2028, 1, 1), LocalDate.of(2028, 2, 28), QUE, "validFrom", "validTo"));
        assertTrue(ex.getMessage().contains("2028-02-29"),
                "El mensaje tiene que decir el ultimo dia que se esperaba: " + ex.getMessage());
    }

    @Test
    void aWindowThatOpensMidMonthIsRejectedNamingTheFieldAndTheExpectedDay() {
        ValidityWindowDoesNotCoverWholePeriodsException ex = assertThrows(
                ValidityWindowDoesNotCoverWholePeriodsException.class,
                () -> MetamodelValidityWindow.requireWholePeriods(
                        LocalDate.of(2026, 9, 12), null, QUE, "validFrom", "validTo"));

        assertTrue(ex.getMessage().contains("validFrom"), ex.getMessage());
        assertTrue(ex.getMessage().contains("2026-09-12"), ex.getMessage());
        assertTrue(ex.getMessage().contains("2026-09-01"),
                "Tiene que decir que dia se esperaba, no solo que el que vino esta mal: " + ex.getMessage());
        assertTrue(ex.getMessage().contains(QUE), ex.getMessage());
    }

    /** El caso del issue: la unidad se va el 12 y la reglamentacion se cierra con ella. */
    @Test
    void aWindowThatClosesMidMonthIsRejectedNamingTheFieldAndTheExpectedDay() {
        ValidityWindowDoesNotCoverWholePeriodsException ex = assertThrows(
                ValidityWindowDoesNotCoverWholePeriodsException.class,
                () -> MetamodelValidityWindow.requireWholePeriods(
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 9, 12), QUE, "validFrom", "validTo"));

        assertTrue(ex.getMessage().contains("validTo"), ex.getMessage());
        assertTrue(ex.getMessage().contains("2026-09-12"), ex.getMessage());
        assertTrue(ex.getMessage().contains("2026-09-30"), ex.getMessage());
    }

    /** El mensaje usa los nombres reales de cada camino, no unos genericos. */
    @Test
    void theMessageUsesTheFieldNamesOfTheCallingPath() {
        ValidityWindowDoesNotCoverWholePeriodsException ex = assertThrows(
                ValidityWindowDoesNotCoverWholePeriodsException.class,
                () -> MetamodelValidityWindow.requireWholePeriods(
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 9, 12),
                        "la alimentacion de 970 desde 101", "effectiveFrom", "effectiveTo"));

        assertTrue(ex.getMessage().contains("effectiveTo"), ex.getMessage());
        assertTrue(ex.getMessage().contains("la alimentacion de 970 desde 101"), ex.getMessage());
    }

    @Test
    void aClosingDateBeforeTheOpeningIsRejected() {
        ValidityWindowDoesNotCoverWholePeriodsException ex = assertThrows(
                ValidityWindowDoesNotCoverWholePeriodsException.class,
                () -> MetamodelValidityWindow.requireWholePeriods(
                        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 1, 31), QUE, "validFrom", "validTo"));

        assertTrue(ex.getMessage().contains("anterior"), ex.getMessage());
    }

    @Test
    void aMissingOpeningDateIsRejected() {
        assertThrows(ValidityWindowDoesNotCoverWholePeriodsException.class,
                () -> MetamodelValidityWindow.requireWholePeriods(
                        null, null, QUE, "validFrom", "validTo"));
    }
}

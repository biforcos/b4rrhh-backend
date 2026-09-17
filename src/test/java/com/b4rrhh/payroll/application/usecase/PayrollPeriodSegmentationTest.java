package com.b4rrhh.payroll.application.usecase;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * La particion del periodo, que ahora sólo sabe de fechas ({@code backend#47}).
 *
 * <p>Que no sepa de verticales es el punto entero. Antes la partición estaba escrita dentro del
 * bucle de las ventanas de jornada, así que romper el período era un privilegio de una vertical:
 * un cambio de categoría a mitad de mes no partía nada, y <b>no había interruptor que lo
 * arreglara</b> — no es que estuviera desactivado, es que no existía el mecanismo.
 */
class PayrollPeriodSegmentationTest {

    private static final LocalDate ENERO_1 = LocalDate.of(2025, 1, 1);
    private static final LocalDate ENERO_31 = LocalDate.of(2025, 1, 31);

    private record Ventana(LocalDate startDate, LocalDate endDate)
            implements PayrollPeriodSegmentation.DatedWindow {
    }

    @Test
    void sinCortesElPeriodoEsUnSegmento() {
        List<PayrollPeriodSegmentation.Interval> tramos =
                PayrollPeriodSegmentation.split(ENERO_1, ENERO_31, List.of());

        assertEquals(1, tramos.size(), "un periodo sin cambios es UN segmento, no ninguno");
        assertEquals(ENERO_1, tramos.getFirst().start());
        assertEquals(ENERO_31, tramos.getFirst().end());
        assertEquals(31, tramos.getFirst().days());
    }

    @Test
    void unCorteLoPartePorElDiaDelCorte() {
        List<PayrollPeriodSegmentation.Interval> tramos =
                PayrollPeriodSegmentation.split(ENERO_1, ENERO_31, List.of(LocalDate.of(2025, 1, 16)));

        assertEquals(2, tramos.size());
        assertEquals(LocalDate.of(2025, 1, 15), tramos.get(0).end(), "el primero cierra la vispera");
        assertEquals(LocalDate.of(2025, 1, 16), tramos.get(1).start());
        assertEquals(15, tramos.get(0).days());
        assertEquals(16, tramos.get(1).days());
    }

    /**
     * Dos verticales que cambian el mismo día son un solo corte. Es el caso normal y no el raro: un
     * alta cambia el contrato y la clasificación a la vez.
     */
    @Test
    void dosVerticalesQueCambianElMismoDiaSonUnCorte() {
        LocalDate dia16 = LocalDate.of(2025, 1, 16);

        assertEquals(
                2,
                PayrollPeriodSegmentation.split(ENERO_1, ENERO_31, List.of(dia16, dia16, dia16)).size());
    }

    @Test
    void losCortesSeOrdenanSolos() {
        List<PayrollPeriodSegmentation.Interval> tramos = PayrollPeriodSegmentation.split(
                ENERO_1, ENERO_31,
                List.of(LocalDate.of(2025, 1, 21), LocalDate.of(2025, 1, 11)));

        assertEquals(3, tramos.size());
        assertEquals(LocalDate.of(2025, 1, 10), tramos.get(0).end());
        assertEquals(LocalDate.of(2025, 1, 20), tramos.get(1).end());
        assertEquals(ENERO_31, tramos.get(2).end());
    }

    /**
     * Un cambio que ocurre cuando el empleado ya no está no parte nada de lo que se paga. Y el
     * arranque tampoco es un corte de nadie: es donde empieza el primer segmento.
     */
    @Test
    void losCortesDeFueraDelTramoNoCuentan() {
        List<PayrollPeriodSegmentation.Interval> tramos = PayrollPeriodSegmentation.split(
                LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 20),
                List.of(LocalDate.of(2025, 1, 5), LocalDate.of(2025, 1, 25), LocalDate.of(2025, 1, 10)));

        assertEquals(1, tramos.size());
        assertEquals(LocalDate.of(2025, 1, 10), tramos.getFirst().start());
        assertEquals(LocalDate.of(2025, 1, 20), tramos.getFirst().end());
    }

    @Test
    void unTramoAportaSuPrimerDiaYElSiguienteASuUltimo() {
        List<LocalDate> cortes = PayrollPeriodSegmentation.cutsOf(List.of(
                new Ventana(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 15))));

        assertEquals(List.of(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 16)), cortes);
    }

    /**
     * La mitad que se olvida: una ventana que termina el 15 y <b>ninguna</b> detrás aporta un corte
     * el 16 que sí hace falta — a partir de ahí no hay tramo, y lo que valiera en el suyo deja de
     * valer.
     */
    @Test
    void unaVentanaQueSeCierraYNoTieneRelevoTambienParte() {
        List<PayrollPeriodSegmentation.Interval> tramos = PayrollPeriodSegmentation.split(
                ENERO_1, ENERO_31,
                PayrollPeriodSegmentation.cutsOf(List.of(
                        new Ventana(ENERO_1, LocalDate.of(2025, 1, 15)))));

        assertEquals(2, tramos.size());
        assertEquals(LocalDate.of(2025, 1, 15), tramos.get(0).end());
    }

    @Test
    void unTramoAbiertoNoAportaCorteDeCierre() {
        assertEquals(
                List.of(ENERO_1),
                PayrollPeriodSegmentation.cutsOf(List.of(new Ventana(ENERO_1, null))));
    }

    @Test
    void sinVentanasNoHayCortes() {
        assertEquals(List.of(), PayrollPeriodSegmentation.cutsOf(null));
        assertEquals(List.of(), PayrollPeriodSegmentation.cutsOf(List.of()));
    }

    /** Una presencia que termina antes de empezar no es un periodo: no hay segmentos que dar. */
    @Test
    void unTramoImposibleNoDaSegmentos() {
        assertEquals(List.of(), PayrollPeriodSegmentation.split(ENERO_31, ENERO_1, List.of()));
    }
}

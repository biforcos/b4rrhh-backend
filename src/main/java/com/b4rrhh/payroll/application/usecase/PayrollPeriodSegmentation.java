package com.b4rrhh.payroll.application.usecase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * Parte el periodo por los puntos de cambio de las verticales que afectan al calculo
 * ({@code backend#47}).
 *
 * <h3>Lo que esto sabe, y lo que no</h3>
 *
 * <p>Sabe de <b>fechas de corte</b>, y de nada mas. No conoce la jornada, ni el convenio, ni el
 * contrato: cada vertical aporta las suyas, se unen, se ordenan, y los segmentos son los intervalos
 * entre cortes consecutivos recortados contra la presencia.
 *
 * <p>Eso es lo que hace que <b>anadir una cuarta vertical no sea volver a tocar esto</b>. Antes la
 * particion estaba escrita dentro del bucle de las ventanas de jornada, asi que el reparto era un
 * privilegio de una vertical: cualquier otra causa de cambio —una categoria de convenio a mitad de
 * mes, un contrato nuevo el dia 16— no partia nada, y no habia interruptor que lo arreglara porque
 * no existia el mecanismo.
 *
 * <h3>Que es un corte</h3>
 *
 * <p>El <b>primer dia de un tramo nuevo</b>. De una ventana que empieza el 16 sale un corte el 16;
 * de una que termina el 15, un corte el 16, porque lo que cambia es el dia siguiente. Los cortes
 * fuera de la presencia no cuentan: un cambio que ocurre cuando el empleado ya no esta no parte
 * nada de lo que se paga.
 *
 * <p>El arranque de la presencia es siempre un corte, y por eso nunca se devuelve la lista vacia:
 * un periodo sin ningun cambio es <b>un</b> segmento, no ninguno.
 */
public final class PayrollPeriodSegmentation {

    private PayrollPeriodSegmentation() {
    }

    /** Un tramo del periodo: desde su primer dia hasta su ultimo, los dos incluidos. */
    public record Interval(LocalDate start, LocalDate end) {

        public Interval {
            if (start == null || end == null) {
                throw new IllegalArgumentException("Un intervalo tiene dos fechas");
            }
            if (start.isAfter(end)) {
                throw new IllegalArgumentException("Un intervalo no empieza despues de terminar: "
                        + start + ".." + end);
            }
        }

        public long days() {
            return end.toEpochDay() - start.toEpochDay() + 1;
        }
    }

    /**
     * Los segmentos entre {@code from} y {@code to}, partidos por los cortes que caigan dentro.
     *
     * @param cuts los primeros dias de tramo que aportan las verticales, en cualquier orden y con
     *             repeticiones: dos verticales que cambian el mismo dia son un solo corte
     */
    public static List<Interval> split(LocalDate from, LocalDate to, Collection<LocalDate> cuts) {
        if (from.isAfter(to)) return List.of();

        // TreeSet: ordenados y sin repetir de una vez. Que dos verticales cambien el mismo dia es
        // el caso normal —un alta cambia contrato y clasificacion a la vez— y no son dos cortes.
        TreeSet<LocalDate> puntos = new TreeSet<>();
        puntos.add(from);
        for (LocalDate cut : cuts) {
            if (cut != null && cut.isAfter(from) && !cut.isAfter(to)) puntos.add(cut);
        }

        List<Interval> segments = new ArrayList<>(puntos.size());
        LocalDate inicio = null;
        for (LocalDate punto : puntos) {
            if (inicio != null) segments.add(new Interval(inicio, punto.minusDays(1)));
            inicio = punto;
        }
        segments.add(new Interval(inicio, to));
        return segments;
    }

    /**
     * Los cortes que aporta una serie de tramos: el primer dia de cada uno, y el dia siguiente al
     * ultimo de cada uno.
     *
     * <p>Lo segundo es la mitad que se olvida. Una ventana que termina el 15 y otra que empieza el
     * 16 aportan el mismo corte y no pasa nada; una ventana que termina el 15 y <b>ninguna</b>
     * detras aporta un corte el 16 que si hace falta — a partir de ahi no hay tramo, y lo que
     * valiera en el suyo deja de valer.
     */
    public static List<LocalDate> cutsOf(Collection<? extends DatedWindow> windows) {
        if (windows == null) return List.of();
        List<LocalDate> cuts = new ArrayList<>();
        for (DatedWindow window : windows) {
            if (window.startDate() != null) cuts.add(window.startDate());
            if (window.endDate() != null) cuts.add(window.endDate().plusDays(1));
        }
        return cuts;
    }

    /** Lo unico que esta particion necesita saber de un tramo de cualquier vertical. */
    public interface DatedWindow {
        LocalDate startDate();

        LocalDate endDate();
    }
}

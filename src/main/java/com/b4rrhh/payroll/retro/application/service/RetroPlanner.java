package com.b4rrhh.payroll.retro.application.service;

import com.b4rrhh.payroll.application.usecase.PayrollRetroRequest;
import com.b4rrhh.payroll.retro.domain.model.RetroMark;
import com.b4rrhh.payroll.retro.domain.port.RetroMarkRepository;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Desde que mes recalcula cada empleado, y que se queda fuera del limite ({@code backend#132}).
 *
 * <p>Es gestion y no motor: quien calcula es el {@code #131}, y lo que se decide aqui es <b>el tramo</b>.
 * La regla, del diseno del 05/10:
 *
 * <blockquote>
 * Desde {@code min(marcas activas, suelo)} hasta {@code P-1}, <b>acotado por el limite</b>.
 * </blockquote>
 *
 * <p>Y la parte que no es aritmetica: una marca mas antigua que el limite <b>no se paga y no se
 * calla</b>. Sale como aviso en el recibo del periodo abierto, con el periodo de la marca y el limite
 * del run, y la marca <b>sigue activa</b> — asi que aparecera en la checklist del ciclo y alguien
 * tendra que decidir. Borrarla o acortarla en silencio seria dejar sin pagar un atraso sin que nadie lo
 * sepa.
 */
@Service
public class RetroPlanner {

    private static final DateTimeFormatter PERIODO = DateTimeFormatter.ofPattern("yyyyMM");

    private final RetroMarkRepository marks;

    public RetroPlanner(RetroMarkRepository marks) {
        this.marks = marks;
    }

    /**
     * El tramo de un empleado.
     *
     * @param openPeriodCode el periodo que se esta lanzando. El tramo termina en {@code P-1}: el abierto
     *        se calcula por el camino normal y escribe su recibo
     */
    public RetroPlan planFor(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String openPeriodCode,
            PayrollRetroRequest retro
    ) {
        List<RetroMark> activas = marks.findActiveByEmployee(
                ruleSystemCode, employeeTypeCode, employeeNumber);

        if (!retro.allowsRetro()) {
            // Sin limite no hay retro, y no se inventa uno. Lo que se devuelve es «no hay tramo, y
            // habia esto sin pagar», para que la corrida lo pueda decir.
            return new RetroPlan(null, null, List.of(), !activas.isEmpty());
        }

        String limite = retro.limitPeriodCode();
        YearMonth hasta = YearMonth.parse(openPeriodCode, PERIODO).minusMonths(1);

        // Las que no llegan: mas antiguas que el limite. Se recogen antes de calcular el minimo, porque
        // el minimo se calcula sobre las que SI llegan -si no, una marca de 2023 con el limite en 2026
        // arrastraria el tramo entero a 2023 y el limite no serviria de nada.
        TreeSet<String> fueraDelLimite = new TreeSet<>();
        TreeSet<String> dentroDelLimite = new TreeSet<>();
        for (RetroMark marca : activas) {
            if (marca.getFromPeriodCode().compareTo(limite) < 0) {
                fueraDelLimite.add(marca.getFromPeriodCode());
            } else {
                dentroDelLimite.add(marca.getFromPeriodCode());
            }
        }

        // El suelo entra en el minimo aunque el empleado no tenga ninguna marca: es lo que hace que
        // «todos desde enero» signifique todos.
        Optional<String> masAntiguo = dentroDelLimite.stream().findFirst();
        String desde = retro.floorPeriodCode() == null
                ? masAntiguo.orElse(null)
                : masAntiguo.map(m -> m.compareTo(retro.floorPeriodCode()) < 0 ? m : retro.floorPeriodCode())
                        .orElse(retro.floorPeriodCode());

        if (desde == null) {
            // Ni marcas dentro del limite ni suelo: este empleado no tiene pasado que recalcular.
            return new RetroPlan(null, null, List.copyOf(fueraDelLimite), false);
        }

        YearMonth primero = YearMonth.parse(desde, PERIODO);
        if (primero.isAfter(hasta)) {
            // La marca es del propio periodo abierto o posterior: no hay nada cerrado que recalcular.
            // Pasa cuando alguien corrige el mes en curso, que es lo normal y no es una retro.
            return new RetroPlan(null, null, List.copyOf(fueraDelLimite), false);
        }

        return new RetroPlan(desde, hasta.format(PERIODO), List.copyOf(fueraDelLimite), false);
    }

    /**
     * El tramo que le toca a un empleado.
     *
     * @param fromPeriodCode el mes mas antiguo a recalcular, o {@code null} si no hay tramo
     * @param toPeriodCode   {@code P-1}, o {@code null} si no hay tramo
     * @param marksOutsideLimit los periodos de las marcas que el limite deja fuera, ordenados y sin
     *        repetir. Cada uno sale como aviso en el recibo del periodo abierto
     * @param hadMarksButNoLimit si el empleado tenia marcas activas y el lanzamiento no llevaba limite:
     *        entonces no se ha pagado nada y la corrida lo dice
     */
    public record RetroPlan(
            String fromPeriodCode,
            String toPeriodCode,
            List<String> marksOutsideLimit,
            boolean hadMarksButNoLimit
    ) {

        public boolean hasRange() {
            return fromPeriodCode != null;
        }

        /** Cuantas unidades empleado x mes son, que es lo que la pantalla tiene que contar. */
        public int monthCount() {
            if (!hasRange()) {
                return 0;
            }
            YearMonth desde = YearMonth.parse(fromPeriodCode, PERIODO);
            YearMonth hasta = YearMonth.parse(toPeriodCode, PERIODO);
            return (int) (java.time.temporal.ChronoUnit.MONTHS.between(desde, hasta) + 1);
        }
    }
}

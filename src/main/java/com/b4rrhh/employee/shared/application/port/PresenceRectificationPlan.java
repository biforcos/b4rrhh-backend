package com.b4rrhh.employee.shared.application.port;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Lo que un vertical movería al rectificar una presencia, y lo que se lo impide (backend#172).
 *
 * @param moves     las filas que empezaban (o acababan) con la presencia y se mueven con ella
 * @param conflicts por qué no se puede, en palabras; vacío si se puede
 */
public record PresenceRectificationPlan(List<Move> moves, List<String> conflicts) {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Una fila que se mueve: su vertical, cuál es en palabras, y sus fechas antes y después. */
    public record Move(
            String vertical,
            String label,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate newStartDate,
            LocalDate newEndDate
    ) {
    }

    public static PresenceRectificationPlan nothing() {
        return new PresenceRectificationPlan(List.of(), List.of());
    }

    /**
     * La regla de las filas con vigencia que viven dentro de la presencia, la misma para todos los
     * verticales que la siguen (contrato, jornada, centro, clasificación, régimen de pagas, centros de
     * coste):
     *
     * <ul>
     *   <li>se mueve la que empezaba el día del alta (o acababa el del cese) que se rectifica;</li>
     *   <li>una que se mueve no puede quedar del revés: empezar después de acabar;</li>
     *   <li>una que no se mueve no puede quedar fuera de la presencia nueva: un cambio de contrato del
     *   día 5 no puede quedar antes de un alta rectificada al 10.</li>
     * </ul>
     */
    public static <T> PresenceRectificationPlan of(
            String vertical,
            List<T> rows,
            Function<T, LocalDate> start,
            Function<T, LocalDate> end,
            Function<T, String> label,
            PresenceRectification rectification
    ) {
        List<Move> moves = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();
        boolean startDate = rectification.field() == PresenceRectification.Field.START_DATE;
        for (T row : rows) {
            LocalDate rowStart = start.apply(row);
            LocalDate rowEnd = end.apply(row);
            if (!rectification.belongsToThePresence(rowStart, rowEnd)) {
                continue;
            }
            boolean moving = startDate
                    ? rowStart.equals(rectification.oldDate())
                    : rowEnd != null && rowEnd.equals(rectification.oldDate());
            LocalDate newStart = startDate && moving ? rectification.newDate() : rowStart;
            LocalDate newEnd = !startDate && moving ? rectification.newDate() : rowEnd;
            if (moving) {
                if (newEnd != null && newStart.isAfter(newEnd)) {
                    conflicts.add(startDate
                            ? label.apply(row) + " acaba el " + rowEnd.format(DAY) + ", antes de la fecha nueva"
                            : label.apply(row) + " empieza el " + rowStart.format(DAY) + ", después de la fecha nueva");
                    continue;
                }
                moves.add(new Move(vertical, label.apply(row), rowStart, rowEnd, newStart, newEnd));
            } else if (startDate && rowStart.isBefore(rectification.newStart())) {
                conflicts.add(label.apply(row) + " empieza el " + rowStart.format(DAY)
                        + ", antes de la fecha nueva");
            } else if (!startDate && (rowEnd == null || rowEnd.isAfter(rectification.newEnd()))) {
                conflicts.add(label.apply(row) + (rowEnd == null ? " sigue abierto" : " acaba el " + rowEnd.format(DAY))
                        + ", después de la fecha nueva");
            }
        }
        return new PresenceRectificationPlan(List.copyOf(moves), List.copyOf(conflicts));
    }
}

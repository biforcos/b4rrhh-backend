package com.b4rrhh.employee.presence.domain.model;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * El estado del empleado en una fecha, leído de sus presencias (b4rrhh/backend#148).
 *
 * <p>No se guarda: se grababa al registrar el cese, y un cese para el 30 grabado el 28 dejaba al
 * empleado de baja el 29. Aquí sale de las fechas cada vez que se pregunta:
 * <ul>
 *   <li>{@code ACTIVE} si una presencia cubre la fecha. El día del cese es el último de la
 *       presencia, así que ese día todavía es alta. {@code plannedTerminationDate} dice cuándo acaba
 *       el tramo, si acaba; presencias seguidas sin hueco cuentan como un solo tramo.</li>
 *   <li>{@code TERMINATED} si ninguna la cubre y alguna acabó antes: de baja desde el día siguiente
 *       al último cese. {@code plannedHireDate} dice cuándo vuelve, si hay readmisión grabada.</li>
 *   <li>{@code NOT_HIRED} si ninguna la cubre y ninguna acabó antes: el alta aún no ha llegado o no
 *       la hay.</li>
 * </ul>
 */
public record EmployeeStanding(
        EmployeeStatus status,
        LocalDate statusDate,
        LocalDate statusSince,
        LocalDate plannedTerminationDate,
        LocalDate plannedHireDate
) {

    public static EmployeeStanding on(LocalDate date, List<PresencePeriod> presences) {
        Objects.requireNonNull(date, "date is required");
        List<PresencePeriod> ordered = presences.stream()
                .sorted(Comparator.comparing(PresencePeriod::startDate))
                .toList();

        Optional<PresencePeriod> covering = ordered.stream().filter(p -> p.covers(date)).findFirst();
        if (covering.isPresent()) {
            return new EmployeeStanding(
                    EmployeeStatus.ACTIVE,
                    date,
                    startOfRun(ordered, covering.get()),
                    endOfRun(ordered, covering.get()),
                    null);
        }

        LocalDate nextHire = ordered.stream()
                .map(PresencePeriod::startDate)
                .filter(start -> start.isAfter(date))
                .findFirst()
                .orElse(null);

        Optional<LocalDate> lastEnd = ordered.stream()
                .map(PresencePeriod::endDate)
                .filter(end -> end != null && end.isBefore(date))
                .max(Comparator.naturalOrder());
        if (lastEnd.isPresent()) {
            return new EmployeeStanding(EmployeeStatus.TERMINATED, date, lastEnd.get().plusDays(1), null, nextHire);
        }

        return new EmployeeStanding(EmployeeStatus.NOT_HIRED, date, null, null, nextHire);
    }

    private static LocalDate startOfRun(List<PresencePeriod> ordered, PresencePeriod covering) {
        LocalDate start = covering.startDate();
        for (int i = ordered.indexOf(covering) - 1; i >= 0; i--) {
            PresencePeriod previous = ordered.get(i);
            if (previous.endDate() == null || !previous.endDate().plusDays(1).equals(start)) {
                break;
            }
            start = previous.startDate();
        }
        return start;
    }

    private static LocalDate endOfRun(List<PresencePeriod> ordered, PresencePeriod covering) {
        LocalDate end = covering.endDate();
        for (int i = ordered.indexOf(covering) + 1; i < ordered.size() && end != null; i++) {
            PresencePeriod next = ordered.get(i);
            if (!next.startDate().equals(end.plusDays(1))) {
                break;
            }
            end = next.endDate();
        }
        return end;
    }
}

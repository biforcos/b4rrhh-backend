package com.b4rrhh.employee.presence.application.usecase;

import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.presence.application.port.PresencePayrollLookupPort;
import com.b4rrhh.employee.presence.application.port.PresenceRectificationTrailPort;
import com.b4rrhh.employee.presence.domain.exception.PresenceRectificationRejectedException;
import com.b4rrhh.employee.presence.domain.exception.PresenceRectificationRejectedException.Reason;
import com.b4rrhh.employee.presence.domain.model.Presence;
import com.b4rrhh.employee.presence.domain.port.PresenceRepository;
import com.b4rrhh.employee.shared.application.port.DatedWrite;
import com.b4rrhh.employee.shared.application.port.DatedWriteNoticePort;
import com.b4rrhh.employee.shared.application.port.PresenceRectification;
import com.b4rrhh.employee.shared.application.port.PresenceRectificationParticipant;
import com.b4rrhh.employee.shared.application.port.PresenceRectificationPlan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Rectifica la fecha de alta o de cese de una presencia (backend#172).
 *
 * <p>La sesión de workspace#18 dio de alta a alguien el 01/10 por error y no tuvo salida: la presencia
 * se crea con el alta y sólo se puede cerrar. Esto la mueve, y con ella todo lo que empezó (o acabó) ese
 * mismo día: un alta es una cosa, no cinco vigencias que casualmente coinciden. Lo que mueve cada
 * vertical lo dice él ({@link PresenceRectificationParticipant}, el patrón del ADR-047); aquí sólo se
 * junta, se comprueba y se aplica todo o nada.</p>
 *
 * <p>Se niega, con el porqué, si la fecha ya se pagó (un recibo DEFINITIVE de la presencia: con el alta,
 * cualquiera; con el cese, uno del mes de la fecha más temprana o posterior), si pisa otra presencia, o
 * si algún vertical no puede moverse (una ausencia que quedaría fuera, un cambio de contrato que quedaría
 * antes del alta). Con recibos calculados y no cerrados se rectifica y se avisa del cambio con fecha, como
 * cualquier otro.</p>
 */
@Service
public class RectifyPresenceDateService implements RectifyPresenceDateUseCase {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter PERIOD = DateTimeFormatter.ofPattern("yyyyMM");

    private final GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKey;
    private final PresenceRepository presenceRepository;
    private final PresencePayrollLookupPort payrollLookup;
    private final PresenceRectificationTrailPort trail;
    private final DatedWriteNoticePort datedWriteNotice;
    private final List<PresenceRectificationParticipant> participants;

    public RectifyPresenceDateService(
            GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKey,
            PresenceRepository presenceRepository,
            PresencePayrollLookupPort payrollLookup,
            PresenceRectificationTrailPort trail,
            DatedWriteNoticePort datedWriteNotice,
            List<PresenceRectificationParticipant> participants
    ) {
        this.getEmployeeByBusinessKey = getEmployeeByBusinessKey;
        this.presenceRepository = presenceRepository;
        this.payrollLookup = payrollLookup;
        this.trail = trail;
        this.datedWriteNotice = datedWriteNotice;
        this.participants = List.copyOf(participants);
    }

    @Override
    @Transactional
    public RectifyPresenceDateResult rectify(RectifyPresenceDateCommand command) {
        Employee employee = getEmployeeByBusinessKey
                .getByBusinessKey(command.ruleSystemCode(), command.employeeTypeCode(), command.employeeNumber())
                .orElseThrow(() -> new PresenceRectificationRejectedException(Reason.NOT_FOUND,
                        "No existe el empleado " + command.employeeNumber() + "."));
        Presence presence = presenceRepository
                .findByEmployeeIdAndPresenceNumber(employee.getId(), command.presenceNumber())
                .orElseThrow(() -> new PresenceRectificationRejectedException(Reason.NOT_FOUND,
                        "El empleado " + command.employeeNumber() + " no tiene presencia " + command.presenceNumber() + "."));

        PresenceRectification rectification = new PresenceRectification(employee.getId(), command.ruleSystemCode(),
                command.employeeTypeCode(), command.employeeNumber(), presence.getPresenceNumber(), command.field(),
                presence.getStartDate(), presence.getEndDate(), command.newDate());
        requireAValidDate(rectification);
        requireNoOtherPresenceOverlaps(employee.getId(), rectification);
        requireNothingPaid(rectification);

        List<PresenceRectificationPlan> plans = participants.stream().map(p -> p.plan(rectification)).toList();
        List<String> conflicts = plans.stream().flatMap(plan -> plan.conflicts().stream()).toList();
        if (!conflicts.isEmpty()) {
            throw new PresenceRectificationRejectedException(Reason.CONFLICT,
                    "No se puede rectificar la presencia " + presence.getPresenceNumber() + ": "
                            + String.join("; ", conflicts) + ".");
        }

        List<PresenceRectificationPlan.Move> moves = new ArrayList<>();
        moves.add(new PresenceRectificationPlan.Move("presence", "La presencia " + presence.getPresenceNumber(),
                presence.getStartDate(), presence.getEndDate(), rectification.newStart(), rectification.newEnd()));
        plans.forEach(plan -> moves.addAll(plan.moves()));

        if (!command.dryRun()) {
            presenceRepository.save(presence.rectifyDates(rectification.newStart(), rectification.newEnd()));
            participants.forEach(participant -> participant.apply(rectification));
            trail.record(rectification, command.rectifiedBy(), describe(moves));
            LocalDate earliest = command.newDate().isBefore(rectification.oldDate())
                    ? command.newDate() : rectification.oldDate();
            datedWriteNotice.notice(DatedWrite.on(earliest, command.ruleSystemCode(), command.employeeTypeCode(),
                    command.employeeNumber(), "presence", "employee.presence", presence.getId()));
        }

        return new RectifyPresenceDateResult(presence.getPresenceNumber(), command.field(), rectification.oldDate(),
                command.newDate(), command.dryRun(), List.copyOf(moves));
    }

    private static void requireAValidDate(PresenceRectification rectification) {
        if (rectification.newDate() == null) {
            throw new PresenceRectificationRejectedException(Reason.INVALID, "Falta la fecha nueva.");
        }
        if (rectification.field() == PresenceRectification.Field.END_DATE && rectification.presenceEnd() == null) {
            throw new PresenceRectificationRejectedException(Reason.INVALID, "La presencia "
                    + rectification.presenceNumber() + " sigue abierta: no tiene fecha de cese que rectificar.");
        }
        if (rectification.newDate().equals(rectification.oldDate())) {
            throw new PresenceRectificationRejectedException(Reason.INVALID,
                    "La fecha nueva es la que ya tiene: " + rectification.oldDate().format(DAY) + ".");
        }
        if (rectification.newEnd() != null && !rectification.newEnd().isAfter(rectification.newStart())) {
            throw new PresenceRectificationRejectedException(Reason.INVALID,
                    "El cese tiene que ser posterior al alta: quedaría del " + rectification.newStart().format(DAY)
                            + " al " + rectification.newEnd().format(DAY) + ".");
        }
    }

    private void requireNoOtherPresenceOverlaps(Long employeeId, PresenceRectification rectification) {
        for (Presence other : presenceRepository.findByEmployeeIdOrderByStartDate(employeeId)) {
            if (other.getPresenceNumber().equals(rectification.presenceNumber())) {
                continue;
            }
            boolean startsBeforeOtherEnds = other.getEndDate() == null
                    || !rectification.newStart().isAfter(other.getEndDate());
            boolean endsAfterOtherStarts = rectification.newEnd() == null
                    || !rectification.newEnd().isBefore(other.getStartDate());
            if (startsBeforeOtherEnds && endsAfterOtherStarts) {
                throw new PresenceRectificationRejectedException(Reason.OVERLAP, "Pisaría la presencia "
                        + other.getPresenceNumber() + ", " + span(other.getStartDate(), other.getEndDate()) + ".");
            }
        }
    }

    private void requireNothingPaid(PresenceRectification rectification) {
        List<String> paid = payrollLookup.definitivePeriods(rectification.ruleSystemCode(),
                rectification.employeeTypeCode(), rectification.employeeNumber(), rectification.presenceNumber());
        if (rectification.field() == PresenceRectification.Field.END_DATE) {
            LocalDate earliest = rectification.newDate().isBefore(rectification.oldDate())
                    ? rectification.newDate() : rectification.oldDate();
            String from = earliest.format(PERIOD);
            paid = paid.stream().filter(period -> period.compareTo(from) >= 0).toList();
        }
        if (!paid.isEmpty()) {
            String what = rectification.field() == PresenceRectification.Field.START_DATE ? "su alta" : "su cese";
            throw new PresenceRectificationRejectedException(Reason.PAID, "La presencia "
                    + rectification.presenceNumber() + " tiene recibos definitivos (" + String.join(", ", paid)
                    + "): " + what + " ya se pagó y no se rectifica por debajo.");
        }
    }

    private static String describe(List<PresenceRectificationPlan.Move> moves) {
        return moves.stream()
                .map(move -> move.label() + ": " + span(move.startDate(), move.endDate()) + " → "
                        + span(move.newStartDate(), move.newEndDate()))
                .collect(Collectors.joining("; "));
    }

    private static String span(LocalDate start, LocalDate end) {
        return end == null ? "desde el " + start.format(DAY) : "del " + start.format(DAY) + " al " + end.format(DAY);
    }
}

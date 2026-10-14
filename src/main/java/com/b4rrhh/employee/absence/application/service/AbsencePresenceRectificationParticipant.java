package com.b4rrhh.employee.absence.application.service;

import com.b4rrhh.employee.shared.application.port.PresenceRectification;
import com.b4rrhh.employee.shared.application.port.PresenceRectificationParticipant;
import com.b4rrhh.employee.shared.application.port.PresenceRectificationPlan;
import com.b4rrhh.employee.absence.domain.model.Absence;
import com.b4rrhh.employee.absence.domain.port.AbsenceRepository;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Ausencias (backend#172): no se mueven con la presencia, pero no pueden quedarse fuera de ella, que es
 * la regla del backend#147. Una ausencia que cabía en la presencia tiene que seguir cabiendo en la
 * rectificada.
 */
@Component
public class AbsencePresenceRectificationParticipant implements PresenceRectificationParticipant {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AbsenceRepository repository;

    public AbsencePresenceRectificationParticipant(AbsenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public PresenceRectificationPlan plan(PresenceRectification rectification) {
        List<String> conflicts = repository.findByEmployeeIdOrderByStartDateDescStartTimeDesc(rectification.employeeId())
                .stream()
                .filter(absence -> rectification.belongsToThePresence(absence.getStartDate(), absence.getEndDate()))
                .filter(absence -> !fits(absence, rectification))
                .map(absence -> "La ausencia " + absence.getAbsenceTypeCode() + " del "
                        + absence.getStartDate().format(DAY) + " quedaría fuera de la presencia")
                .toList();
        return new PresenceRectificationPlan(List.of(), conflicts);
    }

    @Override
    public void apply(PresenceRectification rectification) {
        // Nada que mover: sólo pone condiciones.
    }

    private static boolean fits(Absence absence, PresenceRectification rectification) {
        boolean startsInside = !absence.getStartDate().isBefore(rectification.newStart());
        boolean endsInside = rectification.newEnd() == null
                || (absence.getEndDate() != null && !absence.getEndDate().isAfter(rectification.newEnd()));
        return startsInside && endsInside;
    }
}

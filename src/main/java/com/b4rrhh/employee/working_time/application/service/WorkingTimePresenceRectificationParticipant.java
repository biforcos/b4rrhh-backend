package com.b4rrhh.employee.working_time.application.service;

import com.b4rrhh.employee.shared.application.port.PresenceRectification;
import com.b4rrhh.employee.shared.application.port.PresenceRectificationParticipant;
import com.b4rrhh.employee.shared.application.port.PresenceRectificationPlan;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.working_time.domain.port.WorkingTimeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Jornada: se guarda por su id. Lo que empezaba el día del alta (o acababa el del cese) que se rectifica se mueve con la
 * presencia; lo demás tiene que seguir cabiendo en ella (backend#172).
 */
@Component
public class WorkingTimePresenceRectificationParticipant implements PresenceRectificationParticipant {

    private static final String VERTICAL = "working_time";

    private final WorkingTimeRepository repository;

    public WorkingTimePresenceRectificationParticipant(WorkingTimeRepository repository) {
        this.repository = repository;
    }

    @Override
    public PresenceRectificationPlan plan(PresenceRectification rectification) {
        return planOf(rows(rectification), rectification);
    }

    @Override
    public void apply(PresenceRectification rectification) {
        List<WorkingTime> rows = rows(rectification);
        for (PresenceRectificationPlan.Move move : planOf(rows, rectification).moves()) {
            for (WorkingTime row : rows) {
                if (row.getStartDate().equals(move.startDate()) && Objects.equals(row.getEndDate(), move.endDate())) {
                    repository.save(row.rectifyDates(move.newStartDate(), move.newEndDate()));
                }
            }
        }
    }

    private List<WorkingTime> rows(PresenceRectification rectification) {
        return repository.findByEmployeeIdOrderByStartDate(rectification.employeeId());
    }

    private static PresenceRectificationPlan planOf(List<WorkingTime> rows, PresenceRectification rectification) {
        return PresenceRectificationPlan.of(VERTICAL, rows, WorkingTime::getStartDate, WorkingTime::getEndDate,
                row -> "La jornada del " + row.getWorkingTimePercentage().stripTrailingZeros().toPlainString() + " %", rectification);
    }
}

package com.b4rrhh.employee.workcenter.application.service;

import com.b4rrhh.employee.shared.application.port.PresenceRectification;
import com.b4rrhh.employee.shared.application.port.PresenceRectificationParticipant;
import com.b4rrhh.employee.shared.application.port.PresenceRectificationPlan;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import com.b4rrhh.employee.workcenter.domain.port.WorkCenterRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Centro de trabajo: se guarda por su id. Lo que empezaba el día del alta (o acababa el del cese) que se rectifica se mueve con la
 * presencia; lo demás tiene que seguir cabiendo en ella (backend#172).
 */
@Component
public class WorkCenterPresenceRectificationParticipant implements PresenceRectificationParticipant {

    private static final String VERTICAL = "work_center";

    private final WorkCenterRepository repository;

    public WorkCenterPresenceRectificationParticipant(WorkCenterRepository repository) {
        this.repository = repository;
    }

    @Override
    public PresenceRectificationPlan plan(PresenceRectification rectification) {
        return planOf(rows(rectification), rectification);
    }

    @Override
    public void apply(PresenceRectification rectification) {
        List<WorkCenter> rows = rows(rectification);
        for (PresenceRectificationPlan.Move move : planOf(rows, rectification).moves()) {
            for (WorkCenter row : rows) {
                if (row.getStartDate().equals(move.startDate()) && Objects.equals(row.getEndDate(), move.endDate())) {
                    repository.save(row.rectifyDates(move.newStartDate(), move.newEndDate()));
                }
            }
        }
    }

    private List<WorkCenter> rows(PresenceRectification rectification) {
        return repository.findByEmployeeIdOrderByStartDate(rectification.employeeId());
    }

    private static PresenceRectificationPlan planOf(List<WorkCenter> rows, PresenceRectification rectification) {
        return PresenceRectificationPlan.of(VERTICAL, rows, WorkCenter::getStartDate, WorkCenter::getEndDate,
                row -> "El centro " + row.getWorkCenterCode(), rectification);
    }
}

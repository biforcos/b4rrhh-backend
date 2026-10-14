package com.b4rrhh.employee.cost_center.application.service;

import com.b4rrhh.employee.shared.application.port.PresenceRectification;
import com.b4rrhh.employee.shared.application.port.PresenceRectificationParticipant;
import com.b4rrhh.employee.shared.application.port.PresenceRectificationPlan;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterAllocation;
import com.b4rrhh.employee.cost_center.domain.port.CostCenterRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Centros de coste: el reparto es una ventana de filas que empiezan el mismo día (backend#172). Al
 * rectificar el alta, la ventana que empezaba ese día se vuelve a escribir con el inicio nuevo; al
 * rectificar el cese, se le ajusta el fin.
 */
@Component
public class CostCenterPresenceRectificationParticipant implements PresenceRectificationParticipant {

    private static final String VERTICAL = "cost_center";

    private final CostCenterRepository repository;

    public CostCenterPresenceRectificationParticipant(CostCenterRepository repository) {
        this.repository = repository;
    }

    @Override
    public PresenceRectificationPlan plan(PresenceRectification rectification) {
        return planOf(rows(rectification), rectification);
    }

    @Override
    public void apply(PresenceRectification rectification) {
        List<CostCenterAllocation> rows = rows(rectification);
        Set<LocalDate> windows = new LinkedHashSet<>();
        for (PresenceRectificationPlan.Move move : planOf(rows, rectification).moves()) {
            windows.add(move.startDate());
        }
        for (LocalDate window : windows) {
            if (rectification.field() == PresenceRectification.Field.START_DATE) {
                List<CostCenterAllocation> moved = rows.stream()
                        .filter(row -> row.getStartDate().equals(window))
                        .map(row -> row.rectifyDates(rectification.newDate(), row.getEndDate()))
                        .toList();
                repository.deleteAllForWindow(rectification.employeeId(), window);
                repository.saveAll(moved);
            } else {
                repository.adjustWindowEndDate(rectification.employeeId(), window, rectification.newDate());
            }
        }
    }

    private List<CostCenterAllocation> rows(PresenceRectification rectification) {
        return repository.findByEmployeeIdOrderByStartDate(rectification.employeeId());
    }

    private static PresenceRectificationPlan planOf(List<CostCenterAllocation> rows, PresenceRectification rectification) {
        return PresenceRectificationPlan.of(VERTICAL, rows, CostCenterAllocation::getStartDate,
                CostCenterAllocation::getEndDate, row -> "El centro de coste " + row.getCostCenterCode(), rectification);
    }
}

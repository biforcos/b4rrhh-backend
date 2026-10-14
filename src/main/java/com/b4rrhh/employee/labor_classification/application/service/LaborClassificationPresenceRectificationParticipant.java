package com.b4rrhh.employee.labor_classification.application.service;

import com.b4rrhh.employee.shared.application.port.PresenceRectification;
import com.b4rrhh.employee.shared.application.port.PresenceRectificationParticipant;
import com.b4rrhh.employee.shared.application.port.PresenceRectificationPlan;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.labor_classification.domain.port.LaborClassificationRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Clasificación: su identidad es su inicio, así que se guarda diciendo cuál era. Lo que empezaba el día del alta (o acababa el del cese) que se rectifica se mueve con la
 * presencia; lo demás tiene que seguir cabiendo en ella (backend#172).
 */
@Component
public class LaborClassificationPresenceRectificationParticipant implements PresenceRectificationParticipant {

    private static final String VERTICAL = "labor_classification";

    private final LaborClassificationRepository repository;

    public LaborClassificationPresenceRectificationParticipant(LaborClassificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public PresenceRectificationPlan plan(PresenceRectification rectification) {
        return planOf(rows(rectification), rectification);
    }

    @Override
    public void apply(PresenceRectification rectification) {
        List<LaborClassification> rows = rows(rectification);
        for (PresenceRectificationPlan.Move move : planOf(rows, rectification).moves()) {
            for (LaborClassification row : rows) {
                if (row.getStartDate().equals(move.startDate()) && Objects.equals(row.getEndDate(), move.endDate())) {
                    repository.update(row.rectifyDates(move.newStartDate(), move.newEndDate()), row.getStartDate());
                }
            }
        }
    }

    private List<LaborClassification> rows(PresenceRectification rectification) {
        return repository.findByEmployeeIdOrderByStartDate(rectification.employeeId());
    }

    private static PresenceRectificationPlan planOf(List<LaborClassification> rows, PresenceRectification rectification) {
        return PresenceRectificationPlan.of(VERTICAL, rows, LaborClassification::getStartDate, LaborClassification::getEndDate,
                row -> "La clasificación " + row.getAgreementCategoryCode(), rectification);
    }
}

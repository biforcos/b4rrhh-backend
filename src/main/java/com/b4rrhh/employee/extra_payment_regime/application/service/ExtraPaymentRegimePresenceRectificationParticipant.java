package com.b4rrhh.employee.extra_payment_regime.application.service;

import com.b4rrhh.employee.shared.application.port.PresenceRectification;
import com.b4rrhh.employee.shared.application.port.PresenceRectificationParticipant;
import com.b4rrhh.employee.shared.application.port.PresenceRectificationPlan;
import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegime;
import com.b4rrhh.employee.extra_payment_regime.domain.port.ExtraPaymentRegimeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Régimen de pagas: se guarda por su id. Lo que empezaba el día del alta (o acababa el del cese) que se rectifica se mueve con la
 * presencia; lo demás tiene que seguir cabiendo en ella (backend#172).
 */
@Component
public class ExtraPaymentRegimePresenceRectificationParticipant implements PresenceRectificationParticipant {

    private static final String VERTICAL = "extra_payment_regime";

    private final ExtraPaymentRegimeRepository repository;

    public ExtraPaymentRegimePresenceRectificationParticipant(ExtraPaymentRegimeRepository repository) {
        this.repository = repository;
    }

    @Override
    public PresenceRectificationPlan plan(PresenceRectification rectification) {
        return planOf(rows(rectification), rectification);
    }

    @Override
    public void apply(PresenceRectification rectification) {
        List<ExtraPaymentRegime> rows = rows(rectification);
        for (PresenceRectificationPlan.Move move : planOf(rows, rectification).moves()) {
            for (ExtraPaymentRegime row : rows) {
                if (row.getStartDate().equals(move.startDate()) && Objects.equals(row.getEndDate(), move.endDate())) {
                    repository.save(row.rectifyDates(move.newStartDate(), move.newEndDate()));
                }
            }
        }
    }

    private List<ExtraPaymentRegime> rows(PresenceRectification rectification) {
        return repository.findByEmployeeIdOrderByStartDate(rectification.employeeId());
    }

    private static PresenceRectificationPlan planOf(List<ExtraPaymentRegime> rows, PresenceRectification rectification) {
        return PresenceRectificationPlan.of(VERTICAL, rows, ExtraPaymentRegime::getStartDate, ExtraPaymentRegime::getEndDate,
                row -> row.isProrated() ? "El régimen de pagas prorrateadas" : "El régimen de pagas", rectification);
    }
}

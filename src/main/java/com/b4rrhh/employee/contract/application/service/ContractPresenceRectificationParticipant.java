package com.b4rrhh.employee.contract.application.service;

import com.b4rrhh.employee.shared.application.port.PresenceRectification;
import com.b4rrhh.employee.shared.application.port.PresenceRectificationParticipant;
import com.b4rrhh.employee.shared.application.port.PresenceRectificationPlan;
import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.contract.domain.port.ContractRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Contrato: su identidad es su inicio, así que se guarda diciendo cuál era. Lo que empezaba el día del alta (o acababa el del cese) que se rectifica se mueve con la
 * presencia; lo demás tiene que seguir cabiendo en ella (backend#172).
 */
@Component
public class ContractPresenceRectificationParticipant implements PresenceRectificationParticipant {

    private static final String VERTICAL = "contract";

    private final ContractRepository repository;

    public ContractPresenceRectificationParticipant(ContractRepository repository) {
        this.repository = repository;
    }

    @Override
    public PresenceRectificationPlan plan(PresenceRectification rectification) {
        return planOf(rows(rectification), rectification);
    }

    @Override
    public void apply(PresenceRectification rectification) {
        List<Contract> rows = rows(rectification);
        for (PresenceRectificationPlan.Move move : planOf(rows, rectification).moves()) {
            for (Contract row : rows) {
                if (row.getStartDate().equals(move.startDate()) && Objects.equals(row.getEndDate(), move.endDate())) {
                    repository.update(row.rectifyDates(move.newStartDate(), move.newEndDate()), row.getStartDate());
                }
            }
        }
    }

    private List<Contract> rows(PresenceRectification rectification) {
        return repository.findByEmployeeIdOrderByStartDate(rectification.employeeId());
    }

    private static PresenceRectificationPlan planOf(List<Contract> rows, PresenceRectification rectification) {
        return PresenceRectificationPlan.of(VERTICAL, rows, Contract::getStartDate, Contract::getEndDate,
                row -> "El contrato " + row.getContractCode(), rectification);
    }
}

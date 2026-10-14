package com.b4rrhh.employee.address.application.service;

import com.b4rrhh.employee.shared.application.port.PresenceRectification;
import com.b4rrhh.employee.shared.application.port.PresenceRectificationParticipant;
import com.b4rrhh.employee.shared.application.port.PresenceRectificationPlan;
import com.b4rrhh.employee.address.domain.model.Address;
import com.b4rrhh.employee.address.domain.port.AddressRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Direcciones (backend#172). Una dirección puede vivir más que la presencia (ADR-057), así que no estorba
 * ni se mueve con el cese; con el alta se mueve la que empezaba ese mismo día, que es la que se dio de
 * alta con ella.
 */
@Component
public class AddressPresenceRectificationParticipant implements PresenceRectificationParticipant {

    private static final String VERTICAL = "address";

    private final AddressRepository repository;

    public AddressPresenceRectificationParticipant(AddressRepository repository) {
        this.repository = repository;
    }

    @Override
    public PresenceRectificationPlan plan(PresenceRectification rectification) {
        return planOf(rows(rectification), rectification);
    }

    @Override
    public void apply(PresenceRectification rectification) {
        List<Address> rows = rows(rectification);
        for (PresenceRectificationPlan.Move move : planOf(rows, rectification).moves()) {
            for (Address row : rows) {
                if (row.getStartDate().equals(move.startDate()) && Objects.equals(row.getEndDate(), move.endDate())) {
                    repository.save(row.rectifyDates(move.newStartDate(), move.newEndDate()));
                }
            }
        }
    }

    /** Sólo las que empezaban el día del alta; con el cese, ninguna. */
    private List<Address> rows(PresenceRectification rectification) {
        if (rectification.field() != PresenceRectification.Field.START_DATE) {
            return List.of();
        }
        return repository.findByEmployeeIdOrderByStartDate(rectification.employeeId()).stream()
                .filter(row -> row.getStartDate().equals(rectification.oldDate()))
                .toList();
    }

    private static PresenceRectificationPlan planOf(List<Address> rows, PresenceRectification rectification) {
        return PresenceRectificationPlan.of(VERTICAL, rows, Address::getStartDate, Address::getEndDate,
                row -> "La dirección " + row.getAddressTypeCode(), rectification);
    }
}

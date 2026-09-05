package com.b4rrhh.employee.address.application.model;

import com.b4rrhh.employee.address.domain.model.AddressPeriod;

/**
 * The one existing address a plan moves on its own (ADR-057): closed the
 * day before a new one of its type, or reopened when the one that closed it
 * is removed. Only its end date changes.
 */
public record AddressPlanAdjustment(
        Integer addressNumber,
        AddressPeriod before,
        AddressPeriod after
) {

    public AddressPlanAdjustment {
        if (addressNumber == null) {
            throw new IllegalArgumentException("addressNumber is required");
        }
        if (before == null || after == null) {
            throw new IllegalArgumentException("before and after are required");
        }
    }
}

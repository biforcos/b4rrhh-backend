package com.b4rrhh.employee.address.application.usecase;

import com.b4rrhh.employee.address.application.model.AddressPlan;

public interface PlanAddressChangeUseCase {

    AddressPlan plan(PlanAddressChangeCommand command);
}

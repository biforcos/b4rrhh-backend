package com.b4rrhh.rulesystem.workcenter.infrastructure.web.mapper;

import com.b4rrhh.geo.address.infrastructure.web.dto.PostalAddressRequest;
import com.b4rrhh.rulesystem.workcenter.application.usecase.CreateWorkCenterCommand;
import com.b4rrhh.rulesystem.workcenter.application.usecase.UpdateWorkCenterCommand;
import com.b4rrhh.rulesystem.workcenter.infrastructure.web.dto.CreateWorkCenterRequest;
import com.b4rrhh.rulesystem.workcenter.infrastructure.web.dto.UpdateWorkCenterRequest;
import org.springframework.stereotype.Component;

@Component
public class WorkCenterCommandMapper {

    public CreateWorkCenterCommand toCreateCommand(CreateWorkCenterRequest request) {
        return new CreateWorkCenterCommand(
                request.ruleSystemCode(),
                request.workCenterCode(),
                request.name(),
                request.description(),
                request.startDate(),
                request.companyCode(),
                PostalAddressRequest.toInput(request.address())
        );
    }

    public UpdateWorkCenterCommand toUpdateCommand(
            String ruleSystemCode,
            String workCenterCode,
            UpdateWorkCenterRequest request
    ) {
        return new UpdateWorkCenterCommand(
                ruleSystemCode,
                workCenterCode,
                request.name(),
                request.description(),
                request.companyCode(),
                PostalAddressRequest.toInput(request.address())
        );
    }
}
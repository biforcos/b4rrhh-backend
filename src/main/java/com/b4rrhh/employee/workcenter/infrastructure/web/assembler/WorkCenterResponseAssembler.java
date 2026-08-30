package com.b4rrhh.employee.workcenter.infrastructure.web.assembler;

import com.b4rrhh.employee.workcenter.application.usecase.WorkCenterRuleEntityTypeCodes;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import com.b4rrhh.employee.workcenter.domain.port.WorkCenterCompanyLookupPort;
import com.b4rrhh.employee.workcenter.infrastructure.web.dto.WorkCenterResponse;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("employeeWorkCenterResponseAssembler")
public class WorkCenterResponseAssembler {

    private final RuleEntityLabelResolver ruleEntityLabelResolver;
    private final WorkCenterCompanyLookupPort workCenterCompanyLookupPort;

    public WorkCenterResponseAssembler(
            RuleEntityLabelResolver ruleEntityLabelResolver,
            WorkCenterCompanyLookupPort workCenterCompanyLookupPort
    ) {
        this.ruleEntityLabelResolver = ruleEntityLabelResolver;
        this.workCenterCompanyLookupPort = workCenterCompanyLookupPort;
    }

    public WorkCenterResponse toResponse(String ruleSystemCode, WorkCenter workCenter, ResponseLanguage language) {
        String workCenterName = ruleEntityLabelResolver
                .resolveName(ruleSystemCode, WorkCenterRuleEntityTypeCodes.WORK_CENTER,
                        workCenter.getWorkCenterCode(), language.code())
                .orElse(null);
        String companyCode = workCenterCompanyLookupPort
                .findCompanyCode(ruleSystemCode, workCenter.getWorkCenterCode(), workCenter.getStartDate())
                .orElse(null);
        String companyName = companyCode == null
                ? null
                : ruleEntityLabelResolver
                        .resolveName(ruleSystemCode, WorkCenterRuleEntityTypeCodes.COMPANY, companyCode, language.code())
                        .orElse(null);

        return new WorkCenterResponse(
                workCenter.getWorkCenterAssignmentNumber(),
                workCenter.getWorkCenterCode(),
                workCenterName,
                companyCode,
                companyName,
                workCenter.getStartDate(),
                workCenter.getEndDate()
        );
    }

    public List<WorkCenterResponse> toResponseList(String ruleSystemCode, List<WorkCenter> workCenters, ResponseLanguage language) {
        return workCenters.stream()
                .map(workCenter -> toResponse(ruleSystemCode, workCenter, language))
                .toList();
    }
}

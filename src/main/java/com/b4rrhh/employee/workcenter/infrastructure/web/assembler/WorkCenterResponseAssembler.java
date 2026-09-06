package com.b4rrhh.employee.workcenter.infrastructure.web.assembler;

import com.b4rrhh.employee.workcenter.application.model.WorkCenterPlan;
import com.b4rrhh.employee.workcenter.application.model.WorkCenterPlanAdjustment;
import com.b4rrhh.employee.workcenter.application.usecase.WorkCenterRuleEntityTypeCodes;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenterOccurrence;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenterPeriod;
import com.b4rrhh.employee.workcenter.domain.port.WorkCenterCompanyLookupPort;
import com.b4rrhh.employee.workcenter.infrastructure.web.dto.WorkCenterOccurrenceResponse;
import com.b4rrhh.employee.workcenter.infrastructure.web.dto.WorkCenterPeriodResponse;
import com.b4rrhh.employee.workcenter.infrastructure.web.dto.WorkCenterPlanAdjustmentResponse;
import com.b4rrhh.employee.workcenter.infrastructure.web.dto.WorkCenterPlanResponse;
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

    public WorkCenterPlanResponse toPlanResponse(WorkCenterPlan plan) {
        return new WorkCenterPlanResponse(
                plan.operation().name(),
                plan.isAccepted(),
                plan.rejection() == null ? null : plan.rejection().name(),
                toOccurrence(plan.occurrence()),
                plan.correctedOccurrence() == null ? null : toOccurrence(plan.correctedOccurrence()),
                toAdjustment(plan.adjustedOccurrence()),
                plan.overlaps().stream().map(this::toPeriod).toList(),
                plan.gaps().stream().map(this::toPeriod).toList(),
                plan.stretchCandidates().stream().map(this::toOccurrence).toList(),
                plan.projected().stream().map(this::toOccurrence).toList()
        );
    }

    private WorkCenterPlanAdjustmentResponse toAdjustment(WorkCenterPlanAdjustment adjustment) {
        if (adjustment == null) {
            return null;
        }

        return new WorkCenterPlanAdjustmentResponse(
                adjustment.workCenterAssignmentNumber(),
                toPeriod(adjustment.before()),
                toPeriod(adjustment.after())
        );
    }

    private WorkCenterOccurrenceResponse toOccurrence(WorkCenterOccurrence occurrence) {
        return new WorkCenterOccurrenceResponse(
                occurrence.workCenterAssignmentNumber(),
                occurrence.startDate(),
                occurrence.endDate()
        );
    }

    private WorkCenterPeriodResponse toPeriod(WorkCenterPeriod period) {
        return new WorkCenterPeriodResponse(period.startDate(), period.endDate());
    }
}

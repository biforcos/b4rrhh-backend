package com.b4rrhh.employee.working_time.infrastructure.web.assembler;

import com.b4rrhh.employee.working_time.application.model.WorkingTimePlan;
import com.b4rrhh.employee.working_time.application.model.WorkingTimePlanAdjustment;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimeOccurrence;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimePeriod;
import com.b4rrhh.employee.working_time.infrastructure.web.dto.WorkingTimeOccurrenceResponse;
import com.b4rrhh.employee.working_time.infrastructure.web.dto.WorkingTimePeriodResponse;
import com.b4rrhh.employee.working_time.infrastructure.web.dto.WorkingTimePlanAdjustmentResponse;
import com.b4rrhh.employee.working_time.infrastructure.web.dto.WorkingTimePlanResponse;
import com.b4rrhh.employee.working_time.infrastructure.web.dto.WorkingTimeResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WorkingTimeResponseAssembler {

    public WorkingTimeResponse toResponse(WorkingTime workingTime) {
        return new WorkingTimeResponse(
                workingTime.getWorkingTimeNumber(),
                workingTime.getStartDate(),
                workingTime.getEndDate(),
                workingTime.getWorkingTimePercentage(),
                workingTime.getWeeklyHours(),
                workingTime.getDailyHours(),
                workingTime.getMonthlyHours()
        );
    }

    public List<WorkingTimeResponse> toResponseList(List<WorkingTime> workingTimes) {
        return workingTimes.stream()
                .map(this::toResponse)
                .toList();
    }

    public WorkingTimePlanResponse toPlanResponse(WorkingTimePlan plan) {
        return new WorkingTimePlanResponse(
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

    private WorkingTimePlanAdjustmentResponse toAdjustment(WorkingTimePlanAdjustment adjustment) {
        if (adjustment == null) {
            return null;
        }

        return new WorkingTimePlanAdjustmentResponse(
                adjustment.workingTimeNumber(),
                toPeriod(adjustment.before()),
                toPeriod(adjustment.after())
        );
    }

    private WorkingTimeOccurrenceResponse toOccurrence(WorkingTimeOccurrence occurrence) {
        return new WorkingTimeOccurrenceResponse(
                occurrence.workingTimeNumber(),
                occurrence.startDate(),
                occurrence.endDate()
        );
    }

    private WorkingTimePeriodResponse toPeriod(WorkingTimePeriod period) {
        return new WorkingTimePeriodResponse(period.startDate(), period.endDate());
    }
}
package com.b4rrhh.employee.extra_payment_regime.infrastructure.web.assembler;

import com.b4rrhh.employee.extra_payment_regime.application.model.ExtraPaymentRegimePlan;
import com.b4rrhh.employee.extra_payment_regime.application.model.ExtraPaymentRegimePlanAdjustment;
import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegime;
import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegimeOccurrence;
import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegimePeriod;
import com.b4rrhh.employee.extra_payment_regime.infrastructure.web.dto.ExtraPaymentRegimeOccurrenceResponse;
import com.b4rrhh.employee.extra_payment_regime.infrastructure.web.dto.ExtraPaymentRegimePeriodResponse;
import com.b4rrhh.employee.extra_payment_regime.infrastructure.web.dto.ExtraPaymentRegimePlanAdjustmentResponse;
import com.b4rrhh.employee.extra_payment_regime.infrastructure.web.dto.ExtraPaymentRegimePlanResponse;
import com.b4rrhh.employee.extra_payment_regime.infrastructure.web.dto.ExtraPaymentRegimeResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExtraPaymentRegimeResponseAssembler {

    public ExtraPaymentRegimeResponse toResponse(ExtraPaymentRegime extraPaymentRegime) {
        return new ExtraPaymentRegimeResponse(
                extraPaymentRegime.getExtraPaymentRegimeNumber(),
                extraPaymentRegime.getStartDate(),
                extraPaymentRegime.getEndDate(),
                extraPaymentRegime.isProrated()
        );
    }

    public List<ExtraPaymentRegimeResponse> toResponseList(List<ExtraPaymentRegime> regimes) {
        return regimes.stream()
                .map(this::toResponse)
                .toList();
    }

    public ExtraPaymentRegimePlanResponse toPlanResponse(ExtraPaymentRegimePlan plan) {
        return new ExtraPaymentRegimePlanResponse(
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

    private ExtraPaymentRegimePlanAdjustmentResponse toAdjustment(ExtraPaymentRegimePlanAdjustment adjustment) {
        if (adjustment == null) {
            return null;
        }

        return new ExtraPaymentRegimePlanAdjustmentResponse(
                adjustment.extraPaymentRegimeNumber(),
                toPeriod(adjustment.before()),
                toPeriod(adjustment.after())
        );
    }

    private ExtraPaymentRegimeOccurrenceResponse toOccurrence(ExtraPaymentRegimeOccurrence occurrence) {
        return new ExtraPaymentRegimeOccurrenceResponse(
                occurrence.extraPaymentRegimeNumber(),
                occurrence.startDate(),
                occurrence.endDate()
        );
    }

    private ExtraPaymentRegimePeriodResponse toPeriod(ExtraPaymentRegimePeriod period) {
        return new ExtraPaymentRegimePeriodResponse(period.startDate(), period.endDate());
    }
}
